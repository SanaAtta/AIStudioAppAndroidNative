package com.aiartgenerator.imagegenerator.videogenerator.model.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aiartgenerator.imagegenerator.videogenerator.controller.PromptSafety
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverAudioConverter
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverCatalog
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class AiApi(
    private val client: okhttp3.OkHttpClient = AiHttpClient.http,
) {
    private companion object {
        private const val TAG = "AiApi"
    }

    suspend fun generateImage(
        prompt: String,
        styleHint: String = "",
        width: Int = 1024,
        height: Int = 1024,
        model: String = AiConfig.IMAGE_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            PromptSafety.requireSafe(prompt)
            val fullPrompt = PromptSafety.withVisualSafety(buildPrompt(prompt, styleHint))
            GenerationFailover.tryProviders(
                label = "text-to-image",
                providers = listOf(
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.POLLINATIONS,
                        isAvailable = { AiConfig.isPollinationsConfigured },
                        timeoutMs = 3 * 60_000L,
                    ) {
                        ApiKeyPool.withFallback(ApiKeyPool.Provider.POLLINATIONS) {
                            generateImageWithPollinations(fullPrompt, width, height, model)
                        }
                    },
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.DEAPI,
                        isAvailable = { AiConfig.isDeApiConfigured },
                        timeoutMs = DeApiClient.imagePollTimeoutMs + 30_000L,
                    ) {
                        ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                            generateImageWithDeApi(fullPrompt, width, height)
                        }
                    },
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.GROK,
                        isAvailable = { AiConfig.isGrokConfigured },
                        timeoutMs = 3 * 60_000L,
                    ) {
                        ApiKeyPool.withFallback(ApiKeyPool.Provider.GROK) {
                            generateImageWithGrok(fullPrompt, width, height)
                        }
                    },
                ),
            )
        }
    }

    private fun generateImageWithPollinations(
        fullPrompt: String,
        width: Int,
        height: Int,
        model: String,
    ): ByteArray {
        ApiLogger.logProvider(TAG, ApiProvider.POLLINATIONS, "text-to-image")
        val seed = (System.currentTimeMillis() and 0x7FFFFFFF).toInt().coerceAtLeast(1)
        val url = buildPollinationsUrl(
            path = "/image/${encode(fullPrompt)}",
            query = mapOf(
                "model" to model,
                "width" to width.toString(),
                "height" to height.toString(),
                "nologo" to "true",
                "seed" to seed.toString(),
                "private" to "true",
                "safe" to "nsfw",
            ),
        )
        val bytes = downloadBytes(url, ApiProvider.POLLINATIONS)
        validateImageBytes(bytes, ApiProvider.POLLINATIONS)
        return bytes
    }

    private suspend fun generateImageWithDeApi(
        fullPrompt: String,
        width: Int,
        height: Int,
    ): ByteArray {
        ApiLogger.logProvider(TAG, ApiProvider.DEAPI, "text-to-image")
        val w = width.coerceIn(512, 1536)
        val h = height.coerceIn(512, 1536)
        val payload = JSONObject().apply {
            put("prompt", fullPrompt)
            put("negative_prompt", PromptSafety.VISUAL_NEGATIVE_PROMPT)
            put("model", AiConfig.DEAPI_IMAGE_MODEL)
            put("width", w)
            put("height", h)
            put("steps", 4)
            put("guidance", 3.5)
            put("seed", -1)
        }
        val bytes = DeApiClient.runJsonJob(
            client = client,
            endpointUrl = AiConfig.DEAPI_IMAGE_URL,
            payload = payload,
            timeoutMs = DeApiClient.imagePollTimeoutMs,
        )
        validateImageBytes(bytes, ApiProvider.DEAPI)
        return bytes
    }

    private fun generateImageWithGrok(
        fullPrompt: String,
        width: Int,
        height: Int,
    ): ByteArray {
        ApiLogger.logProvider(TAG, ApiProvider.GROK, "text-to-image")
        val payload = JSONObject().apply {
            put("model", AiConfig.GROK_IMAGE_MODEL)
            put("prompt", fullPrompt)
            put("n", 1)
            put("response_format", "b64_json")
            aspectRatioForSize(width, height)?.let { put("aspect_ratio", it) }
        }
        val url = AiConfig.GROK_IMAGE_URL
        ApiLogger.logRequest(TAG, ApiProvider.GROK, "POST", url, payload.toString())
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val contentType = response.header("Content-Type")
            if (!response.isSuccessful) {
                ApiLogger.logError(TAG, ApiProvider.GROK, url, response.code, raw)
                throw IOException(errorMessage(response.code, raw, ApiProvider.GROK))
            }
            ApiLogger.logTextResponse(TAG, ApiProvider.GROK, url, response.code, contentType, raw)
            return parseOpenAiStyleImageResponse(raw, ApiProvider.GROK)
        }
    }

    private fun aspectRatioForSize(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio in 0.95f..1.05f -> "1:1"
            ratio >= 1.7f -> "16:9"
            ratio <= 0.6f -> "9:16"
            ratio >= 1.25f -> "4:3"
            ratio <= 0.8f -> "3:4"
            else -> null
        }
    }

    suspend fun editImage(
        context: Context,
        imageUri: Uri,
        prompt: String,
        styleHint: String = "",
        model: String = AiConfig.EDIT_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            if (!AiConfig.isImageEditConfigured) {
                throw IOException(
                    "No image edit API keys configured. Add Pollinations, deAPI, or Grok keys in Remote Config.",
                )
            }
            PromptSafety.requireSafe(prompt)
            val fullPrompt = PromptSafety.withVisualSafety(buildPrompt(prompt, styleHint))
            val imageBytes = readUriBytes(context, imageUri)
            GenerationFailover.tryProviders(
                label = "image-edit",
                providers = listOf(
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.POLLINATIONS,
                        isAvailable = { AiConfig.isPollinationsConfigured },
                        timeoutMs = 3 * 60_000L,
                    ) {
                        ApiKeyPool.withFallback(ApiKeyPool.Provider.POLLINATIONS) {
                            editImageWithPollinations(imageBytes, fullPrompt, model)
                        }
                    },
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.DEAPI,
                        isAvailable = { AiConfig.isDeApiConfigured },
                        timeoutMs = DeApiClient.imagePollTimeoutMs + 30_000L,
                    ) {
                        ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                            editImageWithDeApi(imageBytes, fullPrompt)
                        }
                    },
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.GROK,
                        // Reference-image edit via Grok Imagine is not wired yet; skip for edit.
                        isAvailable = { false },
                        timeoutMs = 3 * 60_000L,
                        block = { throw IOException("Grok image edit not available") },
                    ),
                ),
            )
        }
    }

    private fun editImageWithPollinations(
        imageBytes: ByteArray,
        fullPrompt: String,
        model: String,
    ): ByteArray {
        ApiLogger.logProvider(TAG, ApiProvider.POLLINATIONS, "image-to-image edit")
        val temp = File.createTempFile("edit_input", ".jpg").apply {
            writeBytes(imageBytes)
        }
        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("prompt", fullPrompt)
                .addFormDataPart("model", model)
                .addFormDataPart("size", "1024x1024")
                .addFormDataPart("response_format", "b64_json")
                .addFormDataPart(
                    "image",
                    temp.name,
                    temp.asRequestBody("image/jpeg".toMediaType()),
                )
                .build()

            val editUrl = AiConfig.POLLINATIONS_EDIT_URL
            ApiLogger.logRequest(
                TAG,
                ApiProvider.POLLINATIONS,
                "POST",
                editUrl,
                requestBody = "(multipart edit, model=$model)",
            )

            val request = Request.Builder()
                .url(editUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val contentType = response.header("Content-Type")
                if (!response.isSuccessful) {
                    ApiLogger.logError(TAG, ApiProvider.POLLINATIONS, editUrl, response.code, raw)
                    throw IOException(errorMessage(response.code, raw, ApiProvider.POLLINATIONS))
                }
                ApiLogger.logTextResponse(TAG, ApiProvider.POLLINATIONS, editUrl, response.code, contentType, raw)
                return parsePollinationsImageResponse(raw)
            }
        } finally {
            temp.delete()
        }
    }

    private suspend fun editImageWithDeApi(
        imageBytes: ByteArray,
        fullPrompt: String,
    ): ByteArray {
        ApiLogger.logProvider(TAG, ApiProvider.DEAPI, "image-to-image edit")
        val temp = File.createTempFile("edit_deapi", ".jpg").apply {
            writeBytes(imageBytes)
        }
        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("prompt", fullPrompt)
                .addFormDataPart("model", AiConfig.DEAPI_IMAGE_MODEL)
                .addFormDataPart(
                    "image",
                    temp.name,
                    temp.asRequestBody("image/jpeg".toMediaType()),
                )
                .build()
            val bytes = DeApiClient.runMultipartJob(
                client = client,
                endpointUrl = AiConfig.DEAPI_IMAGE_EDIT_URL,
                body = body,
                timeoutMs = DeApiClient.imagePollTimeoutMs,
            )
            validateImageBytes(bytes, ApiProvider.DEAPI)
            return bytes
        } finally {
            temp.delete()
        }
    }

    suspend fun generateVideo(
        prompt: String,
        styleHint: String = "",
        durationSeconds: Int = 5,
        aspectRatio: String = "16:9",
        model: String = AiConfig.DEAPI_VIDEO_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            if (!AiConfig.isVideoGenerationConfigured) {
                throw IOException(
                    "deAPI key missing. Add deapi_api_keys in Firebase Remote Config.",
                )
            }
            PromptSafety.requireSafe(prompt)
            val fullPrompt = PromptSafety.withVisualSafety(buildPrompt(prompt, styleHint))
            val maxSupported = DeApiClient.maxVideoDurationSeconds(model)
            val clampedDuration = durationSeconds.coerceIn(2, maxSupported)
            // Order: Pollinations (N/A) → DeAPI → Grok (N/A until video endpoint is configured).
            GenerationFailover.tryProviders(
                label = "text-to-video",
                providers = listOf(
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.POLLINATIONS,
                        isAvailable = { false },
                        block = { throw IOException("Pollinations video not available") },
                    ),
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.DEAPI,
                        isAvailable = { AiConfig.isDeApiConfigured },
                        timeoutMs = DeApiClient.videoPollTimeoutMs + 60_000L,
                    ) {
                        ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                            generateVideoWithDeApi(
                                fullPrompt = fullPrompt,
                                durationSeconds = clampedDuration,
                                aspectRatio = aspectRatio,
                                model = model,
                            )
                        }
                    },
                    GenerationFailover.ProviderAttempt(
                        name = ApiProvider.GROK,
                        isAvailable = { false },
                        block = { throw IOException("Grok video not available") },
                    ),
                ),
            )
        }
    }

    private suspend fun generateVideoWithDeApi(
        fullPrompt: String,
        durationSeconds: Int,
        aspectRatio: String,
        model: String,
    ): ByteArray {
        return generateVideoWithDeApiModel(
            fullPrompt = fullPrompt,
            durationSeconds = durationSeconds,
            aspectRatio = aspectRatio,
            model = model,
        )
    }

    private suspend fun generateVideoWithDeApiModel(
        fullPrompt: String,
        durationSeconds: Int,
        aspectRatio: String,
        model: String,
    ): ByteArray {
        ApiLogger.logProvider(TAG, ApiProvider.DEAPI, "text-to-video ($model)")
        val (width, height) = DeApiClient.videoDimensions(aspectRatio, model)
        val fps = DeApiClient.videoFps(model)
        val steps = DeApiClient.videoSteps(model)
        val payload = JSONObject().apply {
            put("prompt", fullPrompt)
            put("negative_prompt", PromptSafety.VISUAL_NEGATIVE_PROMPT)
            put("model", model)
            put("width", width)
            put("height", height)
            put("guidance", 7.5)
            put("steps", steps)
            put("seed", -1)
            put("frames", DeApiClient.videoFrames(durationSeconds, model))
            put("fps", fps)
        }
        Log.d(TAG, "deAPI text-to-video request (model=$model): $payload")
        val bytes = DeApiClient.runJsonJob(
            client = client,
            endpointUrl = AiConfig.DEAPI_VIDEO_URL,
            payload = payload,
            timeoutMs = DeApiClient.videoPollTimeoutMs,
        )
        validateVideoBytes(bytes, ApiProvider.DEAPI)
        return bytes
    }

    suspend fun generateVideoFromImage(
        context: Context,
        imageUri: Uri,
        prompt: String,
        styleHint: String = "",
        durationSeconds: Int = 5,
        aspectRatio: String = "16:9",
        model: String = AiConfig.DEAPI_VIDEO_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            PromptSafety.requireSafe(prompt)
            val fullPrompt = PromptSafety.withVisualSafety(buildPrompt(prompt, styleHint))
            val imageBytes = readUriBytes(context, imageUri)
            ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                val temp = File.createTempFile("video_frame", ".jpg").apply {
                    writeBytes(imageBytes)
                }
                try {
                    ApiLogger.logProvider(TAG, ApiProvider.DEAPI, "image-to-video")
                    val maxSupported = DeApiClient.maxVideoDurationSeconds(model)
                    val clampedDuration = durationSeconds.coerceIn(2, maxSupported)
                    val (width, height) = DeApiClient.videoDimensions(aspectRatio, model)
                    val fps = DeApiClient.videoFps(model)
                    val steps = DeApiClient.videoSteps(model)
                    val body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("prompt", fullPrompt)
                        .addFormDataPart("negative_prompt", PromptSafety.VISUAL_NEGATIVE_PROMPT)
                        .addFormDataPart("model", model)
                        .addFormDataPart("width", width.toString())
                        .addFormDataPart("height", height.toString())
                        .addFormDataPart("guidance", "7.5")
                        .addFormDataPart("steps", steps.toString())
                        .addFormDataPart("seed", "-1")
                        .addFormDataPart("frames", DeApiClient.videoFrames(clampedDuration, model).toString())
                        .addFormDataPart("fps", fps.toString())
                        .addFormDataPart(
                            "first_frame_image",
                            temp.name,
                            temp.asRequestBody("image/jpeg".toMediaType()),
                        )
                        .build()

                    Log.d(TAG, "deAPI image-to-video: model=$model, ${width}x$height")
                    val bytes = DeApiClient.runMultipartJob(
                        client = client,
                        endpointUrl = AiConfig.DEAPI_VIDEO_ANIMATION_URL,
                        body = body,
                        timeoutMs = DeApiClient.videoPollTimeoutMs,
                    )
                    validateVideoBytes(bytes, ApiProvider.DEAPI)
                    bytes
                } finally {
                    temp.delete()
                }
            }
        }
    }

    suspend fun generateMusic(
        prompt: String,
        genreHint: String = "",
        instrumental: Boolean = true,
        durationSeconds: Int = 30,
        model: String = AiConfig.DEAPI_MUSIC_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            PromptSafety.requireSafe(prompt)
            val caption = buildPrompt(prompt, genreHint)
            val clampedDuration = DeApiClient.musicDurationSeconds(durationSeconds)
            val lyrics = if (instrumental) "[Instrumental]" else buildMusicLyrics(prompt)

            ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("model", model)
                    .addFormDataPart("lyrics", lyrics)
                    .addFormDataPart("duration", clampedDuration.toString())
                    .addFormDataPart("inference_steps", "8")
                    .addFormDataPart("guidance_scale", "1")
                    .addFormDataPart("seed", "-1")
                    .addFormDataPart("format", "mp3")
                    .build()

                ApiLogger.logProvider(TAG, ApiProvider.DEAPI, "text-to-music ($model)")
                Log.d(
                    TAG,
                    "deAPI music request: model=$model, duration=${clampedDuration}s, instrumental=$instrumental",
                )
                val bytes = DeApiClient.runMultipartJob(
                    client = client,
                    endpointUrl = AiConfig.DEAPI_MUSIC_URL,
                    body = body,
                    timeoutMs = DeApiClient.musicPollTimeoutMs,
                )
                Log.d(TAG, "deAPI music response OK: ${bytes.size} bytes")
                bytes
            }
        }
    }

    suspend fun generateCover(
        context: Context,
        songUri: Uri,
        singerStyleId: String,
        voiceStyleId: String,
        singerStyleHint: String,
        voiceStyleHint: String,
        model: String = AiConfig.DEAPI_COVER_TTS_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val prepared = CoverAudioConverter.prepareWav(context, songUri)
            if (prepared.durationSeconds < 3) {
                throw IOException("Voice sample is too short. Use at least 10 seconds.")
            }

            val styleHint = listOfNotNull(
                singerStyleHint.ifBlank { null },
                voiceStyleHint.ifBlank { null },
            ).joinToString(", ")
            val coverText = CoverCatalog.coverPerformanceLyrics(singerStyleId, voiceStyleId)

            ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                val refBody = prepared.file.asRequestBody("audio/wav".toMediaType())
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("ref_audio", "cover_ref.wav", refBody)
                    .addFormDataPart("text", coverText)
                    .addFormDataPart("model", model)
                    .addFormDataPart("mode", "voice_clone")
                    .addFormDataPart("lang", "English")
                    .addFormDataPart("speed", "1")
                    .addFormDataPart("format", "mp3")
                    .addFormDataPart("sample_rate", "24000")
                    .build()

                ApiLogger.logProvider(TAG, ApiProvider.DEAPI, "ai-cover voice_clone ($model)")
                Log.d(
                    TAG,
                    "deAPI cover request: model=$model, singer=$singerStyleId, voice=$voiceStyleId, " +
                        "style=$styleHint, text=$coverText, ref=cover_ref.wav, " +
                        "duration=${prepared.durationSeconds}s, size=${prepared.file.length()}",
                )
                val bytes = DeApiClient.runMultipartJob(
                    client = client,
                    endpointUrl = AiConfig.DEAPI_SPEECH_URL,
                    body = body,
                    timeoutMs = DeApiClient.speechPollTimeoutMs,
                )
                Log.d(TAG, "deAPI cover response OK: ${bytes.size} bytes")
                bytes
            }
        }
    }

    suspend fun chat(
        messages: List<Pair<String, String>>,
        model: String = AiConfig.CHAT_MODEL,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val providers = buildList<GenerationFailover.ProviderAttempt<String>> {
                // Primary: Grok Chat API (or Groq if Grok key is not configured)
                if (AiConfig.isGrokConfigured) {
                    add(
                        GenerationFailover.ProviderAttempt(
                            name = ApiProvider.GROK,
                            isAvailable = { AiConfig.isGrokConfigured },
                            timeoutMs = 60_000L,
                        ) {
                            ApiKeyPool.withFallback(ApiKeyPool.Provider.GROK) {
                                chatWithGrok(messages, AiConfig.GROK_CHAT_MODEL)
                            }
                        },
                    )
                } else if (AiConfig.isGroqConfigured) {
                    add(
                        GenerationFailover.ProviderAttempt(
                            name = ApiProvider.GROQ,
                            isAvailable = { AiConfig.isGroqConfigured },
                            timeoutMs = 60_000L,
                        ) {
                            ApiKeyPool.withFallback(ApiKeyPool.Provider.GROQ) {
                                chatWithGroq(messages, model)
                            }
                        },
                    )
                } else {
                    add(
                        GenerationFailover.ProviderAttempt(
                            name = ApiProvider.GROK,
                            isAvailable = { false },
                            timeoutMs = 60_000L,
                            block = { throw IOException("Grok API key missing") },
                        ),
                    )
                }

                // If both Grok and Groq are configured, try Groq before DeAPI
                if (AiConfig.isGrokConfigured && AiConfig.isGroqConfigured) {
                    add(
                        GenerationFailover.ProviderAttempt(
                            name = ApiProvider.GROQ,
                            isAvailable = { AiConfig.isGroqConfigured },
                            timeoutMs = 60_000L,
                        ) {
                            ApiKeyPool.withFallback(ApiKeyPool.Provider.GROQ) {
                                chatWithGroq(messages, model)
                            }
                        },
                    )
                }

                // Fallback: DeAPI Chat API
                if (AiConfig.isDeApiConfigured) {
                    add(
                        GenerationFailover.ProviderAttempt(
                            name = ApiProvider.DEAPI,
                            isAvailable = { AiConfig.isDeApiConfigured },
                            timeoutMs = 60_000L,
                        ) {
                            ApiKeyPool.withFallback(ApiKeyPool.Provider.DEAPI) {
                                chatWithDeApi(messages, AiConfig.DEAPI_CHAT_MODEL)
                            }
                        },
                    )
                }

                // Fallback: Pollinations Chat API (verified OpenAI-compatible)
                if (AiConfig.isPollinationsConfigured) {
                    add(
                        GenerationFailover.ProviderAttempt(
                            name = ApiProvider.POLLINATIONS,
                            isAvailable = { AiConfig.isPollinationsConfigured },
                            timeoutMs = 60_000L,
                        ) {
                            ApiKeyPool.withFallback(ApiKeyPool.Provider.POLLINATIONS) {
                                chatWithPollinations(messages, AiConfig.POLLINATIONS_CHAT_MODEL)
                            }
                        },
                    )
                }
            }

            GenerationFailover.tryProviders(
                label = "text-to-text chat",
                providers = providers,
            )
        }
    }

    private fun chatWithGrok(
        messages: List<Pair<String, String>>,
        model: String = AiConfig.GROK_CHAT_MODEL,
    ): String = executeChatCompletion(
        provider = ApiProvider.GROK,
        url = AiConfig.GROK_CHAT_URL,
        model = model.ifBlank { "grok-2-latest" },
        messages = messages,
    )

    private fun chatWithGroq(
        messages: List<Pair<String, String>>,
        model: String = AiConfig.CHAT_MODEL,
    ): String = executeChatCompletion(
        provider = ApiProvider.GROQ,
        url = AiConfig.GROQ_CHAT_URL,
        model = model.ifBlank { "llama-3.3-70b-versatile" },
        messages = messages,
    )

    private fun chatWithDeApi(
        messages: List<Pair<String, String>>,
        model: String = AiConfig.DEAPI_CHAT_MODEL,
    ): String = executeChatCompletion(
        provider = ApiProvider.DEAPI,
        url = AiConfig.DEAPI_CHAT_URL,
        model = model.ifBlank { "llama-3.3-70b" },
        messages = messages,
    )

    private fun chatWithPollinations(
        messages: List<Pair<String, String>>,
        model: String = AiConfig.POLLINATIONS_CHAT_MODEL,
    ): String = executeChatCompletion(
        provider = ApiProvider.POLLINATIONS,
        url = AiConfig.POLLINATIONS_CHAT_URL,
        model = model.ifBlank { "openai" },
        messages = messages,
    )

    private fun executeChatCompletion(
        provider: String,
        url: String,
        model: String,
        messages: List<Pair<String, String>>,
    ): String {
        val payloadString = buildChatPayloadJson(model, messages)

        val request = Request.Builder()
            .url(url)
            .post(payloadString.toRequestBody("application/json".toMediaType()))
            .build()

        val chatUrl = request.url.toString()
        ApiLogger.logProvider(TAG, provider, "text-to-text chat")
        ApiLogger.logRequest(TAG, provider, "POST", chatUrl, payloadString)

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val contentType = response.header("Content-Type")
            if (!response.isSuccessful || ApiErrorParser.isKeyFailover(response.code, raw) || ApiErrorParser.isExplicitErrorBody(raw)) {
                ApiLogger.logError(TAG, provider, chatUrl, response.code, raw)
                throw IOException(errorMessage(response.code, raw, provider))
            }
            ApiLogger.logTextResponse(TAG, provider, chatUrl, response.code, contentType, raw)
            val reply = parseChatResponse(raw, provider)
            if (ApiErrorParser.isKeyFailoverMessage(reply) || ApiErrorParser.isCreditExhausted(response.code, reply)) {
                ApiLogger.logError(TAG, provider, chatUrl, response.code, reply)
                throw IOException(errorMessage(response.code, reply, provider))
            }
            return reply
        }
    }

    private fun buildChatPayloadJson(model: String, messages: List<Pair<String, String>>): String {
        val messagesJson = messages.joinToString(separator = ",", prefix = "[", postfix = "]") { (role, content) ->
            "{\"role\":\"${escapeJson(role)}\",\"content\":\"${escapeJson(content)}\"}"
        }
        return if (model.isNotBlank()) {
            "{\"model\":\"${escapeJson(model)}\",\"messages\":$messagesJson}"
        } else {
            "{\"messages\":$messagesJson}"
        }
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseChatResponse(raw: String, provider: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) throw IOException("Empty chat response from $provider")

        if (ApiErrorParser.isExplicitErrorBody(trimmed) || ApiErrorParser.isKeyFailover(200, trimmed)) {
            throw IOException(errorMessage(200, trimmed, provider))
        }

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                val json = JSONObject(trimmed)
                if (json.has("error") && !json.isNull("error")) {
                    val err = json.opt("error")
                    val errorMsg = when (err) {
                        is JSONObject -> err.optString("message").ifBlank { err.optString("detail").ifBlank { err.toString() } }
                        is String -> err
                        else -> err?.toString().orEmpty()
                    }
                    if (errorMsg.isNotBlank()) {
                        throw IOException(errorMessage(200, errorMsg, provider))
                    }
                }
                val status = json.optString("status")
                if (status.equals("error", ignoreCase = true) || status.equals("failed", ignoreCase = true)) {
                    val msg = json.optString("message").ifBlank { trimmed }
                    throw IOException(errorMessage(200, msg, provider))
                }
                if (json.has("success") && !json.optBoolean("success", true)) {
                    val msg = json.optString("message").ifBlank { trimmed }
                    throw IOException(errorMessage(200, msg, provider))
                }

                val choices = json.optJSONArray("choices")
                    ?: json.optJSONObject("data")?.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val first = choices.optJSONObject(0)
                    if (first != null) {
                        val content = first.optJSONObject("message")?.optString("content")?.takeIf { it.isNotBlank() }
                            ?: first.optJSONObject("delta")?.optString("content")?.takeIf { it.isNotBlank() }
                            ?: first.optString("text").takeIf { it.isNotBlank() }
                        if (content != null) {
                            if (ApiErrorParser.isKeyFailoverMessage(content) || ApiErrorParser.isCreditExhausted(200, content)) {
                                throw IOException(errorMessage(200, content, provider))
                            }
                            return content
                        }
                    }
                }

                val dataObj = json.optJSONObject("data")
                val textCandidate = listOfNotNull(
                    json.optString("reply").takeIf { it.isNotBlank() },
                    json.optString("response").takeIf { it.isNotBlank() },
                    json.optString("content").takeIf { it.isNotBlank() },
                    json.optString("text").takeIf { it.isNotBlank() },
                    json.optString("output").takeIf { it.isNotBlank() },
                    json.optString("result").takeIf { it.isNotBlank() },
                    dataObj?.let { it.optString("reply").takeIf { s -> s.isNotBlank() } },
                    dataObj?.let { it.optString("response").takeIf { s -> s.isNotBlank() } },
                    dataObj?.let { it.optString("content").takeIf { s -> s.isNotBlank() } },
                    dataObj?.let { it.optString("text").takeIf { s -> s.isNotBlank() } },
                    dataObj?.let { it.optString("output").takeIf { s -> s.isNotBlank() } },
                    dataObj?.let { it.optString("result").takeIf { s -> s.isNotBlank() } },
                    dataObj?.let { it.optString("message").takeIf { s -> s.isNotBlank() } },
                ).firstOrNull()

                if (textCandidate != null) {
                    if (ApiErrorParser.isKeyFailoverMessage(textCandidate) || ApiErrorParser.isCreditExhausted(200, textCandidate)) {
                        throw IOException(errorMessage(200, textCandidate, provider))
                    }
                    return textCandidate
                }

                val messageStr = json.optString("message")
                if (messageStr.isNotBlank() && !messageStr.startsWith("{")) {
                    if (ApiErrorParser.isKeyFailoverMessage(messageStr) || ApiErrorParser.isCreditExhausted(200, messageStr)) {
                        throw IOException(errorMessage(200, messageStr, provider))
                    }
                    return messageStr
                }
            } catch (e: IOException) {
                throw e
            } catch (_: Exception) {
                // Ignore JSON parse errors and continue to regex/plain text
            }

            // Resilient regex parser fallback for unit testing / non-standard JSON envelopes
            val contentRegex = Regex(""""content"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            contentRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let {
                val extracted = unescapeJson(it)
                if (ApiErrorParser.isKeyFailoverMessage(extracted) || ApiErrorParser.isCreditExhausted(200, extracted)) {
                    throw IOException(errorMessage(200, extracted, provider))
                }
                return extracted
            }
            val replyRegex = Regex(""""(?:reply|response|text|output|result)"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            replyRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let {
                val extracted = unescapeJson(it)
                if (ApiErrorParser.isKeyFailoverMessage(extracted) || ApiErrorParser.isCreditExhausted(200, extracted)) {
                    throw IOException(errorMessage(200, extracted, provider))
                }
                return extracted
            }
        }

        if (!trimmed.startsWith("<") && trimmed.isNotEmpty()) {
            if (ApiErrorParser.isKeyFailoverMessage(trimmed) || ApiErrorParser.isCreditExhausted(200, trimmed)) {
                throw IOException(errorMessage(200, trimmed, provider))
            }
            return trimmed
        }

        throw IOException("Invalid or empty chat response from $provider")
    }

    private fun unescapeJson(escaped: String): String {
        return escaped
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    fun imageUrl(
        prompt: String,
        width: Int = 1080,
        height: Int = 1920,
        model: String = AiConfig.IMAGE_MODEL,
    ): String = buildPollinationsUrl(
        path = "/image/${encode(prompt)}",
        query = mapOf(
            "model" to model,
            "width" to width.toString(),
            "height" to height.toString(),
            "nologo" to "true",
            "safe" to "nsfw",
        ),
    )

    suspend fun downloadUrl(url: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching { downloadBytes(url, providerForUrl(url)) }
    }

    private fun providerForUrl(url: String): String = when {
        url.contains("deapi.ai", ignoreCase = true) -> ApiProvider.DEAPI
        url.contains("groq.com", ignoreCase = true) -> ApiProvider.GROQ
        url.contains("x.ai", ignoreCase = true) -> ApiProvider.GROK
        url.contains("pollinations.ai", ignoreCase = true) -> ApiProvider.POLLINATIONS
        else -> "unknown"
    }

    fun saveToCache(context: Context, bytes: ByteArray, fileName: String): File {
        val dir = File(context.cacheDir, "generated").apply { mkdirs() }
        return File(dir, fileName).also { file ->
            file.outputStream().use { it.write(bytes) }
        }
    }

    private fun downloadBytes(url: String, provider: String): ByteArray {
        ApiLogger.logRequest(TAG, provider, "GET", url)
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type").orEmpty()
            val bytes = response.body?.bytes()
            if (!response.isSuccessful || bytes == null) {
                val message = bytes?.toString(Charsets.UTF_8).orEmpty()
                ApiLogger.logError(TAG, provider, url, response.code, message)
                throw IOException(errorMessage(response.code, message, provider))
            }
            if (contentType.contains("json", ignoreCase = true) ||
                contentType.contains("text", ignoreCase = true)
            ) {
                ApiLogger.logTextResponse(TAG, provider, url, response.code, contentType, bytes.toString(Charsets.UTF_8))
            } else {
                ApiLogger.logBinaryResponse(TAG, provider, url, response.code, contentType, bytes.size)
            }
            return bytes
        }
    }

    private fun downloadMusicBytes(url: String): ByteArray {
        val provider = ApiProvider.POLLINATIONS
        ApiLogger.logRequest(TAG, provider, "GET", url)
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type").orEmpty()
            val bytes = response.body?.bytes()
            if (!response.isSuccessful || bytes == null) {
                val message = bytes?.toString(Charsets.UTF_8).orEmpty()
                ApiLogger.logError(TAG, provider, url, response.code, message)
                throw IOException(errorMessage(response.code, message, provider))
            }
            if (contentType.contains("json", ignoreCase = true) ||
                contentType.contains("text", ignoreCase = true) ||
                contentType.contains("html", ignoreCase = true)
            ) {
                val textBody = bytes.toString(Charsets.UTF_8)
                ApiLogger.logTextResponse(TAG, provider, url, response.code, contentType, textBody)
                throw IOException(errorMessage(response.code, textBody, provider))
            }
            if (!looksLikeAudio(bytes)) {
                val preview = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
                if (preview.trimStart().startsWith("{") || preview.trimStart().startsWith("<")) {
                    ApiLogger.logTextResponse(TAG, provider, url, response.code, contentType, preview)
                    throw IOException(errorMessage(response.code, preview, provider))
                }
                Log.w(TAG, "Music response may not be audio: contentType=$contentType size=${bytes.size}")
            }
            ApiLogger.logBinaryResponse(TAG, provider, url, response.code, contentType, bytes.size)
            return bytes
        }
    }

    private fun looksLikeAudio(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // MP3: ID3 tag or frame sync
        if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return true
        }
        if (bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0) return true
        // WAV: RIFF....WAVE
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'A'.code.toByte()
        ) {
            return true
        }
        // OGG
        if (bytes[0] == 'O'.code.toByte() && bytes[1] == 'g'.code.toByte() &&
            bytes[2] == 'g'.code.toByte() && bytes[3] == 'S'.code.toByte()
        ) {
            return true
        }
        return bytes.size > 1024
    }

    private fun validateVideoBytes(bytes: ByteArray, provider: String) {
        val contentTypeGuess = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
        if (contentTypeGuess.trimStart().startsWith("{") || contentTypeGuess.trimStart().startsWith("<")) {
            throw IOException(errorMessage(200, contentTypeGuess, provider))
        }
        if (!looksLikeMp4(bytes)) {
            throw IOException("Video generation returned an invalid file")
        }
    }

    private fun validateImageBytes(bytes: ByteArray, provider: String) {
        val textPreview = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
        if (textPreview.trimStart().startsWith("{") || textPreview.trimStart().startsWith("<")) {
            throw IOException(errorMessage(200, textPreview, provider))
        }
        if (!looksLikeImage(bytes)) {
            throw IOException("Image generation returned an invalid file")
        }
    }

    private fun looksLikeImage(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        // JPEG
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return true
        }
        // PNG
        if (bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
        ) {
            return true
        }
        // WEBP (RIFF....WEBP)
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return true
        }
        // GIF
        if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
            return true
        }
        return false
    }

    private fun looksLikeMp4(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val ftyp = byteArrayOf('f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
        return bytes.copyOfRange(4, 8).contentEquals(ftyp)
    }

    private fun copyUriToCache(context: Context, uri: Uri, fileName: String): File {
        val dir = File(context.cacheDir, "generated").apply { mkdirs() }
        val file = File(dir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Unable to read selected file")
        return file
    }

    private fun readUriBytes(context: Context, uri: Uri): ByteArray {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return input.readBytes()
        } ?: throw IOException("Unable to read selected file")
    }

    private fun buildPrompt(prompt: String, hint: String): String {
        val clean = prompt.trim()
        val extra = hint.trim()
        return if (extra.isEmpty()) clean else "$clean, $extra"
    }

    private fun buildMusicLyrics(prompt: String): String {
        val line = prompt.trim().take(120).ifBlank { "Feel the rhythm through the night" }
        return "[verse]\r\n$line\r\n[chorus]\r\n$line"
    }

    private fun readAudioDurationMs(context: Context, uri: Uri): Int? = runCatching {
        android.media.MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toIntOrNull()
        }
    }.getOrNull()

    private fun buildPollinationsUrl(path: String, query: Map<String, String>): String {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "${AiConfig.POLLINATIONS_BASE_URL}$path?$queryString"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun parsePollinationsImageResponse(raw: String): ByteArray {
        val data = JSONObject(raw).optJSONArray("data")?.optJSONObject(0)
            ?: throw IOException("Empty image edit response")

        val b64 = data.optString("b64_json")
        if (b64.isNotBlank()) {
            return Base64.getDecoder().decode(b64)
        }

        val url = data.optString("url")
        if (url.isNotBlank()) {
            return downloadBytes(url, ApiProvider.POLLINATIONS)
        }

        throw IOException("No image data in edit response")
    }

    private fun parseOpenAiStyleImageResponse(raw: String, provider: String): ByteArray {
        val data = JSONObject(raw).optJSONArray("data")?.optJSONObject(0)
            ?: throw IOException("Empty image response")
        val b64 = data.optString("b64_json")
        if (b64.isNotBlank()) {
            val bytes = Base64.getDecoder().decode(b64)
            validateImageBytes(bytes, provider)
            return bytes
        }
        val url = data.optString("url")
        if (url.isNotBlank()) {
            val bytes = downloadBytes(url, provider)
            validateImageBytes(bytes, provider)
            return bytes
        }
        throw IOException("No image data in response")
    }

    private fun errorMessage(code: Int, body: String, provider: String): String {
        if (body.trim().isEmpty()) return "Request failed ($code)"
        return ApiErrorParser.userMessage(provider, code, body)
    }
}

