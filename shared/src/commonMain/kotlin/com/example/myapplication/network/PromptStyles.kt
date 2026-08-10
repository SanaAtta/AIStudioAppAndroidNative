package com.example.myapplication.network

object PromptStyles {
    fun imageStyle(styleId: String): String = when (styleId) {
        "realistic" -> "photorealistic, ultra detailed, natural lighting"
        "cartoon" -> "cartoon style, vibrant colors, clean lines"
        "anime" -> "anime style, detailed illustration"
        "oil" -> "oil painting, textured brush strokes"
        "cyber" -> "cyberpunk, neon lights, futuristic"
        else -> ""
    }

    fun editStyle(style: String): String = when (style.lowercase()) {
        "enhance" -> "enhanced details, sharper, higher quality"
        "realistic" -> "photorealistic finish"
        "moody" -> "moody cinematic lighting, dramatic atmosphere"
        else -> ""
    }

    fun musicGenre(genreId: String): String = when (genreId) {
        "rock" -> "rock genre"
        "pop" -> "pop genre"
        "rap" -> "hip hop rap genre"
        "jazz" -> "jazz genre"
        "edm" -> "edm electronic dance genre"
        else -> ""
    }

    fun musicVocal(vocalId: String): Pair<String, Boolean> = when (vocalId) {
        "female" -> "female vocals" to false
        "male" -> "male vocals" to false
        "random" -> "vocals" to false
        else -> "instrumental" to true
    }
}
