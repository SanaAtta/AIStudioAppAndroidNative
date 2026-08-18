package com.example.myapplication.ads

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
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared interstitial cache keyed by ad unit id.
 * One in-flight load per unit; [showThen] queues on the same load (never starts a duplicate request).
 */
object SplashInterstitial {
    private const val PENDING_SHOW_TIMEOUT_MS = 4_000L
    /** Language / onboarding Done — wait for in-flight load after button tap, then navigate. */
    private const val SETUP_FLOW_PENDING_SHOW_TIMEOUT_MS = 5_000L
    private const val LOAD_RETRY_DELAY_MS = 350L
    private const val SHOW_RETRY_DELAY_MS = 100L

    private val mainHandler = Handler(Looper.getMainLooper())

    private data class PendingShow(
        val activity: Activity,
        val unitId: String,
        val slotLabel: String,
        val rcParam: String,
        val onFinished: () -> Unit,
        val onAdReadyToShow: (() -> Unit)?,
        val onUnavailable: (() -> Unit)?,
        val allowLoadRetry: Boolean = true,
    )

    private class UnitSlot {
        var ad: InterstitialAd? = null
        var loadInFlight: Boolean = false
        var loadSlotLabel: String = "interstitial"
        var loadRcParam: String = AdUnitIds.Rc.INTERSTITIAL
        var pendingShow: PendingShow? = null
        var pendingTimeout: Runnable? = null
        var showing: Boolean = false
        var loadRetryUsed: Boolean = false
    }

    private val slots = ConcurrentHashMap<String, UnitSlot>()

    private fun slot(unitId: String): UnitSlot = slots.getOrPut(unitId) { UnitSlot() }

    fun preload(context: Context, unitId: String, slotLabel: String, rcParam: String) {
        preloadIfNeeded(context, unitId, slotLabel, rcParam)
    }

    fun preloadIfNeeded(context: Context, unitId: String, slotLabel: String, rcParam: String) {
        if (!AdsControl.shouldShowAds() || unitId.isBlank()) return
        if (isWarmForUnit(unitId)) return
        MobileAdsInitializer.runWhenReady {
            mainHandler.post { preloadOnMain(context.applicationContext, unitId, slotLabel, rcParam) }
        }
    }

    fun isWarmForUnit(unitId: String): Boolean {
        if (unitId.isBlank()) return false
        val s = slots[unitId] ?: return false
        return s.ad != null || s.loadInFlight || s.showing
    }

