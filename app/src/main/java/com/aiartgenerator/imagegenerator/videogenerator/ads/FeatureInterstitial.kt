package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.ThemePreferences
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Click-time interstitial.
 *
 * Default click flow: loading dialog → single request → show within timeout, else continue.
 * Post-generate flow: [withLoadingDialog]=false + optional [preload] during generation so
 * only the fullscreen ad appears when generation finishes (no loading dialog).
 */
object FeatureInterstitial {
    private const val CLICK_LOAD_TIMEOUT_MS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionBusy = AtomicBoolean(false)
    private val requestGeneration = AtomicInteger(0)
    private val continueEpoch = AtomicInteger(0)
    private val preloadBusy = AtomicBoolean(false)

    private var loadingDialog: Dialog? = null
    private var timeoutRunnable: Runnable? = null
    private var preloadedAd: InterstitialAd? = null
    private var preloadedUnitId: String? = null
    /** When a new navigation is requested while an ad is showing, run this after dismiss. */
    private var pendingContinue: (() -> Unit)? = null

    fun preload(
        context: Context,
        unitId: String,
        slotLabel: String,
        rcParam: String,
    ) {
        if (unitId.isBlank() || !AdsControl.shouldShowAds()) return
        if (preloadedAd != null && preloadedUnitId == unitId) {
            AdsLoadLog.trace(slotLabel, "preload skipped — already ready")
            return
        }
        if (!preloadBusy.compareAndSet(false, true)) {
            AdsLoadLog.trace(slotLabel, "preload skipped — already loading")
            return
        }
        AdsLoadLog.requesting("$slotLabel (preload)", rcParam, unitId)
        MobileAdsInitializer.runWhenReady {
            mainHandler.post {
                InterstitialAd.load(
                    context.applicationContext,
                    unitId,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            mainHandler.post {
                                preloadBusy.set(false)
                                preloadedAd = ad
                                preloadedUnitId = unitId
                                AdsLoadLog.loaded("$slotLabel (preload)", rcParam, unitId)
                            }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            mainHandler.post {
                                preloadBusy.set(false)
                                AdsLoadLog.failed("$slotLabel (preload)", rcParam, unitId, error)
                            }
                        }
                    },
                )
            }
        }
    }

    fun clearPreload() {
        preloadedAd = null
        preloadedUnitId = null
        preloadBusy.set(false)
    }

    fun showThen(
        activity: Activity?,
        enabled: Boolean,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        onContinue: () -> Unit,
        withLoadingDialog: Boolean = true,
    ) {
        if (activity == null || !enabled || unitId.isBlank() || !AdsControl.shouldShowAds()) {
            clearPreload()
            pendingContinue = null
            onContinue()
            return
        }
        // Block double-tap / overlapping ad requests. Queue navigation so it runs after
        // the current ad dismisses (calling navigate while a fullscreen ad is up can no-op).
        if (!sessionBusy.compareAndSet(false, true)) {
            AdsLoadLog.trace(slotLabel, "session busy — queue continue after current ad")
            pendingContinue = onContinue
            continueEpoch.incrementAndGet()
            return
        }

        pendingContinue = null
        val generation = requestGeneration.incrementAndGet()
        val finished = AtomicBoolean(false)
        val epoch = continueEpoch.get()

        fun finishOnce(reason: String) {
            if (!finished.compareAndSet(false, true)) return
            AdsLoadLog.trace(slotLabel, "finish ($reason)")
            cancelTimeout()
            dismissLoadingDialog()
            sessionBusy.set(false)
            // Invalidate any late load/show callbacks for this click.
            requestGeneration.incrementAndGet()
            val queued = pendingContinue
            pendingContinue = null
            if (epoch != continueEpoch.get()) {
                AdsLoadLog.trace(slotLabel, "finish superseded — run queued continue")
                if (queued != null) {
                    runAfterActivityWindowReady(activity, queued)
                }
                return
            }
            runAfterActivityWindowReady(activity, onContinue)
        }

        val ready = takePreloaded(unitId)
        if (ready != null) {
            AdsLoadLog.trace(slotLabel, "show preloaded interstitial (no loading dialog)")
            showLoadedAd(
                activity = activity,
                ad = ready,
                unitId = unitId,
                slotLabel = slotLabel,
                rcParam = rcParam,
                generation = generation,
                finished = finished,
                finishOnce = ::finishOnce,
            )
            return
        }

        if (withLoadingDialog) {
            presentLoadingDialog(activity)
        }
        AdsLoadLog.trace(
            slotLabel,
            if (withLoadingDialog) {
                "click — request interstitial"
            } else {
                "request interstitial silently (no loading dialog)"
            },
        )

        timeoutRunnable =
            Runnable {
                if (generation != requestGeneration.get()) return@Runnable
                AdsLoadLog.trace(slotLabel, "timeout ${CLICK_LOAD_TIMEOUT_MS}ms — continue without ad")
                finishOnce("timeout")
            }
        mainHandler.postDelayed(timeoutRunnable!!, CLICK_LOAD_TIMEOUT_MS)

        MobileAdsInitializer.runWhenReady {
            mainHandler.post {
                if (generation != requestGeneration.get() || finished.get()) return@post
                if (!canShowOnActivity(activity)) {
                    finishOnce("activity not ready")
                    return@post
                }
                requestAndMaybeShow(
                    activity = activity,
                    unitId = unitId,
                    slotLabel = slotLabel,
                    rcParam = rcParam,
                    generation = generation,
                    finished = finished,
                    finishOnce = ::finishOnce,
                )
            }
        }
    }

    private fun takePreloaded(unitId: String): InterstitialAd? {
        val ad = preloadedAd
        if (ad == null || preloadedUnitId != unitId) return null
        preloadedAd = null
        preloadedUnitId = null
        preloadBusy.set(false)
        return ad
    }

    private fun requestAndMaybeShow(
        activity: Activity,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        generation: Int,
        finished: AtomicBoolean,
        finishOnce: (String) -> Unit,
    ) {
        AdsLoadLog.requesting(slotLabel, rcParam, unitId)
        InterstitialAd.load(
            activity.applicationContext,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    mainHandler.post {
                        if (generation != requestGeneration.get() || finished.get()) {
                            AdsLoadLog.trace(slotLabel, "loaded late — discarded (no show / no re-request)")
                            return@post
                        }
                        AdsLoadLog.loaded(slotLabel, rcParam, unitId)
                        cancelTimeout()
                        dismissLoadingDialog()
                        showLoadedAd(
                            activity = activity,
                            ad = ad,
                            unitId = unitId,
                            slotLabel = slotLabel,
                            rcParam = rcParam,
                            generation = generation,
                            finished = finished,
                            finishOnce = finishOnce,
                        )
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    mainHandler.post {
                        if (generation != requestGeneration.get() || finished.get()) return@post
                        AdsLoadLog.failed(slotLabel, rcParam, unitId, error)
                        finishOnce("load_failed")
                    }
                }
            },
        )
    }

    private fun showLoadedAd(
        activity: Activity,
        ad: InterstitialAd,
        unitId: String,
        slotLabel: String,
        rcParam: String,
        generation: Int,
        finished: AtomicBoolean,
        finishOnce: (String) -> Unit,
    ) {
        if (!canShowOnActivity(activity)) {
            finishOnce("cannot show")
            return
        }

        FullscreenAdLifecycle.markFullscreenAdShowing()
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdsLoadLog.shown(slotLabel, rcParam, unitId)
                }

                override fun onAdDismissedFullScreenContent() {
                    // Cross / dismiss → navigate only. Never request again.
                    AdsLoadLog.showedAndClosed(slotLabel, rcParam, unitId)
                    ad.fullScreenContentCallback = null
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    if (generation != requestGeneration.get()) return
                    finishOnce("dismissed")
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdsLoadLog.showFailed(slotLabel, rcParam, unitId, adError)
                    ad.fullScreenContentCallback = null
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    if (generation != requestGeneration.get() || finished.get()) return
                    finishOnce("show_failed")
                }
            }

        try {
            ad.show(activity)
        } catch (t: Throwable) {
            AdsLoadLog.trace(slotLabel, "show threw: ${t.message}")
            ad.fullScreenContentCallback = null
            FullscreenAdLifecycle.markFullscreenAdDismissed()
            finishOnce("show_exception")
        }
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun presentLoadingDialog(activity: Activity) {
        dismissLoadingDialog()
        if (!canShowOnActivity(activity)) return
        val dark = ThemePreferences.isDarkMode(activity)
        val titleColor = if (dark) "#FFFFFFFF".toColorInt() else "#1C1B1F".toColorInt()
        val accent = "#7C4DFF".toColorInt()
        val density = activity.resources.displayMetrics.density

        val dialog =
            Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }
        val root = activity.layoutInflater.inflate(R.layout.dialog_ad_loading, null)
        val card = root.findViewById<LinearLayout>(R.id.ad_loading_card)
        card.setBackgroundResource(
            if (dark) R.drawable.bg_ad_loading_dialog_dark else R.drawable.bg_ad_loading_dialog_light,
        )
        root.findViewById<ProgressBar>(R.id.ad_loading_progress).indeterminateTintList =
            android.content.res.ColorStateList.valueOf(accent)
        root.findViewById<TextView>(R.id.ad_loading_title).setTextColor(titleColor)

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (292 * density).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        dialog.window?.setDimAmount(if (dark) 0.72f else 0.48f)
        runCatching { dialog.show() }
        loadingDialog = dialog
    }

    private fun dismissLoadingDialog() {
        val d = loadingDialog
        loadingDialog = null
        if (d == null) return
        runCatching {
            if (d.isShowing) d.dismiss()
        }
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
