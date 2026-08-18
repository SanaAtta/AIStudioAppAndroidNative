package com.example.myapplication.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.example.myapplication.BuildConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Replaces [com.google.firebase.provider.FirebaseInitProvider] so Firebase (and Crashlytics
 * `getBuildIdInfo` → `String.format` → ICU currency symbols) runs off the main thread during
 * [android.app.Application.onCreate], not during `handleBindApplication`.
 */
object FirebaseBootstrap {
    private const val TAG = "FirebaseBootstrap"

    private val readyLatch = CountDownLatch(1)

    @Volatile
    private var startRequested = false

    @Volatile
    private var initFailed = false

    fun ensureStarted(context: Context) {
        val appContext = context.applicationContext
        if (startRequested) return
        synchronized(this) {
            if (startRequested) return
            startRequested = true
            Thread(
                {
                    val startedAt = System.currentTimeMillis()
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Firebase init starting on ${Thread.currentThread().name}")
                    }
                    runCatching {
                        FirebaseApp.initializeApp(appContext)
                    }.onFailure {
                        initFailed = true
                        Log.w(TAG, "Firebase init failed", it)
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "Firebase init finished in ${System.currentTimeMillis() - startedAt}ms " +
                                "(failed=$initFailed)",
                        )
                    }
                    readyLatch.countDown()
                },
                "firebase-bootstrap",
            ).apply { isDaemon = true }.start()
        }
    }

    fun isReady(): Boolean = readyLatch.count == 0L

    suspend fun awaitReady(timeoutMs: Long = 20_000L): Boolean =
        withContext(Dispatchers.IO) {
            if (isReady()) return@withContext !initFailed
            val ok = readyLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
            ok && !initFailed
        }

    fun awaitReadyBlocking(timeoutMs: Long = 20_000L): Boolean {
        if (isReady()) return !initFailed
        return readyLatch.await(timeoutMs, TimeUnit.MILLISECONDS) && !initFailed
    }
}
