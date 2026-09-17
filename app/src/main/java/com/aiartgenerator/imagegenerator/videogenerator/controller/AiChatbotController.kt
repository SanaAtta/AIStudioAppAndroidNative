package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatMessage
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatRole
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatSuggestion
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import java.util.UUID

class AiChatbotController(
    private val api: AiApi = AiApi(),
) {
    var inputText by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var selectedSuggestionId by mutableStateOf<String?>(null)
        private set

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    val suggestions: List<ChatSuggestion> get() = ChatCatalog.suggestions
    val canSend: Boolean get() = inputText.isNotBlank() && !isLoading

    init {
        _messages.add(
            ChatMessage(
                id = "aria_welcome",
                role = ChatRole.Assistant,
                text = ChatCatalog.welcomeMessage,
            ),
        )
    }

    fun onInputChange(value: String) {
        inputText = value
    }

    suspend fun onSuggestionClick(suggestion: ChatSuggestion) {
        selectedSuggestionId = suggestion.id
        inputText = suggestion.prompt
        sendMessage()
    }

    fun applyVoiceResult(spoken: String) {
        inputText = if (inputText.isBlank()) {
            spoken
        } else {
            "${inputText.trimEnd()} $spoken"
        }
    }

    fun clearError() {
        errorMessage = null
    }

    suspend fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty() || isLoading) return

        _messages.add(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.User,
                text = text,
            ),
        )
        inputText = ""
        isLoading = true
        errorMessage = null

        try {
            if (!AiConfig.isChatConfigured) {
                throw IllegalStateException(
                    "Chat API key is missing. Add grok_api_key or deapi_api_key in Firebase Remote Config.",
                )
            }
            val history = buildList {
                add("system" to ChatCatalog.ARIA_SYSTEM_PROMPT)
                addAll(
                    _messages.map { message ->
                        val role = when (message.role) {
                            ChatRole.User -> "user"
                            ChatRole.Assistant -> "assistant"
                        }
                        role to message.text
                    },
                )
            }
            val reply = api.chat(history).getOrElse { throw it }
            _messages.add(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.Assistant,
                    text = reply.trim(),
                ),
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Chat failed"
            _messages.add(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.Assistant,
                    text = "Sorry, I couldn't get a response right now. Please try again.",
                ),
            )
        } finally {
            isLoading = false
        }
    }
}
