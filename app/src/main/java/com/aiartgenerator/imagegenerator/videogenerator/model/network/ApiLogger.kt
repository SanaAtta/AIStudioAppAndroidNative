package com.aiartgenerator.imagegenerator.videogenerator.model.network

import android.util.Log

object ApiLogger {
    const val TAG = "AiApiResponse"
    private const val MAX_BODY_CHARS = 4000

    fun logProvider(tag: String, provider: String, feature: String) {
        Log.i(tag, "Using API: ${ApiProvider.label(provider)} → $feature")
    }

    fun logRequest(
        tag: String,
        provider: String,
        method: String,
        url: String,
        requestBody: String? = null,
    ) {
        Log.d(tag, "[${ApiProvider.label(provider)}] → $method ${sanitizeUrl(url)}")
        requestBody?.let { body ->
            Log.d(tag, "  request: ${truncate(body)}")
        }
    }

    fun logTextResponse(
        tag: String,
        provider: String,
        url: String,
        code: Int,
        contentType: String?,
        body: String,
    ) {
        Log.d(tag, "[${ApiProvider.label(provider)}] ← $code ${sanitizeUrl(url)} [${contentType.orEmpty()}]")
        Log.d(tag, "  response: ${truncate(body)}")
    }

    fun logBinaryResponse(
        tag: String,
        provider: String,
        url: String,
        code: Int,
        contentType: String?,
        byteCount: Int,
    ) {
        Log.d(
            tag,
            "[${ApiProvider.label(provider)}] ← $code ${sanitizeUrl(url)} " +
                "[${contentType.orEmpty()}] bytes=$byteCount",
        )
    }

    fun logError(tag: String, provider: String, url: String, code: Int, body: String) {
        Log.e(tag, "[${ApiProvider.label(provider)}] ← ERROR $code ${sanitizeUrl(url)}")
        Log.e(tag, "  response: ${truncate(body)}")
        Log.e(tag, "  userMessage: ${ApiErrorParser.userMessage(provider, code, body)}")
    }

    private fun sanitizeUrl(url: String): String =
        url.replace(Regex("([?&]key=)[^&]+", RegexOption.IGNORE_CASE), "$1***")

    private fun truncate(text: String): String =
        if (text.length <= MAX_BODY_CHARS) {
            text
        } else {
            text.take(MAX_BODY_CHARS) + "…(${text.length} chars total)"
        }
}
