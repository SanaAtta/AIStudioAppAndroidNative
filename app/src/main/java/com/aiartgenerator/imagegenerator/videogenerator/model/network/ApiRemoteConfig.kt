package com.aiartgenerator.imagegenerator.videogenerator.model.network

import android.content.Context
import android.util.Log
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject

object ApiRemoteConfig {
    private const val TAG = "ApiRemoteConfig"
    private const val KEY_API_CONFIG_JSON = "api_config"

    const val KEY_POLLINATIONS_API_KEY = "pollinations_api_key"
    const val KEY_POLLINATIONS_API_KEYS = "pollinations_api_keys"
    const val KEY_POLLINATIONS_BASE_URL = "pollinations_base_url"
    const val KEY_POLLINATIONS_EDIT_URL = "pollinations_edit_url"
    const val KEY_POLLINATIONS_IMAGE_MODEL = "pollinations_image_model"
    const val KEY_POLLINATIONS_EDIT_MODEL = "pollinations_edit_model"
    const val KEY_POLLINATIONS_CHAT_URL = "pollinations_chat_url"
    const val KEY_POLLINATIONS_CHAT_MODEL = "pollinations_chat_model"
    const val KEY_DEAPI_API_KEY = "deapi_api_key"
    const val KEY_DEAPI_API_KEYS = "deapi_api_keys"
    const val KEY_DEAPI_BASE_URL = "deapi_base_url"
    const val KEY_DEAPI_VIDEO_URL = "deapi_video_url"
    const val KEY_DEAPI_VIDEO_ANIMATION_URL = "deapi_video_animation_url"
    const val KEY_DEAPI_MUSIC_URL = "deapi_music_url"
    const val KEY_DEAPI_SPEECH_URL = "deapi_speech_url"
    const val KEY_DEAPI_JOBS_URL = "deapi_jobs_url"
    const val KEY_DEAPI_IMAGE_URL = "deapi_image_url"
    const val KEY_DEAPI_IMAGE_EDIT_URL = "deapi_image_edit_url"
    const val KEY_DEAPI_CHAT_URL = "deapi_chat_url"
    const val KEY_DEAPI_CHAT_MODEL = "deapi_chat_model"
    const val KEY_GROQ_API_KEY = "groq_api_key"
    const val KEY_GROQ_API_KEYS = "groq_api_keys"
    const val KEY_GROQ_BASE_URL = "groq_base_url"
    const val KEY_GROQ_CHAT_URL = "groq_chat_url"
    const val KEY_GROQ_CHAT_MODEL = "groq_chat_model"
    const val KEY_GROK_API_KEY = "grok_api_key"
    const val KEY_GROK_API_KEYS = "grok_api_keys"
    const val KEY_GROK_BASE_URL = "grok_base_url"
    const val KEY_GROK_CHAT_URL = "grok_chat_url"
    const val KEY_GROK_CHAT_MODEL = "grok_chat_model"
    const val KEY_GROK_IMAGE_URL = "grok_image_url"
    const val KEY_GROK_IMAGE_MODEL = "grok_image_model"

    private const val MAX_FALLBACK_KEYS = 5

    private val legacyKeyAliases = mapOf(
        KEY_POLLINATIONS_API_KEY to listOf("pollinations.api.key"),
        KEY_DEAPI_API_KEY to listOf("deapi.api.key"),
        KEY_GROQ_API_KEY to listOf("groq.api.key"),
        KEY_GROK_API_KEY to listOf("grok.api.key", "xai.api.key", "xai_api_key"),
    )

    private val buildFallbackKeys = mapOf(
        KEY_POLLINATIONS_API_KEY to BuildConfig.FALLBACK_POLLINATIONS_API_KEY,
        KEY_DEAPI_API_KEY to BuildConfig.FALLBACK_DEAPI_API_KEY,
        KEY_GROQ_API_KEY to BuildConfig.FALLBACK_GROQ_API_KEY,
        KEY_GROK_API_KEY to BuildConfig.FALLBACK_GROK_API_KEY,
    )

    @Volatile
    private var initializeStarted = false

    private val defaultsReady = CompletableDeferred<Unit>()

