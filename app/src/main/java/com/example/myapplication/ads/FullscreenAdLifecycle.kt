package com.example.myapplication.ads

import java.util.concurrent.atomic.AtomicInteger

/**
 * Full-screen ads (interstitial / app open) pause the activity; on dismiss [ON_START] fires and must
 * not be treated as a user “return from background” (welcome overlay / resume app open).
 */
object FullscreenAdLifecycle {
    private val showingCount = AtomicInteger(0)

    @Volatile
    private var suppressResumeFlowUntilMs: Long = 0L

    private const val POST_AD_GRACE_MS = 2_500L

    fun markFullscreenAdShowing() {
        showingCount.incrementAndGet()
    }

    fun markFullscreenAdDismissed() {
        val left = showingCount.decrementAndGet().coerceAtLeast(0)
        if (left == 0) {
            suppressResumeFlowUntilMs = System.currentTimeMillis() + POST_AD_GRACE_MS
        }
    }

    fun shouldSuppressResumeWelcomeFlow(): Boolean =
        showingCount.get() > 0 || System.currentTimeMillis() < suppressResumeFlowUntilMs

    fun isFullscreenAdShowing(): Boolean = showingCount.get() > 0
}
