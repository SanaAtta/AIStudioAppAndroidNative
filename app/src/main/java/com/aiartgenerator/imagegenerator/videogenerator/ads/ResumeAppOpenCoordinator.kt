package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-resume Open App Ad: Process ON_START → loading overlay → request → show (or dismiss on fail).
 * Skips cold start, premium, RC-off, and returns from other fullscreen ads.
 */
object ResumeAppOpenCoordinator : DefaultLifecycleObserver {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _showLoading = MutableStateFlow(false)
    val showLoading: StateFlow<Boolean> = _showLoading.asStateFlow()

    private val started = AtomicBoolean(false)
    private val flowInFlight = AtomicBoolean(false)
    private val coldStartSkipped = AtomicBoolean(false)

    @Volatile
    private var hostReadyForResumeAds = false

    private var activityRef: WeakReference<Activity>? = null

    fun init(application: Application) {
        if (!started.compareAndSet(false, true)) return
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    activityRef = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
                override fun onActivityStarted(a: Activity) = Unit
                override fun onActivityStopped(a: Activity) = Unit
                override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) {
                    if (activityRef?.get() === activity) {
                        activityRef = null
                    }
                }
            },
        )
    }

    /** Call once splash (and launch gates) are past so resume ads don't fight cold-start splash. */
    fun setHostReadyForResumeAds(ready: Boolean) {
        hostReadyForResumeAds = ready
    }

    fun bindActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        // First process start = cold launch (splash owns ads).
        if (!coldStartSkipped.getAndSet(true)) return
        mainHandler.post { onAppResumed() }
    }

    private fun onAppResumed() {
        if (!hostReadyForResumeAds) return
        if (PremiumAccess.isPremiumUser()) return
        if (!AdsControl.resumeAppOpenEnabled()) return
        if (FullscreenAdLifecycle.shouldSuppressResumeWelcomeFlow()) return
        if (!flowInFlight.compareAndSet(false, true)) return

        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing) {
            flowInFlight.set(false)
            return
        }

        _showLoading.value = true
        AppOpenAdManager.loadAndShowResumeAppOpen(
            activity = activity,
            onAdReadyToShow = Runnable {
                // Hide loading immediately before the fullscreen ad appears.
                _showLoading.value = false
            },
            onFinished = Runnable {
                _showLoading.value = false
                flowInFlight.set(false)
            },
        )
    }

    fun dismissLoading() {
        _showLoading.value = false
    }
}
