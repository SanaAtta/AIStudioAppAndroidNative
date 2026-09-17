package com.aiartgenerator.imagegenerator.videogenerator.model.network

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Tries generation providers in order (Pollinations → DeAPI → Grok).
 * Skips providers that are not configured. Never runs providers in parallel.
 */
object GenerationFailover {
    private const val TAG = "GenerationFailover"

    suspend fun <T> tryProviders(
        label: String,
        providers: List<ProviderAttempt<T>>,
    ): T {
        var lastError: Throwable? = null
        var attempted = 0
        for ((index, attempt) in providers.withIndex()) {
            if (!attempt.isAvailable()) {
                Log.d(TAG, "$label: skip ${attempt.name} (not configured)")
                continue
            }
            attempted++
            try {
                Log.i(TAG, "$label: trying ${attempt.name} (${index + 1}/${providers.size})")
                val result =
                    if (attempt.timeoutMs > 0L) {
                        withTimeout(attempt.timeoutMs) { attempt.block() }
                    } else {
                        attempt.block()
                    }
                Log.i(TAG, "$label: success via ${attempt.name}")
                return result
            } catch (e: TimeoutCancellationException) {
                val wrapped = IOException("${attempt.name} timed out", e)
                Log.w(TAG, "$label: ${attempt.name} timed out — failover", e)
                lastError = wrapped
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "$label: ${attempt.name} failed — failover: ${e.message}")
                lastError = e
            }
        }
        throw (lastError as? Exception)
            ?: IOException(
                if (attempted == 0) {
                    "$label failed: no API keys configured (Pollinations / deAPI / Grok)"
                } else {
                    "$label failed after trying $attempted provider(s)"
                },
            )
    }

    data class ProviderAttempt<T>(
        val name: String,
        val isAvailable: () -> Boolean,
        val timeoutMs: Long = 0L,
        val block: suspend () -> T,
    )
}
