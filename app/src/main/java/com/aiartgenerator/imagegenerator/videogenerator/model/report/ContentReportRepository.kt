package com.aiartgenerator.imagegenerator.videogenerator.model.report

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.aiartgenerator.imagegenerator.videogenerator.analytics.AppAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ContentReportRepository {
    private const val TAG = "ContentReportRepository"
    const val RECIPIENT_EMAIL = "techlazaapps@gmail.com"
    private const val REPORT_ENDPOINT = "https://formsubmit.co/ajax/$RECIPIENT_EMAIL"
    private const val DUPLICATE_COOLDOWN_MS = 10_000L

    private val _reportedContentIds = mutableStateOf<Set<String>>(emptySet())
    val reportedContentIds: State<Set<String>> get() = _reportedContentIds

    // In-flight and recent submission tracking to prevent duplicate rapid submissions
    private val inFlightSubmissions = ConcurrentHashMap<String, Long>()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Submits a report programmatically in the background without opening an external email app.
     * Delivered to techlazaapps@gmail.com.
     */
    suspend fun submitReport(
        context: Context,
        report: ContentReport,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val submissionKey = "${report.contentType.wireName}_${report.contentId}_${report.reason.key}"
        val now = System.currentTimeMillis()

        val lastSubmittedAt = inFlightSubmissions[submissionKey]
        if (lastSubmittedAt != null && (now - lastSubmittedAt) < DUPLICATE_COOLDOWN_MS) {
            return@withContext Result.failure(
                IllegalStateException("A report for this content is already being processed. Please wait."),
            )
        }

        inFlightSubmissions[submissionKey] = now

        try {
            // Log analytics
            AppAnalytics.event(
                name = "report_content_submitted",
                params = mapOf(
                    "content_type" to report.contentType.wireName,
                    "reason" to report.reason.key,
                    "content_id" to report.contentId,
                ),
            )

            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(report.timestamp))

            val payloadMap = linkedMapOf(
                "_subject" to "[AI Content Report] ${report.contentType.name} - ${report.reasonText}",
                "_template" to "table",
                "_captcha" to "false",
                "Recipient Email" to RECIPIENT_EMAIL,
                "Report ID" to report.id,
                "Content Type" to report.contentType.name,
                "Content ID" to report.contentId,
                "Report Reason" to report.reasonText,
                "Additional Details" to (report.additionalInfo ?: "None provided"),
                "Prompt" to (report.prompt ?: "N/A"),
                "Media URI" to (report.mediaUri ?: "N/A"),
                "Timestamp" to formattedDate,
                "App Version" to "${report.appVersion} (${report.appVersionCode})",
                "Device Model" to report.deviceModel,
                "OS Version" to report.osVersion,
            )
            val jsonString = com.google.gson.Gson().toJson(payloadMap)

            val request = Request.Builder()
                .url(REPORT_ENDPOINT)
                .post(jsonString.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "AIArtGenerator/${report.appVersion} (Android)")
                .header("Origin", "https://aiartgenerator.app")
                .header("Referer", "https://aiartgenerator.app/report")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException("Report submission failed with status code ${response.code}: $responseBody")
            }

            // Track reported ID in memory for session without database
            _reportedContentIds.value = _reportedContentIds.value + report.contentId

            runCatching {
                Log.i(
                    TAG,
                    "Report sent successfully to $RECIPIENT_EMAIL for [${report.contentType.wireName}] id=${report.contentId}",
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { Log.e(TAG, "Error submitting report: ${e.message}", e) }
            inFlightSubmissions.remove(submissionKey)
            Result.failure(e)
        }
    }

    fun isReported(contentId: String): Boolean {
        if (contentId.isBlank()) return false
        return _reportedContentIds.value.contains(contentId)
    }

    fun clearForTesting() {
        _reportedContentIds.value = emptySet()
        inFlightSubmissions.clear()
    }
}