    private val remoteConfig: FirebaseRemoteConfig? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { FirebaseRemoteConfig.getInstance() }.getOrNull()
    }

    fun initialize(context: Context) {
        if (initializeStarted) return
        synchronized(this) {
            if (initializeStarted) return
            initializeStarted = true
            val rc = remoteConfig ?: run {
                Log.w(TAG, "FirebaseRemoteConfig instance unavailable")
                if (!defaultsReady.isCompleted) {
                    defaultsReady.complete(Unit)
                }
                return
            }
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3_600)
                .build()
            rc.setConfigSettingsAsync(settings)
            rc.setDefaultsAsync(R.xml.remote_config_defaults)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Remote Config defaults loaded")
                    } else {
                        Log.w(TAG, "Remote Config defaults failed", task.exception)
                    }
                    if (!defaultsReady.isCompleted) {
                        defaultsReady.complete(Unit)
                    }
                    ApiKeyPool.syncAfterRemoteConfig()
                }
            Log.d(TAG, "Remote Config initialize started")
        }
    }

    private suspend fun awaitDefaultsReady() {
        if (!defaultsReady.isCompleted) {
            defaultsReady.await()
        }
    }

    suspend fun fetchAndActivate(): Boolean {
        awaitDefaultsReady()
        val rc = remoteConfig ?: return false
        return suspendCancellableCoroutine { continuation ->
            rc.fetchAndActivate()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Remote Config fetch and activate succeeded (updated=${task.result})")
                    } else {
                        Log.w(
                            TAG,
                            "Remote Config fetch failed; using cached/default values",
                            task.exception,
                        )
                    }
                    logFullConfigResponse()
                    ApiKeyPool.syncAfterRemoteConfig()
                    if (continuation.isActive) {
                        continuation.resume(task.isSuccessful)
                    }
                }
        }
    }

    /** Re-fetch from Firebase when keys are still empty (e.g. first fetch failed on splash). */
    suspend fun ensureFreshConfig(): Boolean {
        awaitDefaultsReady()
        if (AiConfig.isPollinationsConfigured &&
            AiConfig.isDeApiConfigured &&
            (AiConfig.isGroqConfigured || AiConfig.isGrokConfigured)
        ) {
            return true
        }
        Log.d(TAG, "API keys missing after load — retrying Remote Config fetch")
        val fetched = fetchAndActivate()
        if (!AiConfig.isPollinationsConfigured) {
            logMissingKeyDiagnostics(KEY_POLLINATIONS_API_KEY)
        }
        return fetched || AiConfig.isPollinationsConfigured
    }

    private fun resolveValue(primaryKey: String, vararg extraKeys: String): String {
        val remoteCandidates = buildList {
            add(primaryKey)
            addAll(extraKeys)
            legacyKeyAliases[primaryKey]?.let(::addAll)
        }
        for (key in remoteCandidates.distinct()) {
            val remoteValue = remoteConfig?.getString(key)?.trim().orEmpty()
            if (remoteValue.isNotEmpty()) {
                return remoteValue
            }
        }
        val fromJson = valueFromJsonBlob(primaryKey)
        if (fromJson.isNotEmpty()) {
            return fromJson
        }
        return buildFallbackKeys[primaryKey].orEmpty().trim()
    }

    private fun valueFromJsonBlob(jsonKey: String): String {
        val blob = remoteConfig?.getString(KEY_API_CONFIG_JSON)?.trim().orEmpty()
        if (blob.isEmpty()) return ""
        return runCatching {
            JSONObject(blob).optString(jsonKey, "").trim()
        }.getOrElse { "" }
    }

    private fun apiConfigJson(): JSONObject? {
        val blob = remoteConfig?.getString(KEY_API_CONFIG_JSON)?.trim().orEmpty()
        if (blob.isEmpty()) return null
        return runCatching { JSONObject(blob) }.getOrNull()
    }

    /**
     * Up to 5 keys from `*_api_keys` array, numbered `*_api_key_2`…`_5`, and singular `*_api_key`.
     */
    private fun resolveApiKeys(
        singularKey: String,
        arrayKey: String,
        buildFallback: String,
    ): List<String> {
        val collected = LinkedHashSet<String>()

        fun addIfValid(raw: String?) {
            val key = raw?.trim().orEmpty()
            if (AiConfig.isValidApiKey(key)) {
                collected.add(key)
            }
        }

        val json = apiConfigJson()
        json?.optJSONArray(arrayKey)?.let { arr ->
            for (i in 0 until minOf(arr.length(), MAX_FALLBACK_KEYS)) {
                addIfValid(arr.optString(i))
            }
        }

        // Top-level Remote Config JSON array param (rare, but supported)
        runCatching {
            val raw = remoteConfig?.getString(arrayKey)?.trim().orEmpty()
            if (raw.startsWith("[")) {
                val arr = JSONArray(raw)
                for (i in 0 until minOf(arr.length(), MAX_FALLBACK_KEYS)) {
                    addIfValid(arr.optString(i))
                }
            }
        }

        addIfValid(resolveValue(singularKey))
        addIfValid(json?.optString(singularKey))
        for (n in 1..MAX_FALLBACK_KEYS) {
            val numbered = "${singularKey}_$n"
            addIfValid(remoteConfig?.getString(numbered))
            addIfValid(json?.optString(numbered))
            val numberedNoUnder = "${singularKey}$n"
            addIfValid(remoteConfig?.getString(numberedNoUnder))
            addIfValid(json?.optString(numberedNoUnder))
        }

        if (collected.isEmpty()) {
            addIfValid(buildFallback)
        }
        return collected.take(MAX_FALLBACK_KEYS).toList()
    }

    val pollinationsApiKeys: List<String>
        get() = resolveApiKeys(
            singularKey = KEY_POLLINATIONS_API_KEY,
            arrayKey = KEY_POLLINATIONS_API_KEYS,
            buildFallback = BuildConfig.FALLBACK_POLLINATIONS_API_KEY,
        )

    val deapiApiKeys: List<String>
        get() = resolveApiKeys(
            singularKey = KEY_DEAPI_API_KEY,
            arrayKey = KEY_DEAPI_API_KEYS,
            buildFallback = BuildConfig.FALLBACK_DEAPI_API_KEY,
        )

    val groqApiKeys: List<String>
        get() = resolveApiKeys(
            singularKey = KEY_GROQ_API_KEY,
            arrayKey = KEY_GROQ_API_KEYS,
            buildFallback = BuildConfig.FALLBACK_GROQ_API_KEY,
        )

    val grokApiKeys: List<String>
        get() = resolveApiKeys(
            singularKey = KEY_GROK_API_KEY,
            arrayKey = KEY_GROK_API_KEYS,
            buildFallback = BuildConfig.FALLBACK_GROK_API_KEY,
        )

    val pollinationsApiKey: String get() = pollinationsApiKeys.firstOrNull().orEmpty()
    val pollinationsBaseUrl: String
        get() = resolveValue(KEY_POLLINATIONS_BASE_URL).ifBlank { "https://gen.pollinations.ai" }
    val pollinationsEditUrl: String
        get() = resolveValue(KEY_POLLINATIONS_EDIT_URL).ifBlank {
            "${pollinationsBaseUrl.trimEnd('/')}/v1/images/edits"
        }
    val pollinationsImageModel: String
        get() = resolveValue(KEY_POLLINATIONS_IMAGE_MODEL).ifBlank { "flux" }
    val pollinationsEditModel: String
        get() = resolveValue(KEY_POLLINATIONS_EDIT_MODEL).ifBlank { "kontext" }
    val pollinationsChatUrl: String
        get() = resolveValue(KEY_POLLINATIONS_CHAT_URL).ifBlank {
            "${pollinationsBaseUrl.trimEnd('/')}/v1/chat/completions"
        }
    val pollinationsChatModel: String
        get() = resolveValue(KEY_POLLINATIONS_CHAT_MODEL).ifBlank { "openai" }
    val deapiApiKey: String get() = deapiApiKeys.firstOrNull().orEmpty()
    val deapiBaseUrl: String
        get() = resolveValue(KEY_DEAPI_BASE_URL).ifBlank { "https://api.deapi.ai" }
    val deapiVideoUrl: String
        get() = resolveValue(KEY_DEAPI_VIDEO_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/videos/generations"
        }
    val deapiVideoAnimationUrl: String
        get() = resolveValue(KEY_DEAPI_VIDEO_ANIMATION_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/videos/animations"
        }
    val deapiMusicUrl: String
        get() = resolveValue(KEY_DEAPI_MUSIC_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/audio/music"
        }
    val deapiSpeechUrl: String
        get() = resolveValue(KEY_DEAPI_SPEECH_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/audio/speech"
        }
    val deapiJobsUrl: String
        get() = resolveValue(KEY_DEAPI_JOBS_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/jobs"
        }
    val deapiImageUrl: String
        get() = resolveValue(KEY_DEAPI_IMAGE_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/images/generations"
        }
    val deapiImageEditUrl: String
        get() = resolveValue(KEY_DEAPI_IMAGE_EDIT_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/images/edits"
        }
    val deapiChatUrl: String
        get() = resolveValue(KEY_DEAPI_CHAT_URL).ifBlank {
            "${deapiBaseUrl.trimEnd('/')}/api/v2/chat/completions"
        }
    val deapiChatModel: String
        get() = resolveValue(KEY_DEAPI_CHAT_MODEL).ifBlank { "llama-3.3-70b" }
    val groqApiKey: String get() = groqApiKeys.firstOrNull().orEmpty()
    val groqBaseUrl: String
        get() = resolveValue(KEY_GROQ_BASE_URL).ifBlank { "https://api.groq.com/openai/v1" }
    val groqChatUrl: String
        get() = resolveValue(KEY_GROQ_CHAT_URL).ifBlank {
            "${groqBaseUrl.trimEnd('/')}/chat/completions"
        }
    val groqChatModel: String
        get() = resolveValue(KEY_GROQ_CHAT_MODEL).ifBlank { "llama-3.3-70b-versatile" }
    val grokApiKey: String get() = grokApiKeys.firstOrNull().orEmpty()
    val grokBaseUrl: String
        get() = resolveValue(KEY_GROK_BASE_URL).ifBlank { "https://api.x.ai/v1" }
    val grokChatUrl: String
        get() = resolveValue(KEY_GROK_CHAT_URL).ifBlank {
            "${grokBaseUrl.trimEnd('/')}/chat/completions"
        }
    val grokChatModel: String
        get() = resolveValue(KEY_GROK_CHAT_MODEL).ifBlank { "grok-2-latest" }
    val grokImageUrl: String
        get() = resolveValue(KEY_GROK_IMAGE_URL).ifBlank {
            "${grokBaseUrl.trimEnd('/')}/images/generations"
        }
    val grokImageModel: String
        get() = resolveValue(KEY_GROK_IMAGE_MODEL).ifBlank { "grok-imagine-image" }

    fun deapiJobUrl(requestId: String): String =
        "${deapiJobsUrl.trimEnd('/')}/$requestId"

    fun logConfigStatus() {
        logFullConfigResponse()
    }

    fun logFullConfigResponse() {
        val keys = listOf(
            KEY_POLLINATIONS_API_KEY,
            KEY_POLLINATIONS_BASE_URL,
            KEY_POLLINATIONS_EDIT_URL,
            KEY_POLLINATIONS_IMAGE_MODEL,
            KEY_POLLINATIONS_EDIT_MODEL,
            KEY_POLLINATIONS_CHAT_URL,
            KEY_POLLINATIONS_CHAT_MODEL,
            KEY_DEAPI_API_KEY,
            KEY_DEAPI_BASE_URL,
            KEY_DEAPI_VIDEO_URL,
            KEY_DEAPI_VIDEO_ANIMATION_URL,
            KEY_DEAPI_MUSIC_URL,
            KEY_DEAPI_SPEECH_URL,
            KEY_DEAPI_JOBS_URL,
            KEY_DEAPI_IMAGE_URL,
            KEY_DEAPI_IMAGE_EDIT_URL,
            KEY_DEAPI_CHAT_URL,
            KEY_DEAPI_CHAT_MODEL,
            KEY_GROQ_API_KEY,
            KEY_GROQ_BASE_URL,
            KEY_GROQ_CHAT_URL,
            KEY_GROQ_CHAT_MODEL,
            KEY_GROK_API_KEY,
            KEY_GROK_BASE_URL,
            KEY_GROK_CHAT_URL,
            KEY_GROK_CHAT_MODEL,
            KEY_GROK_IMAGE_URL,
            KEY_GROK_IMAGE_MODEL,
        )
        Log.d(TAG, "======== Remote Config Response ========")
        Log.d(TAG, "Firebase keys received: ${remoteConfig?.all?.keys?.sorted().orEmpty()}")
        keys.forEach { key ->
            logResolvedKey(key)
        }
        Log.d(
            TAG,
            "fallback keys: pollinations=${pollinationsApiKeys.size}, " +
                "deapi=${deapiApiKeys.size}, groq=${groqApiKeys.size}, grok=${grokApiKeys.size}",
        )
        Log.d(
            TAG,
            "pollinations configured=${AiConfig.isPollinationsConfigured}, " +
                "deapi configured=${AiConfig.isDeApiConfigured}, " +
                "groq configured=${AiConfig.isGroqConfigured}, " +
                "grok configured=${AiConfig.isGrokConfigured}",
        )
        Log.d(TAG, "========================================")
    }

    private fun logResolvedKey(key: String) {
        val resolved = when (key) {
            KEY_POLLINATIONS_API_KEY -> pollinationsApiKey
            KEY_POLLINATIONS_BASE_URL -> pollinationsBaseUrl
            KEY_POLLINATIONS_EDIT_URL -> pollinationsEditUrl
            KEY_POLLINATIONS_IMAGE_MODEL -> pollinationsImageModel
            KEY_POLLINATIONS_EDIT_MODEL -> pollinationsEditModel
            KEY_POLLINATIONS_CHAT_URL -> pollinationsChatUrl
            KEY_POLLINATIONS_CHAT_MODEL -> pollinationsChatModel
            KEY_DEAPI_API_KEY -> deapiApiKey
            KEY_DEAPI_BASE_URL -> deapiBaseUrl
            KEY_DEAPI_VIDEO_URL -> deapiVideoUrl
            KEY_DEAPI_VIDEO_ANIMATION_URL -> deapiVideoAnimationUrl
            KEY_DEAPI_MUSIC_URL -> deapiMusicUrl
            KEY_DEAPI_SPEECH_URL -> deapiSpeechUrl
            KEY_DEAPI_JOBS_URL -> deapiJobsUrl
            KEY_DEAPI_IMAGE_URL -> deapiImageUrl
            KEY_DEAPI_IMAGE_EDIT_URL -> deapiImageEditUrl
            KEY_DEAPI_CHAT_URL -> deapiChatUrl
            KEY_DEAPI_CHAT_MODEL -> deapiChatModel
            KEY_GROQ_API_KEY -> groqApiKey
            KEY_GROQ_BASE_URL -> groqBaseUrl
            KEY_GROQ_CHAT_URL -> groqChatUrl
            KEY_GROQ_CHAT_MODEL -> groqChatModel
            KEY_GROK_API_KEY -> grokApiKey
            KEY_GROK_BASE_URL -> grokBaseUrl
            KEY_GROK_CHAT_URL -> grokChatUrl
            KEY_GROK_CHAT_MODEL -> grokChatModel
            KEY_GROK_IMAGE_URL -> grokImageUrl
            KEY_GROK_IMAGE_MODEL -> grokImageModel
            else -> resolveValue(key)
        }
        val display = if (isSecretKey(key)) maskSecret(resolved) else resolved.ifBlank { "(empty)" }
        Log.d(TAG, "$key | resolved=$display")
    }

    private fun logMissingKeyDiagnostics(key: String) {
        Log.e(TAG, "Missing $key after Remote Config refresh")
        Log.e(TAG, "Expected Firebase param: $key (or legacy pollinations.api.key / api_config JSON)")
        Log.e(TAG, "Firebase project: ai-image-generator-3a636")
        Log.e(TAG, "All Firebase params: ${remoteConfig?.all?.keys?.sorted().orEmpty()}")
        remoteConfig?.all?.forEach { (param, configValue) ->
            if (param.contains("pollinat", ignoreCase = true) ||
                param.contains("api", ignoreCase = true) ||
                param == KEY_API_CONFIG_JSON
            ) {
                val raw = configValue.asString().trim()
                val display = if (param.endsWith("_api_key") || param.contains(".api.key")) {
                    maskSecret(raw)
                } else {
                    raw.take(120).ifBlank { "(empty)" }
                }
                Log.e(TAG, "  $param | source=${sourceLabel(configValue.source)} | value=$display")
            }
        }
        val fallback = buildFallbackKeys[key].orEmpty().trim()
        Log.e(
            TAG,
            "Local build fallback for $key: ${if (fallback.isEmpty()) "not set" else maskSecret(fallback)}",
        )
    }

    private fun isSecretKey(key: String): Boolean = key.endsWith("_api_key")

    private fun maskSecret(value: String): String {
        if (value.isEmpty()) return "(empty)"
        if (value.length <= 8) return "*** (${value.length} chars)"
        return "${value.take(6)}...${value.takeLast(4)} (${value.length} chars)"
    }

    private fun sourceLabel(source: Int): String = when (source) {
        2 -> "REMOTE"
        1 -> "DEFAULT"
        0 -> "STATIC"
        else -> "UNKNOWN($source)"
    }
}
