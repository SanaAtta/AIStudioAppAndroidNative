package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Image-generation progress fullscreen ads (RC-controlled):
 * 1) Request **Rewarded** first
 * 2) Only if Rewarded fails → request **Rewarded Interstitial**
 * 3) Never show a regular interstitial in this flow
 * 4) At most one show (or none) per generation session
 */
object GenerateProgressRewardedAds {
    private const val LOAD_TIMEOUT_MS = 8_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionBusy = AtomicBoolean(false)
    private val requestGeneration = AtomicInteger(0)
    private val preloadBusy = AtomicBoolean(false)

    private var preloadedRewarded: RewardedAd? = null
    private var preloadedRewardedUnitId: String? = null
    private var timeoutRunnable: Runnable? = null

    fun preloadRewarded(
        context: Context,
        unitId: String,
        slotLabel: String,
        rcParam: String,
    ) {
        if (unitId.isBlank() || !AdsControl.shouldShowAds()) return
        if (preloadedRewarded != null && preloadedRewardedUnitId == unitId) {
            AdsLoadLog.trace(slotLabel, "preload skipped — rewarded already ready")
            return
        }
        if (!preloadBusy.compareAndSet(false, true)) {
            AdsLoadLog.trace(slotLabel, "preload skipped — already loading")
            return
        }
        AdsLoadLog.requesting("$slotLabel (preload rewarded)", rcParam, unitId)
        MobileAdsInitializer.runWhenReady {
            mainHandler.post {
                RewardedAd.load(
                    context.applicationContext,
                    unitId,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            mainHandler.post {
                                preloadBusy.set(false)
                                preloadedRewarded = ad
                                preloadedRewardedUnitId = unitId
                                AdsLoadLog.loaded("$slotLabel (preload rewarded)", rcParam, unitId)
                            }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            mainHandler.post {
                                preloadBusy.set(false)
                                AdsLoadLog.failed("$slotLabel (preload rewarded)", rcParam, unitId, error)
                            }
                        }
                    },
                )
            }
        }
    }

    fun clearPreload() {
        preloadedRewarded = null
        preloadedRewardedUnitId = null
        preloadBusy.set(false)
    }

    /**
     * Show rewarded → on failure only, rewarded interstitial. Never regular interstitial.
     */
    fun showThen(
        activity: Activity?,
        rewardedEnabled: Boolean,
        rewardedUnitId: String,
        rewardedRcParam: String,
        rewardedInterstitialEnabled: Boolean,
        rewardedInterstitialUnitId: String,
        rewardedInterstitialRcParam: String,
        slotLabel: String,
        onContinue: () -> Unit,
    ) {
        if (activity == null || !AdsControl.shouldShowAds()) {
            clearPreload()
            onContinue()
            return
        }
        val tryRewarded = rewardedEnabled && rewardedUnitId.isNotBlank()
        val tryRewardedInterstitial =
            rewardedInterstitialEnabled && rewardedInterstitialUnitId.isNotBlank()
        if (!tryRewarded && !tryRewardedInterstitial) {
            clearPreload()
            onContinue()
            return
        }
        if (!tryRewarded) {
            AdsLoadLog.trace(slotLabel, "rewarded off in RC — skip fullscreen (no interstitial)")
            clearPreload()
            onContinue()
            return
        }
        if (!sessionBusy.compareAndSet(false, true)) {
            AdsLoadLog.trace(slotLabel, "session busy — skip duplicate ad request/show")
            return
        }

        val generation = requestGeneration.incrementAndGet()
        val finished = AtomicBoolean(false)

        fun finishOnce(reason: String) {
            if (!finished.compareAndSet(false, true)) return
            AdsLoadLog.trace(slotLabel, "finish ($reason)")
            cancelTimeout()
            sessionBusy.set(false)
            requestGeneration.incrementAndGet()
            clearPreload()
            runAfterActivityWindowReady(activity, onContinue)
        }

        fun armTimeout(stage: String) {
            cancelTimeout()
            timeoutRunnable =
                Runnable {
                    if (generation != requestGeneration.get() || finished.get()) return@Runnable
                    AdsLoadLog.trace(slotLabel, "timeout ${LOAD_TIMEOUT_MS}ms at $stage — continue")
                    finishOnce("timeout_$stage")
                }
            mainHandler.postDelayed(timeoutRunnable!!, LOAD_TIMEOUT_MS)
        }

        fun requestRewardedInterstitialFallback() {
            if (!tryRewardedInterstitial) {
                finishOnce("rewarded_failed_no_fallback")
                return
            }
            AdsLoadLog.trace(slotLabel, "rewarded failed — request rewarded interstitial fallback")
            armTimeout("rewarded_interstitial")
            MobileAdsInitializer.runWhenReady {
                mainHandler.post {
                    if (generation != requestGeneration.get() || finished.get()) return@post
                    if (!canShowOnActivity(activity)) {
                        finishOnce("activity not ready")
                        return@post
                    }
                    AdsLoadLog.requesting(
                        "$slotLabel rewarded interstitial",
                        rewardedInterstitialRcParam,
                        rewardedInterstitialUnitId,
                    )
                    RewardedInterstitialAd.load(
                        activity.applicationContext,
                        rewardedInterstitialUnitId,
                        AdRequest.Builder().build(),
                        object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                mainHandler.post {
                                    if (generation != requestGeneration.get() || finished.get()) {
                                        AdsLoadLog.trace(
                                            slotLabel,
                                            "rewarded interstitial loaded late — discarded",
                                        )
                                        return@post
                                    }
                                    AdsLoadLog.loaded(
                                        "$slotLabel rewarded interstitial",
                                        rewardedInterstitialRcParam,
                                        rewardedInterstitialUnitId,
                                    )
                                    cancelTimeout()
                                    showRewardedInterstitial(
                                        activity = activity,
                                        ad = ad,
                                        unitId = rewardedInterstitialUnitId,
                                        slotLabel = "$slotLabel rewarded interstitial",
                                        rcParam = rewardedInterstitialRcParam,
                                        generation = generation,
                                        finished = finished,
                                        finishOnce = ::finishOnce,
                                    )
                                }
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                mainHandler.post {
                                    if (generation != requestGeneration.get() || finished.get()) {
                                        return@post
                                    }
                                    AdsLoadLog.failed(
                                        "$slotLabel rewarded interstitial",
                                        rewardedInterstitialRcParam,
                                        rewardedInterstitialUnitId,
                                        error,
                                    )
                                    finishOnce("rewarded_interstitial_load_failed")
                                }
                            }
                        },
                    )
                }
            }
        }

        fun showOrRequestRewarded() {
            val ready = takePreloadedRewarded(rewardedUnitId)
            if (ready != null) {
                AdsLoadLog.trace(slotLabel, "show preloaded rewarded")
                cancelTimeout()
                showRewarded(
                    activity = activity,
                    ad = ready,
                    unitId = rewardedUnitId,
                    slotLabel = "$slotLabel rewarded",
                    rcParam = rewardedRcParam,
                    generation = generation,
                    finished = finished,
                    finishOnce = ::finishOnce,
                    onShowFailedThenFallback = { requestRewardedInterstitialFallback() },
                )
                return
            }
            AdsLoadLog.trace(slotLabel, "request rewarded")
            armTimeout("rewarded")
            MobileAdsInitializer.runWhenReady {
                mainHandler.post {
                    if (generation != requestGeneration.get() || finished.get()) return@post
                    if (!canShowOnActivity(activity)) {
                        finishOnce("activity not ready")
                        return@post
                    }
                    AdsLoadLog.requesting("$slotLabel rewarded", rewardedRcParam, rewardedUnitId)
                    RewardedAd.load(
                        activity.applicationContext,
                        rewardedUnitId,
                        AdRequest.Builder().build(),
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedAd) {
                                mainHandler.post {
                                    if (generation != requestGeneration.get() || finished.get()) {
                                        AdsLoadLog.trace(slotLabel, "rewarded loaded late — discarded")
                                        return@post
                                    }
                                    AdsLoadLog.loaded(
                                        "$slotLabel rewarded",
                                        rewardedRcParam,
                                        rewardedUnitId,
                                    )
                                    cancelTimeout()
                                    showRewarded(
                                        activity = activity,
                                        ad = ad,
                                        unitId = rewardedUnitId,
                                        slotLabel = "$slotLabel rewarded",
                                        rcParam = rewardedRcParam,
                                        generation = generation,
                                        finished = finished,
                                        finishOnce = ::finishOnce,
                                        onShowFailedThenFallback = {
                                            requestRewardedInterstitialFallback()
                                        },
                                    )
                                }
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                mainHandler.post {
                                    if (generation != requestGeneration.get() || finished.get()) {
                                        return@post
                                    }
                                    AdsLoadLog.failed(
                                        "$slotLabel rewarded",
                                        rewardedRcParam,
                                        rewardedUnitId,
                                        error,
                                    )
                                    cancelTimeout()
                                    requestRewardedInterstitialFallback()
                                }
                            }
                        },
                    )
                }
            }
        }

        if (tryRewarded) {
            showOrRequestRewarded()
        } else {
            finishOnce("rewarded_disabled")
        }
    }

    private fun takePreloadedRewarded(unitId: String): RewardedAd? {
        val ad = preloadedRewarded
        if (ad == null || preloadedRewardedUnitId != unitId) return null
        preloadedRewarded = null
        preloadedRewardedUnitId = null
        preloadBusy.set(false)
        return ad
    }

    private fun showRewarded(
        activity: Activity,
        ad: RewardedAd,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        generation: Int,
        finished: AtomicBoolean,
        finishOnce: (String) -> Unit,
        onShowFailedThenFallback: () -> Unit,
    ) {
        if (!canShowOnActivity(activity)) {
            onShowFailedThenFallback()
            return
        }
        FullscreenAdLifecycle.markFullscreenAdShowing()
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdsLoadLog.shown(slotLabel, rcParam, unitId)
                }

                override fun onAdDismissedFullScreenContent() {
                    AdsLoadLog.showedAndClosed(slotLabel, rcParam, unitId)
                    ad.fullScreenContentCallback = null
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    if (generation != requestGeneration.get()) return
                    finishOnce("rewarded_dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdsLoadLog.showFailed(slotLabel, rcParam, unitId, adError)
                    ad.fullScreenContentCallback = null
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    if (generation != requestGeneration.get() || finished.get()) return
                    onShowFailedThenFallback()
                }
            }
        try {
            ad.show(activity, OnUserEarnedRewardListener { /* reward optional for navigation */ })
        } catch (t: Throwable) {
            AdsLoadLog.trace(slotLabel, "show threw: ${t.message}")
            ad.fullScreenContentCallback = null
            FullscreenAdLifecycle.markFullscreenAdDismissed()
            onShowFailedThenFallback()
        }
    }

    private fun showRewardedInterstitial(
        activity: Activity,
        ad: RewardedInterstitialAd,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        generation: Int,
        finished: AtomicBoolean,
        finishOnce: (String) -> Unit,
    ) {
        if (!canShowOnActivity(activity)) {
            finishOnce("cannot show rewarded interstitial")
            return
        }
        FullscreenAdLifecycle.markFullscreenAdShowing()
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdsLoadLog.shown(slotLabel, rcParam, unitId)
                }

                override fun onAdDismissedFullScreenContent() {
                    AdsLoadLog.showedAndClosed(slotLabel, rcParam, unitId)
                    ad.fullScreenContentCallback = null
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    if (generation != requestGeneration.get()) return
                    finishOnce("rewarded_interstitial_dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdsLoadLog.showFailed(slotLabel, rcParam, unitId, adError)
                    ad.fullScreenContentCallback = null
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    if (generation != requestGeneration.get() || finished.get()) return
                    finishOnce("rewarded_interstitial_show_failed")
                }
            }
        try {
            ad.show(activity, OnUserEarnedRewardListener { })
        } catch (t: Throwable) {
            AdsLoadLog.trace(slotLabel, "show threw: ${t.message}")
            ad.fullScreenContentCallback = null
            FullscreenAdLifecycle.markFullscreenAdDismissed()
            finishOnce("rewarded_interstitial_show_exception")
        }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun canShowOnActivity(activity: Activity): Boolean {
        if (activity.isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            return false
        }
        if (activity is LifecycleOwner) {
            return activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        return true
    }

    private fun runAfterActivityWindowReady(activity: Activity, block: () -> Unit) {
        mainHandler.post {
            if (!canShowOnActivity(activity)) {
                block()
                return@post
            }
            block()
        }
    }
}
