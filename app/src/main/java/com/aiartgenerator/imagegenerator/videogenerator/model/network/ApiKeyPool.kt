package com.aiartgenerator.imagegenerator.videogenerator.model.network

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Rotates through up to 5 Remote Config API keys per provider when credits / auth fail.
 */
object ApiKeyPool {
    private const val TAG = "ApiKeyPool"
    private const val PREFS = "api_key_pool"
    private const val MAX_KEYS = 5

    enum class Provider(val prefsIndexKey: String, val prefsFingerprintKey: String) {
        POLLINATIONS("idx_pollinations", "fp_pollinations"),
        DEAPI("idx_deapi", "fp_deapi"),
        GROQ("idx_groq", "fp_groq"),
        GROK("idx_grok", "fp_grok"),
    }

    @Volatile
    private var appContext: Context? = null

    private val indices = mapOf(
        Provider.POLLINATIONS to AtomicInteger(0),
        Provider.DEAPI to AtomicInteger(0),
        Provider.GROQ to AtomicInteger(0),
        Provider.GROK to AtomicInteger(0),
    )

    private val mutex = Mutex()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        Provider.entries.forEach { provider ->
            val prefs = prefs() ?: return@forEach
            indices.getValue(provider).set(prefs.getInt(provider.prefsIndexKey, 0).coerceAtLeast(0))
        }
    }

    fun currentKey(provider: Provider): String {
        val keys = keys(provider)
        if (keys.isEmpty()) return ""
        val index = indices.getValue(provider).get().coerceIn(0, keys.lastIndex)
        return keys[index]
    }

    @Volatile
    var testKeys: Map<Provider, List<String>>? = null

    fun keys(provider: Provider): List<String> {
        testKeys?.get(provider)?.let { return it.take(MAX_KEYS) }
        return when (provider) {
            Provider.POLLINATIONS -> ApiRemoteConfig.pollinationsApiKeys
            Provider.DEAPI -> ApiRemoteConfig.deapiApiKeys
            Provider.GROQ -> ApiRemoteConfig.groqApiKeys
            Provider.GROK -> ApiRemoteConfig.grokApiKeys
        }.take(MAX_KEYS)
    }

    fun hasAnyKey(provider: Provider): Boolean = keys(provider).isNotEmpty()

    fun syncAfterRemoteConfig() {
        Provider.entries.forEach { provider ->
            val keys = keys(provider)
            val fingerprint = keys.joinToString("|")
            val prefs = prefs()
            val previous = prefs?.getString(provider.prefsFingerprintKey, null)
            if (previous != fingerprint) {
                indices.getValue(provider).set(0)
                prefs?.edit()
                    ?.putString(provider.prefsFingerprintKey, fingerprint)
                    ?.putInt(provider.prefsIndexKey, 0)
                    ?.apply()
                Log.d(TAG, "${provider.name}: key list updated (${keys.size} keys), reset to index 0")
            } else {
                val clamped = indices.getValue(provider).get().coerceIn(0, (keys.size - 1).coerceAtLeast(0))
                indices.getValue(provider).set(clamped)
            }
            logStatus(provider)
        }
    }

    /**
     * Runs [block]; on credit/auth failure advances to the next key and retries
     * until all configured keys for [provider] have been tried.
     */
    suspend fun <T> withFallback(provider: Provider, block: suspend () -> T): T {
        val allKeys = keys(provider)
        val keyCount = allKeys.size
        if (keyCount == 0) {
            throw IOException("${provider.name} API key missing. Add keys in Firebase api_config.")
        }
        var lastError: IOException? = null
        var attempts = 0
        while (attempts < keyCount) {
            val currentIndex = indices.getValue(provider).get().coerceIn(0, keyCount - 1)
            try {
                return block()
            } catch (e: IOException) {
                lastError = e
                val canRotate = ApiErrorParser.isKeyFailoverMessage(e.message)
                if (!canRotate) throw e
                
                if (keyCount > 1) {
                    val nextIndex = (currentIndex + 1) % keyCount
                    mutex.withLock {
                        indices.getValue(provider).set(nextIndex)
                        prefs()?.edit()?.putInt(provider.prefsIndexKey, nextIndex)?.apply()
                    }
                    Log.w(
                        TAG,
                        "${provider.name} key ${currentIndex + 1} failed → trying key ${nextIndex + 1} (${attempts + 2}/$keyCount). reason=${e.message?.take(100)}",
                    )
                }
                attempts++
            }
        }
        throw lastError ?: IOException("${provider.name} request failed after $keyCount keys")
    }

    private fun advanceLocked(provider: Provider): Boolean {
        val allKeys = keys(provider)
        if (allKeys.size <= 1) return false
        val current = indices.getValue(provider).get()
        val next = (current + 1) % allKeys.size
        indices.getValue(provider).set(next)
        prefs()?.edit()?.putInt(provider.prefsIndexKey, next)?.apply()
        Log.d(TAG, "${provider.name}: active key index $current → $next")
        return true
    }

    private fun logStatus(provider: Provider) {
        val keys = keys(provider)
        val index = indices.getValue(provider).get().coerceIn(0, (keys.size - 1).coerceAtLeast(0))
        Log.d(
            TAG,
            "${provider.name}: ${keys.size} key(s), activeIndex=$index",
        )
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
