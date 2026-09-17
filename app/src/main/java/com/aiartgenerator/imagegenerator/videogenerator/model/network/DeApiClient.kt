package com.aiartgenerator.imagegenerator.videogenerator.model.network

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal object DeApiClient {
    private const val TAG = "DeApiClient"
    private const val POLL_INTERVAL_MS = 4_000L
    private const val VIDEO_POLL_TIMEOUT_MS = 10 * 60 * 1000L
    private const val IMAGE_POLL_TIMEOUT_MS = 3 * 60 * 1000L
    private const val MUSIC_POLL_TIMEOUT_MS = 5 * 60 * 1000L
    private const val RATE_LIMIT_MAX_RETRIES = 3
    private const val RATE_LIMIT_BASE_DELAY_MS = 20_000L

    suspend fun submitJson(
        client: OkHttpClient,
        endpointUrl: String,
        payload: JSONObject,
    ): String {
        var lastError: IOException? = null
        repeat(RATE_LIMIT_MAX_RETRIES) { attempt ->
            try {
                return submitJsonOnce(client, endpointUrl, payload)
            } catch (e: IOException) {
                lastError = e
                val canRetry = attempt < RATE_LIMIT_MAX_RETRIES - 1 &&
                    ApiErrorParser.isRateLimitedMessage(e.message)
                if (!canRetry) throw e
                val waitMs = RATE_LIMIT_BASE_DELAY_MS * (attempt + 1)
                Log.w(
                    TAG,
                    "deAPI rate limited on submit; retry ${attempt + 2}/$RATE_LIMIT_MAX_RETRIES in ${waitMs / 1000}s",
                )
                delay(waitMs)
            }
        }
        throw lastError ?: IOException("deAPI request failed")
    }

    private fun submitJsonOnce(
        client: OkHttpClient,
        endpointUrl: String,
        payload: JSONObject,
    ): String {
        val body = payload.toString()
        ApiLogger.logRequest(TAG, ApiProvider.DEAPI, "POST", endpointUrl, body)

        val request = Request.Builder()
            .url(endpointUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val contentType = response.header("Content-Type")
            if (!response.isSuccessful) {
                ApiLogger.logError(TAG, ApiProvider.DEAPI, endpointUrl, response.code, raw)
                throw IOException(parseError(response.code, raw))
            }
            ApiLogger.logTextResponse(TAG, ApiProvider.DEAPI, endpointUrl, response.code, contentType, raw)
            val requestId = extractRequestId(raw)
            Log.d(TAG, "Job submitted: $requestId")
            return requestId
        }
    }

    suspend fun submitMultipart(
        client: OkHttpClient,
        endpointUrl: String,
        body: MultipartBody,
    ): String {
        var lastError: IOException? = null
        repeat(RATE_LIMIT_MAX_RETRIES) { attempt ->
            try {
                return submitMultipartOnce(client, endpointUrl, body)
            } catch (e: IOException) {
                lastError = e
                val canRetry = attempt < RATE_LIMIT_MAX_RETRIES - 1 &&
                    ApiErrorParser.isRateLimitedMessage(e.message)
                if (!canRetry) throw e
                val waitMs = RATE_LIMIT_BASE_DELAY_MS * (attempt + 1)
                Log.w(
                    TAG,
                    "deAPI rate limited on multipart submit; retry ${attempt + 2}/$RATE_LIMIT_MAX_RETRIES in ${waitMs / 1000}s",
                )
                delay(waitMs)
            }
        }
        throw lastError ?: IOException("deAPI request failed")
    }

    private fun submitMultipartOnce(
        client: OkHttpClient,
        endpointUrl: String,
        body: MultipartBody,
    ): String {
        ApiLogger.logRequest(TAG, ApiProvider.DEAPI, "POST", endpointUrl, requestBody = "(multipart)")

        val request = Request.Builder()
            .url(endpointUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val contentType = response.header("Content-Type")
            if (!response.isSuccessful) {
                ApiLogger.logError(TAG, ApiProvider.DEAPI, endpointUrl, response.code, raw)
                throw IOException(parseError(response.code, raw))
            }
            ApiLogger.logTextResponse(TAG, ApiProvider.DEAPI, endpointUrl, response.code, contentType, raw)
            val requestId = extractRequestId(raw)
            Log.d(TAG, "Job submitted: $requestId")
            return requestId
        }
    }

    suspend fun pollJobResult(
        client: OkHttpClient,
        requestId: String,
        timeoutMs: Long,
    ): String {
        val startedAt = System.currentTimeMillis()
        val pollUrl = ApiRemoteConfig.deapiJobUrl(requestId)
        while (System.currentTimeMillis() - startedAt < timeoutMs) {
            ApiLogger.logRequest(TAG, ApiProvider.DEAPI, "GET", pollUrl)
            val request = Request.Builder()
                .url(pollUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val contentType = response.header("Content-Type")
                if (!response.isSuccessful) {
                    ApiLogger.logError(TAG, ApiProvider.DEAPI, pollUrl, response.code, raw)
                    if (ApiErrorParser.isRateLimited(response.code, raw)) {
                        Log.w(TAG, "deAPI rate limited while polling job $requestId; waiting 20s")
                        delay(RATE_LIMIT_BASE_DELAY_MS)
                        continue
                    }
                    throw IOException(parseError(response.code, raw))
                }
                ApiLogger.logTextResponse(TAG, ApiProvider.DEAPI, pollUrl, response.code, contentType, raw)

                val data = JSONObject(raw).optJSONObject("data")
                    ?: throw IOException("Invalid deAPI job response")
                val status = data.optString("status")
                val progress = data.optDouble("progress", 0.0)
                Log.d(TAG, "Job $requestId status=$status progress=$progress")

                when (status) {
                    "done" -> {
                        val resultUrl = data.optString("result_url")
                        if (resultUrl.isBlank()) {
                            throw IOException("deAPI job finished without a result URL")
                        }
                        Log.d(TAG, "Job $requestId result_url=$resultUrl")
                        return resultUrl
                    }
                    "error", "failed" -> {
                        val message = data.optString("message")
                            .ifBlank { data.optString("error") }
                            .ifBlank { raw }
                            .ifBlank { "deAPI video job failed" }
                        throw IOException(message)
                    }
                }
            }
            delay(POLL_INTERVAL_MS)
        }
        throw IOException("deAPI job timed out after ${timeoutMs / 60_000} minutes")
    }

    suspend fun runJsonJob(
        client: OkHttpClient,
        endpointUrl: String,
        payload: JSONObject,
        timeoutMs: Long = IMAGE_POLL_TIMEOUT_MS,
    ): ByteArray {
        val requestId = submitJson(client, endpointUrl, payload)
        val resultUrl = pollJobResult(client, requestId, timeoutMs)
        return downloadBytes(client, resultUrl)
    }

    suspend fun runMultipartJob(
        client: OkHttpClient,
        endpointUrl: String,
        body: MultipartBody,
        timeoutMs: Long = IMAGE_POLL_TIMEOUT_MS,
    ): ByteArray {
        val requestId = submitMultipart(client, endpointUrl, body)
        val resultUrl = pollJobResult(client, requestId, timeoutMs)
        return downloadBytes(client, resultUrl)
    }

    fun downloadBytes(client: OkHttpClient, url: String): ByteArray {
        ApiLogger.logRequest(TAG, ApiProvider.DEAPI, "GET", url)
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type")
            val bytes = response.body?.bytes()
            if (!response.isSuccessful || bytes == null) {
                val message = bytes?.toString(Charsets.UTF_8).orEmpty()
                ApiLogger.logError(TAG, ApiProvider.DEAPI, url, response.code, message)
                throw IOException(parseError(response.code, message))
            }
            ApiLogger.logBinaryResponse(TAG, ApiProvider.DEAPI, url, response.code, contentType, bytes.size)
            return bytes
        }
    }

    private const val VIDEO_MIN_SIDE = 512

    data class VideoProfile(
        val maxSide: Int,
        val fps: Int,
        val maxFrames: Int,
        val steps: Int,
    )

    fun videoProfile(model: String): VideoProfile = when {
        // Distilled LTXV — deAPI enforces frames <= 241. At 24 fps, max duration is 10s (240 frames).
        model.contains("Ltxv", ignoreCase = true) ||
            (model.contains("13B", ignoreCase = true) && !model.contains("Ltx", ignoreCase = true)) ->
            VideoProfile(maxSide = 768, fps = 24, maxFrames = 241, steps = 1)
        // LTX2 / LTX2.5 family (e.g. Ltx2_5_22B_Dist_INT8) — deAPI enforces frames <= 241. At 24 fps, max duration is 10s (240 frames).
        model.contains("Ltx2", ignoreCase = true) || model.contains("Ltx", ignoreCase = true) ->
            VideoProfile(maxSide = 1024, fps = 24, maxFrames = 241, steps = 8)
        else ->
            VideoProfile(maxSide = 1024, fps = 24, maxFrames = 241, steps = 8)
    }

    fun videoDimensions(aspectRatio: String, model: String): Pair<Int, Int> {
        val profile = videoProfile(model)
        val (aspectW, aspectH) = when (aspectRatio) {
            "9:16" -> 9 to 16
            "1:1" -> 1 to 1
            "4:3" -> 4 to 3
            else -> 16 to 9
        }
        return scaleToVideoConstraints(aspectW, aspectH, profile.maxSide)
    }

    private fun scaleToVideoConstraints(aspectW: Int, aspectH: Int, maxSide: Int): Pair<Int, Int> {
        var width: Int
        var height: Int
        if (aspectW >= aspectH) {
            height = VIDEO_MIN_SIDE
            width = (height * (aspectW.toFloat() / aspectH)).toInt()
        } else {
            width = VIDEO_MIN_SIDE
            height = (width * (aspectH.toFloat() / aspectW)).toInt()
        }
        if (width > maxSide || height > maxSide) {
            val scale = minOf(
                maxSide.toFloat() / width,
                maxSide.toFloat() / height,
            )
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }
        width = makeEven(width.coerceAtLeast(VIDEO_MIN_SIDE).coerceAtMost(maxSide))
        height = makeEven(height.coerceAtLeast(VIDEO_MIN_SIDE).coerceAtMost(maxSide))
        return width to height
    }

    private fun makeEven(value: Int): Int =
        if (value % 2 == 0) value else value + 1

    fun videoFps(model: String): Int = videoProfile(model).fps

    fun videoSteps(model: String): Int = videoProfile(model).steps

    fun videoFrames(durationSeconds: Int, model: String): Int {
        val profile = videoProfile(model)
        val maxSupportedSeconds = (profile.maxFrames / profile.fps).coerceAtLeast(2)
        val seconds = durationSeconds.coerceIn(2, maxSupportedSeconds)
        return (seconds * profile.fps).coerceIn(24, profile.maxFrames)
    }

    /** Longest duration (seconds) this model profile can request. */
    fun maxVideoDurationSeconds(model: String): Int {
        val profile = videoProfile(model)
        return (profile.maxFrames / profile.fps).coerceAtLeast(2)
    }

    val videoPollTimeoutMs: Long = VIDEO_POLL_TIMEOUT_MS
    val imagePollTimeoutMs: Long = IMAGE_POLL_TIMEOUT_MS
    val musicPollTimeoutMs: Long = MUSIC_POLL_TIMEOUT_MS
    val speechPollTimeoutMs: Long = MUSIC_POLL_TIMEOUT_MS

    fun musicDurationSeconds(seconds: Int): Int = seconds.coerceIn(10, 300)

    private fun extractRequestId(raw: String): String {
        val requestId = JSONObject(raw)
            .optJSONObject("data")
            ?.optString("request_id")
            .orEmpty()
        if (requestId.isBlank()) {
            throw IOException("deAPI did not return a request_id")
        }
        return requestId
    }

    private fun parseError(code: Int, body: String): String =
        ApiErrorParser.userMessage(ApiProvider.DEAPI, code, body)
}
