package com.aiartgenerator.imagegenerator.videogenerator

import com.aiartgenerator.imagegenerator.videogenerator.model.VideoCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiErrorParser
import com.aiartgenerator.imagegenerator.videogenerator.model.network.DeApiClient
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeApiVideoConstraintsTest {

    @Test
    fun testLtx2_5ModelConfiguration() {
        assertEquals("Ltx2_5_22B_Dist_INT8", AiConfig.DEAPI_VIDEO_MODEL_LTX2_5)
        assertEquals("Ltx2_5_22B_Dist_INT8", AiConfig.DEAPI_VIDEO_MODEL)
    }

    @Test
    fun testLtx2_5FrameCalculationAndConstraints() {
        val model = AiConfig.DEAPI_VIDEO_MODEL_LTX2_5
        val profile = DeApiClient.videoProfile(model)
        val fps = DeApiClient.videoFps(model)
        val steps = DeApiClient.videoSteps(model)
        val maxDuration = DeApiClient.maxVideoDurationSeconds(model)

        assertEquals(24, fps)
        assertEquals(8, steps)
        assertEquals(10, maxDuration)
        assertEquals(241, profile.maxFrames)
        assertEquals(1024, profile.maxSide)

        // Test 3s, 5s, 10s calculations
        val frames3s = DeApiClient.videoFrames(3, model)
        assertEquals(72, frames3s)
        assertTrue("frames3s must be <= 241", frames3s <= 241)

        val frames5s = DeApiClient.videoFrames(5, model)
        assertEquals(120, frames5s)
        assertTrue("frames5s must be <= 241", frames5s <= 241)

        val frames10s = DeApiClient.videoFrames(10, model)
        assertEquals(240, frames10s)
        assertTrue("frames10s must be <= 241", frames10s <= 241)

        // Oversized requests (e.g. 15s, 30s) must be clamped and never exceed 241
        val frames15s = DeApiClient.videoFrames(15, model)
        assertEquals(240, frames15s)
        assertTrue("frames15s must be <= 241", frames15s <= 241)

        val frames30s = DeApiClient.videoFrames(30, model)
        assertEquals(240, frames30s)
        assertTrue("frames30s must be <= 241", frames30s <= 241)
    }

    @Test
    fun testVideoCatalogDurationsAreWithinConstraints() {
        val maxSupportedSeconds = DeApiClient.maxVideoDurationSeconds(AiConfig.DEAPI_VIDEO_MODEL)
        for (duration in VideoCatalog.durations) {
            assertTrue(
                "Duration ${duration.seconds}s exceeds max supported duration of ${maxSupportedSeconds}s",
                duration.seconds <= maxSupportedSeconds,
            )
            val frames = DeApiClient.videoFrames(duration.seconds, AiConfig.DEAPI_VIDEO_MODEL)
            assertTrue("Calculated frames ($frames) exceeds deAPI 241 limit", frames <= 241)
        }
    }

    @Test
    fun testFrameValidationErrorDoesNotTriggerKeyRotation() {
        val errorMessage = "The frames field must not be greater than 241."
        val isFailover = ApiErrorParser.isKeyFailoverMessage(errorMessage)
        assertFalse("Validation error should not trigger API key rotation", isFailover)
    }

    @Test
    fun testDeApiVideoPayloadStructure() {
        val model = AiConfig.DEAPI_VIDEO_MODEL
        val prompt = "rainy weather"
        val (width, height) = DeApiClient.videoDimensions("1:1", model)
        val fps = DeApiClient.videoFps(model)
        val steps = DeApiClient.videoSteps(model)
        val frames = DeApiClient.videoFrames(5, model)

        val jsonString = JsonObject().apply {
            addProperty("prompt", prompt)
            addProperty("model", model)
            addProperty("width", width)
            addProperty("height", height)
            addProperty("fps", fps)
            addProperty("steps", steps)
            addProperty("frames", frames)
            addProperty("seed", -1)
        }.toString()

        val parsed = JsonParser.parseString(jsonString).asJsonObject

        assertEquals("Ltx2_5_22B_Dist_INT8", parsed.get("model").asString)
        assertEquals("rainy weather", parsed.get("prompt").asString)
        assertEquals(24, parsed.get("fps").asInt)
        assertEquals(8, parsed.get("steps").asInt)
        assertEquals(120, parsed.get("frames").asInt)
        assertEquals(-1, parsed.get("seed").asInt)
    }
}