object PromptStyles {
    fun imageStyle(styleId: String): String = when (styleId) {
        "realistic" -> "photorealistic, ultra detailed, natural lighting"
        "cartoon" -> "cartoon style, vibrant colors, clean lines"
        "anime" -> "anime style, detailed illustration"
        "oil" -> "oil painting, textured brush strokes"
        "cyber" -> "cyberpunk, neon lights, futuristic"
        else -> ""
    }

    fun editStyle(style: String): String = when (style.lowercase()) {
        "enhance" -> "enhanced details, sharper, higher quality"
        "realistic" -> "photorealistic finish"
        "moody" -> "moody cinematic lighting, dramatic atmosphere"
        else -> ""
    }

    fun musicGenre(genreId: String): String = when (genreId) {
        "lofi" -> "lo-fi genre"
        "pop" -> "pop genre"
        "hiphop" -> "hip-hop genre"
        "classical" -> "classical genre"
        "jazz" -> "jazz genre"
        "rock" -> "rock genre"
        "rap" -> "hip hop rap genre"
        "edm" -> "edm electronic dance genre"
        else -> ""
    }

    fun musicMood(moodId: String): String = when (moodId) {
        "happy" -> "happy upbeat mood"
        "sad" -> "sad melancholic mood"
        "energetic" -> "energetic dynamic mood"
        "relaxed" -> "relaxed calm mood"
        "intense" -> "intense powerful mood"
        "dreamy" -> "dreamy atmospheric mood"
        else -> ""
    }

    fun musicVoice(voiceId: String): Pair<String, Boolean> = when (voiceId) {
        "male" -> "male vocals" to false
        "female" -> "female vocals" to false
        "choir" -> "choir vocals" to false
        else -> "instrumental" to true
    }

    fun musicVocal(vocalId: String): Pair<String, Boolean> = musicVoice(vocalId)
}
