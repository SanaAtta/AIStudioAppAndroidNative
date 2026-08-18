package com.example.myapplication.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Single-flight Mobile Ads SDK startup.
 *
 * Call [start] after UMP consent on splash. [MobileAds.initialize] always runs on
 * [Dispatchers.Default] (requires manifest OPTIMIZE_INITIALIZATION=true).
 * Ad UI callbacks are posted back to the main looper.
 */
object MobileAdsInitializer {
    private const val INIT_CALLBACK_TIMEOUT_MS = 15_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)
    private val initializing = AtomicBoolean(false)
    private val ready = AtomicBoolean(false)
    private val readyDeferred = CompletableDeferred<Unit>()
    private val pendingMainTasks = CopyOnWriteArrayList<Runnable>()

    val isReady: Boolean
        get() = ready.get()

    /** Call once after UMP consent (splash). Safe to call multiple times. */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        scheduleInitialize(context.applicationContext)
    }

    private fun scheduleInitialize(appContext: Context) {
        initScope.launch {
            initializeIfNeeded(appContext)
        }
    }

    private suspend fun initializeIfNeeded(appContext: Context) {
        if (ready.get() || !initializing.compareAndSet(false, true)) return
        val initResult =
            runCatching {
                withContext(Dispatchers.Default) {
                    check(Looper.myLooper() != Looper.getMainLooper()) {
                        "MobileAds.initialize must not run on the main thread"
                    }
                    val initDone = CompletableDeferred<InitializationStatus?>()
                    MobileAds.initialize(appContext) { status ->
                        if (!initDone.isCompleted) {
                            initDone.complete(status)
                        }
                    }
                    withTimeout(INIT_CALLBACK_TIMEOUT_MS) {
                        initDone.await()
                    }
                }
            }
        mainHandler.post {
            onMobileAdsInitialized(initResult.getOrNull())
        }
    }

    private fun onMobileAdsInitialized(status: InitializationStatus?) {
        if (!ready.compareAndSet(false, true)) return
        if (!readyDeferred.isCompleted) {
            readyDeferred.complete(Unit)
        }
        val tasks = pendingMainTasks.toList()
        pendingMainTasks.clear()
        tasks.forEach { task -> mainHandler.post(task) }
    }

    /** Runs [task] on the main looper after the SDK is ready (ad UI must stay on main). */
    fun runWhenReady(task: Runnable) {
        if (ready.get()) {
            mainHandler.post(task)
            return
        }
        pendingMainTasks.add(task)
    }

    fun runWhenReady(block: () -> Unit) {
        runWhenReady(Runnable { runCatching { block() } })
    }

    suspend fun awaitReady(timeoutMs: Long = 10_000L): Boolean {
        if (ready.get()) return true
        return withTimeoutOrNull(timeoutMs) {
            readyDeferred.await()
            true
        } ?: false
    }
}
