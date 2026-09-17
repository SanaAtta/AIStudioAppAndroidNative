package com.aiartgenerator.imagegenerator.videogenerator.model.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

object AiHttpClient {
    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .addInterceptor(ProviderAuthInterceptor())
            .build()
    }
}

private class ProviderAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val builder = request.newBuilder()

        when {
            host.contains("groq.com") -> {
                AiConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
                    builder.header("Authorization", "Bearer $key")
                }
            }
            host.contains("x.ai") -> {
                AiConfig.GROK_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
                    builder.header("Authorization", "Bearer $key")
                }
            }
            host.contains("deapi.ai") -> {
                AiConfig.DEAPI_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
                    builder.header("Authorization", "Bearer $key")
                }
                builder.header("Accept", "application/json")
            }
            host.contains("pollinations.ai") -> {
                AiConfig.POLLINATIONS_API_KEY.takeIf { it.isNotBlank() }?.let { key ->
                    builder.header("Authorization", "Bearer $key")
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
