package com.example.myapplication.controller

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.myapplication.model.ChatCatalog
import com.example.myapplication.model.ChatMessage
import com.example.myapplication.model.ChatRole
import com.example.myapplication.network.AiApi
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

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    val suggestedPrompts: List<String> get() = ChatCatalog.suggestedPrompts
    val canSend: Boolean get() = inputText.isNotBlank() && !isLoading
    val showWelcome: Boolean get() = _messages.isEmpty()

    fun onInputChange(value: String) {
        inputText = value
    }

    suspend fun onSuggestionClick(prompt: String) {
        inputText = prompt
        sendMessage()
    }

    fun onMicClick() {
        // Future: voice input
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
            val history = _messages.map { message ->
                val role = when (message.role) {
                    ChatRole.User -> "user"
                    ChatRole.Assistant -> "assistant"
                }
                role to message.text
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
                    text = "Sorry, I couldn't get a response. Please try again.",
                ),
            )
        } finally {
            isLoading = false
        }
    }
}
