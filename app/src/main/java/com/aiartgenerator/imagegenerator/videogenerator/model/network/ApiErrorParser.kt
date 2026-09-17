package com.aiartgenerator.imagegenerator.videogenerator.model.network

import org.json.JSONObject

object ApiProvider {
    const val POLLINATIONS = "Pollinations"
    const val DEAPI = "deAPI"
    const val GROQ = "Groq"
    /** xAI Grok Imagine (api.x.ai) — image/video fallback, not Groq chat. */
    const val GROK = "Grok"

    fun label(name: String): String = when (name) {
        POLLINATIONS -> "Pollinations (gen.pollinations.ai)"
        DEAPI -> "deAPI (api.deapi.ai)"
        GROQ -> "Groq (api.groq.com)"
        GROK -> "Grok (api.x.ai)"
        else -> name
    }
}

object ApiErrorParser {
    fun parse(body: String): String {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return "Unknown API error"
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed.take(220)
        }
        return runCatching {
            val root = JSONObject(trimmed)
            root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: root.optJSONObject("error")?.optString("detail")?.takeIf { it.isNotBlank() }
                ?: root.optJSONObject("error")?.optString("error")?.takeIf { it.isNotBlank() }
                ?: root.optString("message").takeIf { it.isNotBlank() }
                ?: root.optString("detail").takeIf { it.isNotBlank() }
                ?: root.optString("error_description").takeIf { it.isNotBlank() }
                ?: root.optString("error").takeIf { it.isNotBlank() && !it.startsWith("{") }
                ?: root.optJSONObject("data")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: trimmed.take(220)
        }.getOrDefault(trimmed.take(220))
    }

    fun isRateLimited(httpCode: Int, body: String): Boolean {
        val message = parse(body)
        return httpCode == 429 ||
            message.contains("Too Many Attempts", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true)
    }

    fun isRateLimitedMessage(message: String?): Boolean {
        val text = message.orEmpty()
        return text.contains("Too Many Attempts", ignoreCase = true) ||
            text.contains("too many requests", ignoreCase = true) ||
            text.contains("rate limit", ignoreCase = true)
    }

    fun isCreditExhausted(httpCode: Int, body: String): Boolean {
        if (httpCode == 402) return true
        val parsed = parse(body)
        return isCreditExhaustedMessage(body) || isCreditExhaustedMessage(parsed)
    }

    fun isCreditExhaustedMessage(message: String?): Boolean {
        val text = message.orEmpty()
        if (text.isBlank()) return false
        return text.contains("insufficient pollen", ignoreCase = true) ||
            text.contains("insufficient credit", ignoreCase = true) ||
            text.contains("insufficient credits", ignoreCase = true) ||
            text.contains("insufficient balance", ignoreCase = true) ||
            text.contains("insufficient funds", ignoreCase = true) ||
            text.contains("insufficient quota", ignoreCase = true) ||
            text.contains("insufficient_quota", ignoreCase = true) ||
            text.contains("insufficient_pollen", ignoreCase = true) ||
            text.contains("insufficient_credits", ignoreCase = true) ||
            text.contains("insufficient_balance", ignoreCase = true) ||
            text.contains("insufficient", ignoreCase = true) ||
            text.contains("low credit", ignoreCase = true) ||
            text.contains("low credits", ignoreCase = true) ||
            text.contains("low pollen", ignoreCase = true) ||
            text.contains("low balance", ignoreCase = true) ||
            text.contains("balance too low", ignoreCase = true) ||
            text.contains("zero balance", ignoreCase = true) ||
            text.contains("0 pollen", ignoreCase = true) ||
            text.contains("0 credits", ignoreCase = true) ||
            text.contains("no credit", ignoreCase = true) ||
            text.contains("no credits", ignoreCase = true) ||
            text.contains("no pollen", ignoreCase = true) ||
            text.contains("not enough credit", ignoreCase = true) ||
            text.contains("not enough credits", ignoreCase = true) ||
            text.contains("not enough pollen", ignoreCase = true) ||
            text.contains("not enough balance", ignoreCase = true) ||
            text.contains("have enough pollen", ignoreCase = true) ||
            text.contains("have enough credit", ignoreCase = true) ||
            text.contains("have enough credits", ignoreCase = true) ||
            text.contains("have enough balance", ignoreCase = true) ||
            text.contains("do not have enough", ignoreCase = true) ||
            text.contains("don't have enough", ignoreCase = true) ||
            text.contains("out of credit", ignoreCase = true) ||
            text.contains("out of credits", ignoreCase = true) ||
            text.contains("out of pollen", ignoreCase = true) ||
            text.contains("out of balance", ignoreCase = true) ||
            text.contains("exhausted credit", ignoreCase = true) ||
            text.contains("exhausted credits", ignoreCase = true) ||
            text.contains("exhausted pollen", ignoreCase = true) ||
            text.contains("credits exhausted", ignoreCase = true) ||
            text.contains("credit exhausted", ignoreCase = true) ||
            text.contains("quota exceeded", ignoreCase = true) ||
            text.contains("exceeded quota", ignoreCase = true) ||
            text.contains("exceeded your quota", ignoreCase = true) ||
            text.contains("exceeded your current quota", ignoreCase = true) ||
            text.contains("quota_exceeded", ignoreCase = true) ||
            text.contains("credit limit", ignoreCase = true) ||
            text.contains("PAYMENT_REQUIRED", ignoreCase = true) ||
            text.contains("payment required", ignoreCase = true) ||
            text.contains("payment needed", ignoreCase = true) ||
            text.contains("enter.pollinations.ai", ignoreCase = true) ||
            text.contains("top up", ignoreCase = true) ||
            text.contains("tier limit", ignoreCase = true) ||
            text.contains("upgrade plan", ignoreCase = true) ||
            text.contains("upgrade your plan", ignoreCase = true) ||
            text.contains("upgrade your account", ignoreCase = true)
    }

    /** Credit exhaustion or bad/expired key — safe to try the next RC fallback key. */
    fun isKeyFailover(httpCode: Int, body: String): Boolean {
        if (httpCode == 401 || httpCode == 402 || httpCode == 403) return true
        if (isCreditExhausted(httpCode, body)) return true
        val parsed = parse(body)
        return isKeyAuthErrorMessage(body) || isKeyAuthErrorMessage(parsed)
    }

    private fun isKeyAuthErrorMessage(text: String): Boolean {
        if (text.isBlank()) return false
        return text.contains("unauthorized", ignoreCase = true) ||
            text.contains("invalid api key", ignoreCase = true) ||
            text.contains("invalid key", ignoreCase = true) ||
            text.contains("invalid_api_key", ignoreCase = true) ||
            text.contains("invalid api-key", ignoreCase = true) ||
            text.contains("api key is invalid", ignoreCase = true) ||
            text.contains("api key not valid", ignoreCase = true) ||
            text.contains("bad api key", ignoreCase = true) ||
            text.contains("bad key", ignoreCase = true) ||
            text.contains("authentication", ignoreCase = true) ||
            text.contains("access denied", ignoreCase = true) ||
            text.contains("forbidden", ignoreCase = true) ||
            text.contains("expired key", ignoreCase = true) ||
            text.contains("key expired", ignoreCase = true) ||
            text.contains("unauthenticated", ignoreCase = true)
    }

    fun isKeyFailoverMessage(message: String?): Boolean {
        val text = message.orEmpty()
        if (text.isBlank()) return false
        return isCreditExhaustedMessage(text) ||
            isKeyAuthErrorMessage(text) ||
            text.contains("(401)", ignoreCase = true) ||
            text.contains("(402)", ignoreCase = true) ||
            text.contains("(403)", ignoreCase = true)
    }

    fun isExplicitErrorBody(body: String): Boolean {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return false
        if (isKeyFailover(200, trimmed)) return true
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return runCatching {
                val json = JSONObject(trimmed)
                if (json.has("error") && !json.isNull("error")) {
                    val err = json.opt("error")
                    if (err is JSONObject) return@runCatching true
                    if (err is String && err.isNotBlank() && err != "null") return@runCatching true
                    if (err is Boolean && err) return@runCatching true
                }
                val status = json.optString("status")
                if (status.equals("error", ignoreCase = true) || status.equals("failed", ignoreCase = true)) {
                    return@runCatching true
                }
                if (json.has("success") && !json.optBoolean("success", true)) {
                    return@runCatching true
                }
                val code = json.optString("code")
                if (code.contains("error", ignoreCase = true) ||
                    code.contains("insufficient", ignoreCase = true) ||
                    code.contains("quota", ignoreCase = true)
                ) {
                    return@runCatching true
                }
                val detail = json.optString("detail")
                if (detail.isNotBlank() && (isKeyFailoverMessage(detail) || detail.contains("error", ignoreCase = true))) {
                    return@runCatching true
                }
                false
            }.getOrDefault(false)
        }
        return isKeyFailoverMessage(trimmed)
    }

    fun userMessage(provider: String, httpCode: Int, body: String): String {
        val message = parse(body)
        val providerLabel = ApiProvider.label(provider)
        val isPaymentRequired = isCreditExhausted(httpCode, body)

        return when {
            isRateLimited(httpCode, body) ->
                "$providerLabel — Too many requests. Wait 1–2 minutes and try again. " +
                    "Free plans have strict rate limits — upgrade at ${AiConfig.DEAPI_KEYS_URL}"
            isPaymentRequired && provider == ApiProvider.POLLINATIONS ->
                "$providerLabel — Insufficient pollen credits. Add credits at ${AiConfig.POLLINATIONS_KEYS_URL}"
            isPaymentRequired && provider == ApiProvider.DEAPI ->
                "$providerLabel — Insufficient credits. Add credits at ${AiConfig.DEAPI_KEYS_URL}"
            isPaymentRequired && provider == ApiProvider.GROQ ->
                "$providerLabel — Insufficient credits or quota. Check your Groq key."
            isPaymentRequired && provider == ApiProvider.GROK ->
                "$providerLabel — Insufficient credits or quota. Check your Grok (xAI) key."
            message.contains("not available on your plan", ignoreCase = true) ||
                message.contains("upgrade to access", ignoreCase = true) ->
                "$providerLabel — This model is not available on your plan. Upgrade at ${AiConfig.DEAPI_KEYS_URL} to access premium models."
            provider.isNotBlank() -> "$providerLabel — $message"
            else -> message
        }
    }
}
