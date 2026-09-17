package com.aiartgenerator.imagegenerator.videogenerator.model.network

object AiConfig {
    val POLLINATIONS_API_KEY: String get() = ApiKeyPool.currentKey(ApiKeyPool.Provider.POLLINATIONS)
    val POLLINATIONS_BASE_URL: String get() = ApiRemoteConfig.pollinationsBaseUrl
    val POLLINATIONS_EDIT_URL: String get() = ApiRemoteConfig.pollinationsEditUrl
    val POLLINATIONS_CHAT_URL: String get() = ApiRemoteConfig.pollinationsChatUrl
    val POLLINATIONS_CHAT_MODEL: String get() = ApiRemoteConfig.pollinationsChatModel
    const val POLLINATIONS_KEYS_URL = "https://enter.pollinations.ai"
    /** Image generate model from RC `pollinations_image_model` (default flux). */
    val IMAGE_MODEL: String get() = ApiRemoteConfig.pollinationsImageModel
    /** Image edit model from RC `pollinations_edit_model` (default kontext). */
    val EDIT_MODEL: String get() = ApiRemoteConfig.pollinationsEditModel

    val DEAPI_API_KEY: String get() = ApiKeyPool.currentKey(ApiKeyPool.Provider.DEAPI)
    val DEAPI_BASE_URL: String get() = ApiRemoteConfig.deapiBaseUrl
    val DEAPI_VIDEO_URL: String get() = ApiRemoteConfig.deapiVideoUrl
    val DEAPI_VIDEO_ANIMATION_URL: String get() = ApiRemoteConfig.deapiVideoAnimationUrl
    val DEAPI_MUSIC_URL: String get() = ApiRemoteConfig.deapiMusicUrl
    val DEAPI_SPEECH_URL: String get() = ApiRemoteConfig.deapiSpeechUrl
    val DEAPI_JOBS_URL: String get() = ApiRemoteConfig.deapiJobsUrl
    val DEAPI_IMAGE_URL: String get() = ApiRemoteConfig.deapiImageUrl
    val DEAPI_IMAGE_EDIT_URL: String get() = ApiRemoteConfig.deapiImageEditUrl
    val DEAPI_CHAT_URL: String get() = ApiRemoteConfig.deapiChatUrl
    val DEAPI_CHAT_MODEL: String get() = ApiRemoteConfig.deapiChatModel
    const val DEAPI_KEYS_URL = "https://deapi.ai"
    const val DEAPI_VIDEO_MODEL_LTX2_5 = "Ltx2_5_22B_Dist_INT8"
    /** Default text-to-video generation model for deAPI. */
    const val DEAPI_VIDEO_MODEL = DEAPI_VIDEO_MODEL_LTX2_5
    const val DEAPI_MUSIC_MODEL = "AceStep_1_5_XL_Turbo_INT8"
    const val DEAPI_COVER_TTS_MODEL = "Qwen3_TTS_12Hz_1_7B_Base"
    const val DEAPI_IMAGE_MODEL = "Flux1schnell"

    val GROQ_API_KEY: String get() = ApiKeyPool.currentKey(ApiKeyPool.Provider.GROQ)
    val GROQ_BASE_URL: String get() = ApiRemoteConfig.groqBaseUrl
    val GROQ_CHAT_URL: String get() = ApiRemoteConfig.groqChatUrl
    val CHAT_MODEL: String get() = ApiRemoteConfig.groqChatModel

    val GROK_API_KEY: String get() = ApiKeyPool.currentKey(ApiKeyPool.Provider.GROK)
    val GROK_BASE_URL: String get() = ApiRemoteConfig.grokBaseUrl
    val GROK_CHAT_URL: String get() = ApiRemoteConfig.grokChatUrl
    val GROK_CHAT_MODEL: String get() = ApiRemoteConfig.grokChatModel
    val GROK_IMAGE_URL: String get() = ApiRemoteConfig.grokImageUrl
    val GROK_IMAGE_MODEL: String get() = ApiRemoteConfig.grokImageModel

    val isPollinationsConfigured: Boolean
        get() = ApiKeyPool.hasAnyKey(ApiKeyPool.Provider.POLLINATIONS)

    val isDeApiConfigured: Boolean
        get() = ApiKeyPool.hasAnyKey(ApiKeyPool.Provider.DEAPI)

    val isGroqConfigured: Boolean
        get() = ApiKeyPool.hasAnyKey(ApiKeyPool.Provider.GROQ)

    val isGrokConfigured: Boolean
        get() = ApiKeyPool.hasAnyKey(ApiKeyPool.Provider.GROK)

    /** True when any chat provider in Grok → Groq → Pollinations → DeAPI chain is available. */
    val isChatConfigured: Boolean
        get() = isGrokConfigured || isGroqConfigured || isPollinationsConfigured || isDeApiConfigured

    /** True when any image provider in the Pollinations → DeAPI → Grok chain is available. */
    val isImageGenerationConfigured: Boolean
        get() = isPollinationsConfigured || isDeApiConfigured || isGrokConfigured

    val isImageEditConfigured: Boolean
        get() = isPollinationsConfigured || isDeApiConfigured

    val isVideoGenerationConfigured: Boolean
        get() = isDeApiConfigured

    val isMusicGenerationConfigured: Boolean
        get() = isDeApiConfigured

    val isCoverGenerationConfigured: Boolean
        get() = isDeApiConfigured

    @Deprecated("Use provider-specific flags", ReplaceWith("isPollinationsConfigured"))
    val isApiConfigured: Boolean
        get() = isPollinationsConfigured

    fun isValidApiKey(value: String): Boolean {
        val key = value.trim()
        if (key.length < 8) return false
        if (key.contains("://")) return false
        if (key.startsWith("http", ignoreCase = true)) return false
        if (key.endsWith(".ai") || key.endsWith(".com")) return false
        return true
    }
}
