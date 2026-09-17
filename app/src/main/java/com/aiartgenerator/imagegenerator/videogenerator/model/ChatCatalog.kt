package com.aiartgenerator.imagegenerator.videogenerator.model

enum class ChatRole {
    User,
    Assistant,
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
)

data class ChatSuggestion(
    val id: String,
    val label: String,
    val prompt: String,
)

object ChatCatalog {
    const val ARIA_SYSTEM_PROMPT =
        "You are a friendly creative AI assistant inside a mobile app. " +
            "Help users write, brainstorm, explain ideas, and craft prompts for image/video/music generation. " +
            "Keep replies concise, warm, and practical. Use **bold** for key product or feature names when helpful."

    const val welcomeMessage =
        "Hi! I'm your creative AI assistant. I can help you write, brainstorm ideas, " +
            "generate images, and more. What would you like to create today?"

    val suggestions = listOf(
        ChatSuggestion(
            id = "poem",
            label = "Write a poem 🌸",
            prompt = "Write a short creative poem about chasing dreams under city lights.",
        ),
        ChatSuggestion(
            id = "image",
            label = "Generate image 🖼️",
            prompt = "Help me write a detailed image generation prompt for a futuristic neon city at sunset.",
        ),
        ChatSuggestion(
            id = "explain",
            label = "Explain this 💡",
            prompt = "Explain how AI image generation works in simple terms for a beginner.",
        ),
    )
}
