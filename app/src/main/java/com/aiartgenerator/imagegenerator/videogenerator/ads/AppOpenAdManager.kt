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
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Splash app-open: one preload per cold start via [startSplashLoad] (from [SplashScreen] only).
 * Resume app-open: triggered from [ResumeAppOpenCoordinator] on process ON_START
 * (loading overlay → load → show).
 */
object AppOpenAdManager {
    private const val MAX_CACHE_HOURS = 4L
    private const val RESUME_SHOW_WAIT_MS = 3_500L
    /** Min gap between resume app-open load requests (reduces no-fill / low show-rate churn). */
    private const val MIN_RESUME_LOAD_INTERVAL_MS = 60_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var splashAppOpenAd: AppOpenAd? = null
    private var splashLoadTimeMs: Long = 0
    private var splashLoadInFlight = false
    /** One AdMob request per cold start — no retry from [showSplashAppOpenIfPreloaded]. */
    private val splashLoadAttempted = AtomicBoolean(false)
    private val splashLoadListeners = CopyOnWriteArrayList<SplashLoadListener>()

    private var resumeAppOpenAd: AppOpenAd? = null
    private var resumeLoadTimeMs: Long = 0
    private var resumeLoadInFlight = false
    private var lastResumeLoadRequestMs: Long = 0
    private val resumeLoadListeners = CopyOnWriteArrayList<ResumeLoadListener>()
    private val resumeWelcomeFlowInFlight = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)

    private fun interface SplashLoadListener {
        fun onComplete(success: Boolean)
    }

    private fun interface ResumeLoadListener {
        fun onComplete(success: Boolean)
    }

    /** @deprecated Use [startSplashLoad]. */
    fun load(context: Context) {
        startSplashLoad(context)
    }

    /** Single-flight splash app-open load. Call once when the splash screen is shown. */
    fun startSplashLoad(context: Context) {
        requestSplashAppOpenLoad(context.applicationContext, null)
    }

    suspend fun awaitSplashAppOpenReady(
        context: Context,
        timeoutMs: Long,
    ): Boolean {
        if (isSplashAppOpenReady()) return true
        return withTimeoutOrNull(timeoutMs) {
            val deferred = CompletableDeferred<Boolean>()
            requestSplashAppOpenLoad(
                context.applicationContext,
                object : SplashLoadListener {
                    override fun onComplete(success: Boolean) {
                        val ready = success && isSplashAppOpenReady()
                        deferred.complete(ready)
                    }
                },
            )
            deferred.await()
        } == true
    }

    fun isSplashAppOpenReady(): Boolean =
        splashAppOpenAd != null && !splashCacheExpired()

    private fun deliverSplashLoadResult(success: Boolean) {
        splashLoadInFlight = false
        val listeners = splashLoadListeners.toList()
        splashLoadListeners.clear()
        listeners.forEach { listener ->
            runCatching { listener.onComplete(success) }
        }
    }

    private fun requestSplashAppOpenLoad(
        app: Context,
        listener: SplashLoadListener?,
    ) {
        MobileAdsInitializer.runWhenReady(
            Runnable { requestSplashAppOpenLoadOnMain(app, listener) },
        )
    }

    private fun requestSplashAppOpenLoadOnMain(
        app: Context,
        listener: SplashLoadListener?,
    ) {
        mainHandler.post {
            if (!AdsControl.splashAppOpenEnabled()) {
                listener?.onComplete(false)
                return@post
            }
            val unitId = AdUnitIds.splashAppOpen().trim()
            if (unitId.isBlank()) {
                listener?.onComplete(false)
                return@post
            }
            if (splashAppOpenAd != null && !splashCacheExpired()) {
                listener?.onComplete(true)
                return@post
            }
            if (splashLoadInFlight) {
                listener?.let { splashLoadListeners.add(it) }
                return@post
            }
            if (splashLoadAttempted.get()) {
                listener?.onComplete(false)
                return@post
            }
            listener?.let { splashLoadListeners.add(it) }
            splashLoadAttempted.set(true)
            splashLoadInFlight = true
            AdsLoadLog.requesting("splash app open ad", AdUnitIds.Rc.SPLASH_APP_OPEN, unitId)

            AppOpenAd.load(
                app,
                unitId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        AdsLoadLog.loaded("splash app open ad", AdUnitIds.Rc.SPLASH_APP_OPEN, unitId)
                        splashAppOpenAd = ad
                        splashLoadTimeMs = Date().time
                        deliverSplashLoadResult(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AdsLoadLog.failed("splash app open ad", AdUnitIds.Rc.SPLASH_APP_OPEN, unitId, loadAdError)
                        splashAppOpenAd = null
                        deliverSplashLoadResult(false)
                    }
                },
            )
        }
    }

    private fun splashCacheExpired(): Boolean {
        val loaded = splashLoadTimeMs
        if (loaded <= 0L) return true
        val diffHours = (Date().time - loaded) / 3_600_000L
        return diffHours >= MAX_CACHE_HOURS
    }

    private fun resumeCacheExpired(): Boolean {
        val loaded = resumeLoadTimeMs
        if (loaded <= 0L) return true
        val diffHours = (Date().time - loaded) / 3_600_000L
        return diffHours >= MAX_CACHE_HOURS
    }

    /**
     * Resume Open App Ad: load (or reuse a warm cache) then show.
     * [onAdReadyToShow] runs immediately before [AppOpenAd.show] so the loading overlay can dismiss.
     * [onFinished] runs after dismiss, show failure, timeout, or skip.
     */
    fun loadAndShowResumeAppOpen(
        activity: Activity,
        onAdReadyToShow: Runnable? = null,
        onFinished: Runnable,
    ) {
        if (!resumeWelcomeFlowInFlight.compareAndSet(false, true)) {
            onFinished.run()
            return
        }
        MobileAdsInitializer.runWhenReady(
            Runnable {
                mainHandler.post {
                    loadAndShowResumeAppOpenOnMain(activity, onAdReadyToShow) {
                        resumeWelcomeFlowInFlight.set(false)
                        onFinished.run()
                    }
                }
            },
        )
    }

    private fun loadAndShowResumeAppOpenOnMain(
        activity: Activity,
        onAdReadyToShow: Runnable?,
        onFinished: Runnable,
    ) {
        if (!AdsControl.resumeAppOpenEnabled()) {
            onFinished.run()
            return
        }
        if (!canShowAppOpen(activity)) {
            onFinished.run()
            return
        }
        val cached = resumeAppOpenAd
        if (cached != null && !resumeCacheExpired()) {
            resumeAppOpenAd = null
            showResumeInternal(activity, cached, onAdReadyToShow, onFinished)
            return
        }
        waitForResumeLoadThenShow(activity, onAdReadyToShow, onFinished)
    }

    private fun deliverResumeLoadResult(success: Boolean) {
        resumeLoadInFlight = false
        val listeners = resumeLoadListeners.toList()
        resumeLoadListeners.clear()
        listeners.forEach { listener ->
            runCatching { listener.onComplete(success) }
        }
    }

    private fun requestResumeAppOpenLoad(
        app: Context,
        listener: ResumeLoadListener?,
    ) {
        MobileAdsInitializer.runWhenReady(
            Runnable { requestResumeAppOpenLoadOnMain(app, listener) },
        )
    }

    private fun requestResumeAppOpenLoadOnMain(
        app: Context,
        listener: ResumeLoadListener?,
    ) {
        mainHandler.post {
            if (!AdsControl.resumeAppOpenEnabled()) {
                listener?.onComplete(false)
                return@post
            }
            val unitId = AdUnitIds.appOpenResume().trim()
            if (unitId.isBlank()) {
                listener?.onComplete(false)
                return@post
            }
            if (resumeAppOpenAd != null && !resumeCacheExpired()) {
                listener?.onComplete(true)
                return@post
            }
            if (resumeLoadInFlight) {
                listener?.let { resumeLoadListeners.add(it) }
                return@post
            }
            val now = System.currentTimeMillis()
            if (now - lastResumeLoadRequestMs < MIN_RESUME_LOAD_INTERVAL_MS) {
                listener?.onComplete(false)
                return@post
            }
            listener?.let { resumeLoadListeners.add(it) }
            lastResumeLoadRequestMs = now
            resumeLoadInFlight = true
            resumeAppOpenAd = null
            AdsLoadLog.requesting("resume app open ad", AdUnitIds.Rc.APP_OPEN_RESUME, unitId)

            AppOpenAd.load(
                app,
                unitId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        AdsLoadLog.loaded("resume app open ad", AdUnitIds.Rc.APP_OPEN_RESUME, unitId)
                        resumeAppOpenAd = ad
                        resumeLoadTimeMs = Date().time
                        deliverResumeLoadResult(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AdsLoadLog.failed("resume app open ad", AdUnitIds.Rc.APP_OPEN_RESUME, unitId, loadAdError)
                        resumeAppOpenAd = null
                        deliverResumeLoadResult(false)
                    }
                },
            )
        }
    }

    private fun waitForResumeLoadThenShow(
        activity: Activity,
        onAdReadyToShow: Runnable?,
        onFinished: Runnable,
    ) {
        val timedOut = AtomicBoolean(false)
        val timeoutRunnable =
            Runnable {
                if (timedOut.compareAndSet(false, true)) {
                    onFinished.run()
                }
            }
        mainHandler.postDelayed(timeoutRunnable, RESUME_SHOW_WAIT_MS)
        requestResumeAppOpenLoad(
            activity.applicationContext,
            object : ResumeLoadListener {
                override fun onComplete(success: Boolean) {
                    if (timedOut.get()) return
                    mainHandler.removeCallbacks(timeoutRunnable)
                    val ad = resumeAppOpenAd
                    if (success && ad != null && !resumeCacheExpired() && canShowAppOpen(activity)) {
                        resumeAppOpenAd = null
                        showResumeInternal(activity, ad, onAdReadyToShow, onFinished)
                    } else {
                        onFinished.run()
                    }
                }
            },
        )
    }

    private fun showResumeInternal(
        activity: Activity,
        ad: AppOpenAd,
        onAdReadyToShow: Runnable?,
        onFinished: Runnable,
    ) {
        if (!canShowAppOpen(activity)) {
            resumeAppOpenAd = ad
            onFinished.run()
            return
        }
        if (!isShowing.compareAndSet(false, true)) {
            resumeAppOpenAd = ad
            onFinished.run()
            return
        }
        FullscreenAdLifecycle.markFullscreenAdShowing()
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdsLoadLog.shown(
                        "resume app open ad",
                        AdUnitIds.Rc.APP_OPEN_RESUME,
                        AdUnitIds.appOpenResume(),
                    )
                }

                override fun onAdDismissedFullScreenContent() {
                    AdsLoadLog.showedAndClosed(
                        "resume app open ad",
                        AdUnitIds.Rc.APP_OPEN_RESUME,
                        AdUnitIds.appOpenResume(),
                    )
                    ad.fullScreenContentCallback = null
                    isShowing.set(false)
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    onFinished.run()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdsLoadLog.showFailed(
                        "resume app open ad",
                        AdUnitIds.Rc.APP_OPEN_RESUME,
                        AdUnitIds.appOpenResume(),
                        adError,
                    )
                    isShowing.set(false)
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    onFinished.run()
                }
            }
        onAdReadyToShow?.run()
        ad.show(activity)
    }

    private fun canShowAppOpen(activity: Activity): Boolean {
        if (activity.isFinishing) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed) {
            return false
        }
        if (activity is LifecycleOwner) {
            return activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        return true
    }

    /** Shows the single splash preload if ready; never starts a second request. */
    fun showSplashAppOpenIfPreloaded(activity: Activity, onFinished: Runnable) {
        mainHandler.post {
            if (!AdsControl.splashAppOpenEnabled()) {
                onFinished.run()
                return@post
            }
            showSplashAppOpenFromCacheInternal(activity, onFinished)
        }
    }

    /** @deprecated Use [showSplashAppOpenIfPreloaded] after the splash timer. */
    fun showSplashAppOpenFromCacheIfReady(activity: Activity, onFinished: Runnable) {
        showSplashAppOpenIfPreloaded(activity, onFinished)
    }

    /** @deprecated Use [showSplashAppOpenIfPreloaded]; does not issue a second load. */
    fun showSplashAppOpenThenContinue(activity: Activity, onFinished: Runnable) {
        showSplashAppOpenIfPreloaded(activity, onFinished)
    }

    private fun showSplashAppOpenFromCacheInternal(activity: Activity, onFinished: Runnable) {
        val ad = splashAppOpenAd ?: run {
            onFinished.run()
            return
        }
        if (splashCacheExpired()) {
            splashAppOpenAd = null
            onFinished.run()
            return
        }
        splashAppOpenAd = null
        if (!canShowAppOpen(activity)) {
            onFinished.run()
            return
        }
        if (!isShowing.compareAndSet(false, true)) {
            onFinished.run()
            return
        }
        FullscreenAdLifecycle.markFullscreenAdShowing()
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    AdsLoadLog.shown(
                        "splash app open ad",
                        AdUnitIds.Rc.SPLASH_APP_OPEN,
                        AdUnitIds.splashAppOpen(),
                    )
                }

                override fun onAdDismissedFullScreenContent() {
                    AdsLoadLog.showedAndClosed(
                        "splash app open ad",
                        AdUnitIds.Rc.SPLASH_APP_OPEN,
                        AdUnitIds.splashAppOpen(),
                    )
                    ad.fullScreenContentCallback = null
                    isShowing.set(false)
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    onFinished.run()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdsLoadLog.showFailed(
                        "splash app open ad",
                        AdUnitIds.Rc.SPLASH_APP_OPEN,
                        AdUnitIds.splashAppOpen(),
                        adError,
                    )
                    isShowing.set(false)
                    FullscreenAdLifecycle.markFullscreenAdDismissed()
                    onFinished.run()
                }
            }
        ad.show(activity)
    }
}
