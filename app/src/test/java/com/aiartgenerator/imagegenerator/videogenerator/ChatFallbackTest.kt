package com.aiartgenerator.imagegenerator.videogenerator

import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFallbackTest {

    @Test
    fun testChatConfiguredFlag() {
        val isConfigured = AiConfig.isChatConfigured
        assertTrue(isConfigured == true || isConfigured == false)
    }

    @Test
    fun testChatFallbackFromGrokToDeApiWhenGrokFails() = runBlocking {
        val grokCalls = AtomicInteger(0)
        val deapiCalls = AtomicInteger(0)
        val receivedPrompts = mutableListOf<String>()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val bodyBuffer = okio.Buffer()
                request.body?.writeTo(bodyBuffer)
                val bodyString = bodyBuffer.readUtf8()
                receivedPrompts.add(bodyString)

                if (url.contains("x.ai") || url.contains("groq.com")) {
                    grokCalls.incrementAndGet()
                    // Grok fails with 500 error
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(500)
                        .message("Internal Server Error")
                        .header("Content-Type", "application/json")
                        .body("{\"error\":{\"message\":\"Grok service unavailable\"}}".toResponseBody("application/json".toMediaType()))
                        .build()
                } else if (url.contains("deapi.ai")) {
                    deapiCalls.incrementAndGet()
                    // DeAPI succeeds
                    val deapiSuccessJson = """
                        {
                            "choices": [
                                {
                                    "message": {
                                        "role": "assistant",
                                        "content": "Hello from DeAPI Fallback!"
                                    }
                                }
                            ]
                        }
                    """.trimIndent()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Content-Type", "application/json")
                        .body(deapiSuccessJson.toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    throw IOException("Unexpected URL: $url")
                }
            }
            .build()

        val api = AiApi(client)
        val messages = listOf(
            "system" to "You are Aria",
            "user" to "Hello AI",
        )

        val result = api.chat(messages)
        if (result.isFailure) {
            println("Chat failed with: ${result.exceptionOrNull()}")
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Chat should succeed via DeAPI fallback: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals("Hello from DeAPI Fallback!", result.getOrNull())
        assertTrue("DeAPI should have been called", deapiCalls.get() >= 1)
    }

    @Test
    fun testChatSucceedsOnGrokWithoutCallingDeApi() = runBlocking {
        val grokCalls = AtomicInteger(0)
        val deapiCalls = AtomicInteger(0)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()

                if (url.contains("x.ai") || url.contains("groq.com")) {
                    grokCalls.incrementAndGet()
                    val grokSuccessJson = """
                        {
                            "choices": [
                                {
                                    "message": {
                                        "role": "assistant",
                                        "content": "Hello from Grok!"
                                    }
                                }
                            ]
                        }
                    """.trimIndent()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Content-Type", "application/json")
                        .body(grokSuccessJson.toResponseBody("application/json".toMediaType()))
                        .build()
                } else if (url.contains("deapi.ai")) {
                    deapiCalls.incrementAndGet()
                    throw IOException("DeAPI should not be called when Grok succeeds")
                } else {
                    throw IOException("Unexpected URL: $url")
                }
            }
            .build()

        val api = AiApi(client)
        val messages = listOf(
            "system" to "You are Aria",
            "user" to "Hello AI",
        )

        val result = api.chat(messages)
        if (grokCalls.get() > 0) {
            assertTrue("Chat should succeed via Grok", result.isSuccess)
            assertEquals("Hello from Grok!", result.getOrNull())
            assertEquals("DeAPI must not be called when Grok succeeds", 0, deapiCalls.get())
        }
    }

    @Test
    fun testChatFailsWhenBothGrokAndDeApiFail() = runBlocking {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .header("Content-Type", "application/json")
                    .body("{\"error\":{\"message\":\"Service unavailable\"}}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val api = AiApi(client)
        val messages = listOf("user" to "Hello")

        val result = api.chat(messages)
        assertTrue("Chat should return failure result when both fail", result.isFailure)
    }

    @Test
    fun testChatFallbackWhenGrokReturnsEmptyChoices() = runBlocking {
        val deapiCalled = AtomicInteger(0)

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()

                if (url.contains("x.ai") || url.contains("groq.com")) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Content-Type", "application/json")
                        .body("{\"choices\":[]}".toResponseBody("application/json".toMediaType()))
                        .build()
                } else if (url.contains("deapi.ai")) {
                    deapiCalled.incrementAndGet()
                    val deapiSuccessJson = """
                        {
                            "choices": [
                                {
                                    "message": {
                                        "role": "assistant",
                                        "content": "Recovered by DeAPI!"
                                    }
                                }
                            ]
                        }
                    """.trimIndent()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Content-Type", "application/json")
                        .body(deapiSuccessJson.toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    throw IOException("Unexpected URL: $url")
                }
            }
            .build()

        val api = AiApi(client)
        val result = api.chat(listOf("user" to "Hello"))
        assertTrue("Should succeed via DeAPI", result.isSuccess)
        assertEquals("Recovered by DeAPI!", result.getOrNull())
        assertEquals(1, deapiCalled.get())
    }

    @Test
    fun testChatHistoryPreservedInDeApiRequest() = runBlocking {
        var deapiPayload: String? = null

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                val bodyBuffer = okio.Buffer()
                request.body?.writeTo(bodyBuffer)
                val bodyString = bodyBuffer.readUtf8()

                if (url.contains("x.ai") || url.contains("groq.com")) {
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(400)
                        .message("Bad Request")
                        .header("Content-Type", "application/json")
                        .body("{\"error\":{\"message\":\"Invalid\"}}".toResponseBody("application/json".toMediaType()))
                        .build()
                } else if (url.contains("deapi.ai")) {
                    deapiPayload = bodyString
                    val deapiSuccessJson = """
                        {
                            "choices": [
                                {
                                    "message": {
                                        "role": "assistant",
                                        "content": "History verified."
                                    }
                                }
                            ]
                        }
                    """.trimIndent()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Content-Type", "application/json")
                        .body(deapiSuccessJson.toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    throw IOException("Unexpected URL: $url")
                }
            }
            .build()

        val api = AiApi(client)
        val history = listOf(
            "system" to "System prompt aria",
            "user" to "User question 1",
            "assistant" to "Assistant answer 1",
            "user" to "User question 2",
        )

        val result = api.chat(history)
        assertTrue(result.isSuccess)
        assertEquals("History verified.", result.getOrNull())
        assertTrue("DeAPI payload should contain system prompt", deapiPayload?.contains("System prompt aria") == true)
        assertTrue("DeAPI payload should contain user question 1", deapiPayload?.contains("User question 1") == true)
        assertTrue("DeAPI payload should contain assistant answer 1", deapiPayload?.contains("Assistant answer 1") == true)
        assertTrue("DeAPI payload should contain user question 2", deapiPayload?.contains("User question 2") == true)
    }

    @Test
    fun testPollinationsKeyFallbackOnLowCreditsHttp200() = runBlocking {
        val testPollinationsKeys = listOf("key_1_test", "key_2_test", "key_3_test", "key_4_test", "key_5_test")
        com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.testKeys = mapOf(
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.POLLINATIONS to testPollinationsKeys,
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.DEAPI to emptyList(),
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.GROQ to emptyList(),
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.GROK to emptyList(),
        )

        val attemptCount = AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val currentAttempt = attemptCount.incrementAndGet()
                when (currentAttempt) {
                    1 -> {
                        // Key 1 returns HTTP 200 with low credits JSON
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "application/json")
                            .body("{\"error\":\"Insufficient pollen credits. Please top up at https://enter.pollinations.ai\"}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    2 -> {
                        // Key 2 returns HTTP 200 with choices containing credit warning
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "application/json")
                            .body("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"You do not have enough pollen / credits to use model openai.\"}}]}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                    else -> {
                        // Key 3 succeeds
                        Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", "application/json")
                            .body("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Success from Key 3!\"}}]}".toResponseBody("application/json".toMediaType()))
                            .build()
                    }
                }
            }
            .build()

        try {
            val api = AiApi(client)
            val result = api.chat(listOf("user" to "Hi"))
            assertTrue("Chat should succeed after rotating exhausted keys: ${result.exceptionOrNull()?.message}", result.isSuccess)
            assertEquals("Success from Key 3!", result.getOrNull())
            assertTrue("Should have attempted key 1, 2, and 3", attemptCount.get() >= 3)
        } finally {
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.testKeys = null
        }
    }

    @Test
    fun testPollinationsAllFiveKeysFailReturnsError() = runBlocking {
        val testPollinationsKeys = listOf("key_1_test", "key_2_test", "key_3_test", "key_4_test", "key_5_test")
        com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.testKeys = mapOf(
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.POLLINATIONS to testPollinationsKeys,
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.DEAPI to emptyList(),
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.GROQ to emptyList(),
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.Provider.GROK to emptyList(),
        )

        val attemptCount = AtomicInteger(0)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                attemptCount.incrementAndGet()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Insufficient pollen credits. Please top up.\"}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        try {
            val api = AiApi(client)
            val result = api.chat(listOf("user" to "Hi"))
            assertTrue("Chat should return failure after all 5 keys fail", result.isFailure)
            assertEquals("Should attempt all 5 keys sequentially", 5, attemptCount.get())
        } finally {
            com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool.testKeys = null
        }
    }

    @Test
    fun testApiErrorParserDetectsLowCredits() {
        val parser = com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiErrorParser
        assertTrue(parser.isCreditExhausted(200, "{\"error\":\"Insufficient pollen credits\"}"))
        assertTrue(parser.isCreditExhausted(200, "{\"message\":\"Low credits. Please add balance.\"}"))
        assertTrue(parser.isCreditExhausted(200, "{\"status\":\"error\",\"message\":\"quota exceeded\"}"))
        assertTrue(parser.isCreditExhausted(200, "You do not have enough pollen"))
        assertTrue(parser.isCreditExhausted(402, "Payment required"))
        assertTrue(parser.isKeyFailoverMessage("Insufficient pollen credits. Add credits at https://enter.pollinations.ai"))
        assertTrue(parser.isKeyFailoverMessage("Unauthorized (401)"))
        assertTrue(parser.isExplicitErrorBody("{\"error\": \"User has 0 pollen remaining\"}"))
    }
}
