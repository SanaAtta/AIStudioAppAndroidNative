package com.example.myapplication.model

enum class ChatRole {
    User,
    Assistant,
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
)

object ChatCatalog {
    val suggestedPrompts = listOf(
        "How to generate image",
        "Create a short video from my idea",
        "Help me design a cool wallpaper",
    )
}
