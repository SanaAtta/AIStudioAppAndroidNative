package com.example.myapplication.network

import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AiApi(
    private val client: io.ktor.client.HttpClient = AiHttpClient.http,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateImage(
        prompt: String,
        styleHint: String = "",
        width: Int = 1024,
        height: Int = 1024,
        model: String = AiConfig.IMAGE_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val fullPrompt = buildPrompt(prompt, styleHint)
            downloadBytes(
                buildUrl(
                    path = "/image/${encode(fullPrompt)}",
                    query = mapOf(
                        "model" to model,
                        "width" to width.toString(),
                        "height" to height.toString(),
                        "nologo" to "true",
                    ),
                ),
            )
        }
    }

    suspend fun editImage(
        imageBytes: ByteArray,
        fileName: String = "edit_input.jpg",
        prompt: String,
        styleHint: String = "",
        model: String = AiConfig.EDIT_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val response = client.post("${AiConfig.BASE_URL}/v1/images/edits") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "image",
                                imageBytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/*")
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"$fileName\"",
                                    )
                                },
                            )
                            append("prompt", buildPrompt(prompt, styleHint))
                            append("model", model)
                            append("response_format", "b64_json")
                        },
                    ),
                )
            }
            val raw = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error(errorMessage(response.status.value, raw))
            }
            decodeImagePayload(raw)
        }
    }

    suspend fun generateVideo(
        prompt: String,
        styleHint: String = "",
        durationSeconds: Int = 5,
        width: Int = 1280,
        height: Int = 720,
        model: String = AiConfig.VIDEO_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val fullPrompt = buildPrompt(prompt, styleHint)
            downloadBytes(
                buildUrl(
                    path = "/video/${encode(fullPrompt)}",
                    query = mapOf(
                        "model" to model,
                        "duration" to durationSeconds.coerceIn(2, 15).toString(),
                        "width" to width.toString(),
                        "height" to height.toString(),
                    ),
                ),
            )
        }
    }

    suspend fun generateMusic(
        prompt: String,
        genreHint: String = "",
        instrumental: Boolean = true,
        durationSeconds: Int = 30,
        model: String = AiConfig.MUSIC_MODEL,
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val fullPrompt = buildPrompt(prompt, genreHint)
            downloadBytes(
                buildUrl(
                    path = "/audio/${encode(fullPrompt)}",
                    query = mapOf(
                        "model" to model,
                        "duration" to durationSeconds.coerceIn(3, 300).toString(),
                        "instrumental" to if (instrumental) "true" else "false",
                    ),
                ),
            )
        }
    }

    suspend fun generateCover(
        audioBytes: ByteArray,
        fileName: String = "cover_input.m4a",
        voiceTitle: String,
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val voicePart = if (voiceTitle.equals("No Voice", ignoreCase = true)) {
                "instrumental remake"
            } else {
                "sung in a $voiceTitle voice style"
            }
            val input = "Create an AI cover of this song, $voicePart, high quality"
            val response = client.post("${AiConfig.BASE_URL}/v1/audio/speech") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("model", "stable-audio-3-medium")
                            append("input", input)
                            append("seconds", "30")
                            append(
                                "reference_audio",
                                audioBytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "audio/*")
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"$fileName\"",
                                    )
                                },
                            )
                        },
                    ),
                )
            }
            val bytes = response.body<ByteArray>()
            if (!response.status.isSuccess()) {
                error(errorMessage(response.status.value, bytes.decodeToString()))
            }
            bytes
        }
    }

    suspend fun chat(
        messages: List<Pair<String, String>>,
        model: String = AiConfig.CHAT_MODEL,
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val payload = buildJsonObject {
                put("model", model)
                put(
                    "messages",
                    buildJsonArray {
                        messages.forEach { (role, content) ->
                            add(
                                buildJsonObject {
                                    put("role", role)
                                    put("content", content)
                                },
                            )
                        }
                    },
                )
            }
            val response = client.post("${AiConfig.BASE_URL}/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val raw = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error(errorMessage(response.status.value, raw))
            }
            val root = json.parseToJsonElement(raw).jsonObject
            val content = root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
            content?.takeIf { it.isNotBlank() } ?: error("Empty assistant message")
        }
    }

    fun imageUrl(
        prompt: String,
        width: Int = 1080,
        height: Int = 1920,
        model: String = AiConfig.IMAGE_MODEL,
    ): String = buildUrl(
        path = "/image/${encode(prompt)}",
        query = mapOf(
            "model" to model,
            "width" to width.toString(),
            "height" to height.toString(),
            "nologo" to "true",
        ),
    )

    suspend fun downloadUrl(url: String): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching { downloadBytes(url) }
    }

    private suspend fun downloadBytes(url: String): ByteArray {
        val response = client.get(url)
        val bytes = response.body<ByteArray>()
        if (!response.status.isSuccess()) {
            error(errorMessage(response.status.value, bytes.decodeToString()))
        }
        return bytes
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun decodeImagePayload(raw: String): ByteArray {
        val root = json.parseToJsonElement(raw).jsonObject
        val data = root["data"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: error("Invalid image edit response")
        val b64 = data["b64_json"]?.jsonPrimitive?.contentOrNull
        if (!b64.isNullOrBlank()) {
            return Base64.decode(b64)
        }
        val url = data["url"]?.jsonPrimitive?.contentOrNull
        if (!url.isNullOrBlank()) {
            return downloadBytes(url)
        }
        error("No image data in response")
    }

    private fun buildPrompt(prompt: String, hint: String): String {
        val clean = prompt.trim()
        val extra = hint.trim()
        return if (extra.isEmpty()) clean else "$clean, $extra"
    }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "${AiConfig.BASE_URL}$path?$queryString"
    }

    private fun encode(value: String): String = value.encodeURLParameter(spaceToPlus = false)

    private fun errorMessage(code: Int, body: String): String {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return "Request failed ($code)"
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element as? JsonObject ?: return@runCatching null
            when (val error = obj["error"]) {
                is JsonPrimitive -> error.contentOrNull
                is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
                else -> null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: trimmed.take(180)
    }
}