    private fun preloadOnMain(app: Context, unitId: String, slotLabel: String, rcParam: String) {
        if (!AdsControl.shouldShowAds() || unitId.isBlank()) return
        val s = slot(unitId)
        if (s.ad != null || s.loadInFlight || s.showing) return

        s.loadInFlight = true
        s.loadSlotLabel = slotLabel
        s.loadRcParam = rcParam
        AdsLoadLog.requesting(slotLabel, rcParam, unitId)
        InterstitialAd.load(
            app,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mainHandler.post {
                        val current = slot(unitId)
                        if (!current.loadInFlight) {
                            return@post
                        }
                        AdsLoadLog.loaded(current.loadSlotLabel, current.loadRcParam, unitId)
                        current.loadInFlight = false
                        current.ad = ad
                        flushPendingShow(unitId, success = true)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    mainHandler.post {
                        val current = slot(unitId)
                        if (!current.loadInFlight) return@post
                        AdsLoadLog.failed(current.loadSlotLabel, current.loadRcParam, unitId, error)
                        current.loadInFlight = false
                        current.ad = null
                        if (current.pendingShow != null) {
                            val pending = current.pendingShow
                            if (pending?.allowLoadRetry == true && !current.loadRetryUsed) {
                                current.loadRetryUsed = true
                                mainHandler.postDelayed(
                                    {
                                        preloadOnMain(
                                            app,
                                            unitId,
                                            current.loadSlotLabel,
                                            current.loadRcParam,
                                        )
                                    },
                                    LOAD_RETRY_DELAY_MS,
                                )
                            } else {
                                flushPendingShow(unitId, success = false)
                            }
                            return@post
                        }
                        flushPendingShow(unitId, success = false)
                    }
                }
            },
        )
    }

    private fun flushPendingShow(unitId: String, success: Boolean) {
        val s = slot(unitId)
        s.pendingTimeout?.let { mainHandler.removeCallbacks(it) }
        s.pendingTimeout = null
        val pending = s.pendingShow
        s.pendingShow = null
        if (pending == null) return

        if (success) {
            val cached = s.ad
            if (cached != null) {
                s.ad = null
                attachShowAndShow(
                    activity = pending.activity,
                    unitId = unitId,
                    slotLabel = pending.slotLabel,
                    rcParam = pending.rcParam,
                    ad = cached,
                    onFinished = pending.onFinished,
                    onUnavailable = pending.onUnavailable,
                    onAdDisplayed = pending.onAdReadyToShow,
                )
                return
            }
        }
        deliverUnavailable(pending)
    }

    private fun deliverUnavailable(pending: PendingShow) {
        pending.onAdReadyToShow?.invoke()
        if (pending.onUnavailable != null) {
            pending.onUnavailable.invoke()
        } else {
            pending.onFinished()
        }
    }

    fun showThen(
        activity: Activity,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        onFinished: () -> Unit,
        onAdReadyToShow: (() -> Unit)? = null,
        onUnavailable: (() -> Unit)? = null,
        preloadWaitMs: Long = PENDING_SHOW_TIMEOUT_MS,
    ) {
        if (!AdsControl.shouldShowAds() || unitId.isBlank()) {
            if (onUnavailable != null) onUnavailable() else onFinished()
            return
        }
        MobileAdsInitializer.runWhenReady {
            mainHandler.post {
                showThenOnMain(
                    activity,
                    unitId,
                    slotLabel,
                    rcParam,
                    onFinished,
                    onAdReadyToShow,
                    onUnavailable,
                    preloadWaitMs,
                )
            }
        }
    }

    private fun showThenOnMain(
        activity: Activity,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        onFinished: () -> Unit,
        onAdReadyToShow: (() -> Unit)?,
        onUnavailable: (() -> Unit)?,
        preloadWaitMs: Long,
    ) {
        val s = slot(unitId)
        if (s.showing) {
            onAdReadyToShow?.invoke()
            if (onUnavailable != null) onUnavailable() else onFinished()
            return
        }

        val cached = s.ad
        if (cached != null) {
            s.ad = null
            attachShowAndShow(
                activity = activity,
                unitId = unitId,
                slotLabel = slotLabel,
                rcParam = rcParam,
                ad = cached,
                onFinished = onFinished,
                onUnavailable = onUnavailable,
                onAdDisplayed = onAdReadyToShow,
            )
            return
        }

        queuePendingShow(
            activity = activity,
            unitId = unitId,
            slotLabel = slotLabel,
            rcParam = rcParam,
            onFinished = onFinished,
            onAdReadyToShow = onAdReadyToShow,
            onUnavailable = onUnavailable,
            preloadWaitMs = preloadWaitMs,
            allowLoadRetry = preloadWaitMs != SETUP_FLOW_PENDING_SHOW_TIMEOUT_MS,
        )
        if (!s.loadInFlight) {
            preloadOnMain(activity.applicationContext, unitId, slotLabel, rcParam)
        }
    }

    fun setupFlowPreloadWaitMs(): Long = SETUP_FLOW_PENDING_SHOW_TIMEOUT_MS

    fun defaultPreloadWaitMs(): Long = PENDING_SHOW_TIMEOUT_MS

    private fun queuePendingShow(
        activity: Activity,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        onFinished: () -> Unit,
        onAdReadyToShow: (() -> Unit)?,
        onUnavailable: (() -> Unit)?,
        preloadWaitMs: Long,
        allowLoadRetry: Boolean = true,
    ) {
        val s = slot(unitId)
        s.loadRetryUsed = false
        s.pendingShow =
            PendingShow(
                activity = activity,
                unitId = unitId,
                slotLabel = slotLabel,
                rcParam = rcParam,
                onFinished = onFinished,
                onAdReadyToShow = onAdReadyToShow,
                onUnavailable = onUnavailable,
                allowLoadRetry = allowLoadRetry,
            )
        s.pendingTimeout?.let { mainHandler.removeCallbacks(it) }
        val timeout =
            Runnable {
                val current = slot(unitId)
                val pending = current.pendingShow ?: return@Runnable
                current.pendingShow = null
                current.pendingTimeout = null
                deliverUnavailable(pending)
            }
        s.pendingTimeout = timeout
        mainHandler.postDelayed(timeout, preloadWaitMs)
    }

    private fun canShowInterstitial(activity: Activity): Boolean {
        if (activity.isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            return false
        }
        if (activity is LifecycleOwner) {
            return activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        return true
    }

    private fun attachShowAndShow(
        activity: Activity,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        ad: InterstitialAd,
        onFinished: () -> Unit,
        onUnavailable: (() -> Unit)?,
        onAdDisplayed: (() -> Unit)? = null,
        showAttempt: Int = 0,
    ) {
        if (!canShowInterstitial(activity)) {
            if (showAttempt < 3) {
                mainHandler.postDelayed(
                    {
                        attachShowAndShow(
                            activity = activity,
                            unitId = unitId,
                            slotLabel = slotLabel,
                            rcParam = rcParam,
                            ad = ad,
                            onFinished = onFinished,
                            onUnavailable = onUnavailable,
                            onAdDisplayed = onAdDisplayed,
                            showAttempt = showAttempt + 1,
                        )
                    },
                    SHOW_RETRY_DELAY_MS,
                )
                return
            }
            onAdDisplayed?.invoke()
            val fallback = onUnavailable ?: onFinished
            runAfterActivityWindowReady(activity, fallback)
            return
        }

        val s = slot(unitId)
        s.showing = true
        s.ad = null
        s.loadInFlight = false
        s.pendingShow = null
        s.pendingTimeout?.let { mainHandler.removeCallbacks(it) }
        s.pendingTimeout = null

        FullscreenAdLifecycle.markFullscreenAdShowing()
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    // Logged on dismiss so one line covers show + close.
                }

                override fun onAdDismissedFullScreenContent() {
                    AdsLoadLog.showedAndClosed(slotLabel, rcParam, unitId)
                    ad.fullScreenContentCallback = null
                    s.showing = false
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    runAfterActivityWindowReady(activity, onFinished)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdsLoadLog.showFailed(slotLabel, rcParam, unitId, adError)
                    ad.fullScreenContentCallback = null
                    s.showing = false
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    onAdDisplayed?.invoke()
                    val fallback = onUnavailable ?: onFinished
                    runAfterActivityWindowReady(activity, fallback)
                }
            }
        // Dismiss Compose loader before presenting fullscreen ad (overlay can block presentation).
        onAdDisplayed?.invoke()
        ad.show(activity)
    }

    private fun runAfterActivityWindowReady(activity: Activity, block: () -> Unit) {
        val decor = activity.window?.decorView
        if (decor != null) {
            decor.post { block() }
        } else {
            mainHandler.post { block() }
        }
    }
}
