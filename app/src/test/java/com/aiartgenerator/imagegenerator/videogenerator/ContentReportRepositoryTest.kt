package com.aiartgenerator.imagegenerator.videogenerator

import android.content.Context
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ContentReport
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ContentReportRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportReason
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ContentReportRepositoryTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        ContentReportRepository.clearForTesting()
        tempDir = Files.createTempDirectory("report_test").toFile()

        // Configure mock HTTP client by default for test isolation
        ContentReportRepository.httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"success":"true","message":"Submitted"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    @Test
    fun recipientEmailIsConfiguredCorrectly() {
        assertEquals("techlazaapps@gmail.com", ContentReportRepository.RECIPIENT_EMAIL)
    }

    @Test
    fun reportReasonsContainAllRequiredGooglePlayCategories() {
        val expectedReasons = setOf(
            ReportReason.Offensive,
            ReportReason.Sexual,
            ReportReason.Hate,
            ReportReason.Violent,
            ReportReason.Other,
        )
        assertEquals(5, ReportReason.entries.size)
        assertEquals(expectedReasons, ReportReason.entries.toSet())
    }

    @Test
    fun reportContentTypesCoverImageVideoAndAudio() {
        val types = ReportContentType.entries.toSet()
        assertTrue(types.contains(ReportContentType.Image))
        assertTrue(types.contains(ReportContentType.Video))
        assertTrue(types.contains(ReportContentType.Audio))
    }

    @Test
    fun reportCreationSetsAppropriateDefaults() {
        val report = ContentReport(
            contentId = "img_12345",
            contentType = ReportContentType.Image,
            reason = ReportReason.Offensive,
            reasonText = "Offensive or abusive content",
            prompt = "A generated landscape",
            mediaUri = "content://media/1",
        )

        assertNotNull(report.id)
        assertTrue(report.id.isNotEmpty())
        assertEquals("img_12345", report.contentId)
        assertEquals(ReportContentType.Image, report.contentType)
        assertEquals(ReportReason.Offensive, report.reason)
        assertEquals("Offensive or abusive content", report.reasonText)
        assertEquals("A generated landscape", report.prompt)
        assertTrue(report.timestamp > 0)
    }

    @Test
    fun submitReportSendsExpectedPayloadAndPreventsDuplicate() = runBlocking {
        val dummyContext = createMockContext(tempDir)
        var capturedPayload: String? = null

        ContentReportRepository.httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                capturedPayload = buffer.readUtf8()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"success":"true"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val report = ContentReport(
            contentId = "video_999",
            contentType = ReportContentType.Video,
            reason = ReportReason.Violent,
            reasonText = "Violent or graphic content",
            prompt = "action scene",
            additionalInfo = "Explosive content",
        )

        val firstResult = ContentReportRepository.submitReport(dummyContext, report)
        assertTrue("First submission should succeed", firstResult.isSuccess)
        assertTrue(ContentReportRepository.isReported("video_999"))

        assertNotNull("Payload should be captured", capturedPayload)
        val json = com.google.gson.JsonParser.parseString(capturedPayload!!).asJsonObject
        assertEquals("techlazaapps@gmail.com", json.get("Recipient Email").asString)
        assertEquals("Video", json.get("Content Type").asString)
        assertEquals("video_999", json.get("Content ID").asString)
        assertEquals("Violent or graphic content", json.get("Report Reason").asString)
        assertEquals("Explosive content", json.get("Additional Details").asString)

        // Immediate duplicate submission with the same contentId & reason should be prevented
        val duplicateResult = ContentReportRepository.submitReport(dummyContext, report)
        assertTrue("Duplicate submission should fail", duplicateResult.isFailure)
        assertTrue(
            duplicateResult.exceptionOrNull()?.message?.contains("already being processed") == true,
        )
    }

    @Test
    fun submitReportHandlesServerErrorGracefully() = runBlocking {
        val dummyContext = createMockContext(tempDir)

        ContentReportRepository.httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Internal Server Error")
                    .body("""{"error":"Server down"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val report = ContentReport(
            contentId = "img_fail_test",
            contentType = ReportContentType.Image,
            reason = ReportReason.Other,
            reasonText = "Other",
        )

        val result = ContentReportRepository.submitReport(dummyContext, report)
        assertTrue("Submission with 500 status should return failure", result.isFailure)
        assertFalse(ContentReportRepository.isReported("img_fail_test"))
    }

    @Test
    fun allowsReportingDifferentReasonsOrDifferentContents() = runBlocking {
        val dummyContext = createMockContext(tempDir)

        val reportImage = ContentReport(
            contentId = "img_001",
            contentType = ReportContentType.Image,
            reason = ReportReason.Sexual,
            reasonText = "Sexual or inappropriate content",
        )

        val reportAudio = ContentReport(
            contentId = "audio_002",
            contentType = ReportContentType.Audio,
            reason = ReportReason.Hate,
            reasonText = "Hate or discriminatory content",
        )

        val imgResult = ContentReportRepository.submitReport(dummyContext, reportImage)
        val audioResult = ContentReportRepository.submitReport(dummyContext, reportAudio)

        assertTrue(imgResult.isSuccess)
        assertTrue(audioResult.isSuccess)
        assertTrue(ContentReportRepository.isReported("img_001"))
        assertTrue(ContentReportRepository.isReported("audio_002"))
        assertFalse(ContentReportRepository.isReported("non_existent"))
    }

    private fun createMockContext(filesDir: File): Context = object : android.content.ContextWrapper(null) {
        override fun getFilesDir(): File = filesDir
        override fun getPackageName(): String = "com.aiartgenerator.imagegenerator.videogenerator"
        override fun getApplicationContext(): Context = this
    }
}
