package com.aiartgenerator.imagegenerator.videogenerator.model

data class MusicMood(
    val id: String,
    val emoji: String,
    val label: String,
)

data class MusicGenreOption(
    val id: String,
    val label: String,
)

data class MusicDurationOption(
    val id: String,
    val label: String,
    val seconds: Int,
)

data class VoiceStyleOption(
    val id: String,
    val label: String,
)

object MusicCatalog {
    val moods = listOf(
        MusicMood("happy", "😊", "Happy"),
        MusicMood("sad", "😢", "Sad"),
        MusicMood("energetic", "⚡", "Energetic"),
        MusicMood("relaxed", "😌", "Relaxed"),
        MusicMood("intense", "🔥", "Intense"),
        MusicMood("dreamy", "🌙", "Dreamy"),
    )

    val genres = listOf(
        MusicGenreOption("lofi", "Lo-fi"),
        MusicGenreOption("pop", "Pop"),
        MusicGenreOption("hiphop", "Hip-hop"),
        MusicGenreOption("classical", "Classical"),
        MusicGenreOption("jazz", "Jazz"),
    )

    val durations = listOf(
        MusicDurationOption("15", "15s", 15),
        MusicDurationOption("30", "30s", 30),
        MusicDurationOption("60", "60s", 60),
        MusicDurationOption("120", "2min", 120),
        MusicDurationOption("180", "3min", 180),
    )

    val voiceStyles = listOf(
        VoiceStyleOption("no_voice", "No Voice"),
        VoiceStyleOption("male", "Male"),
        VoiceStyleOption("female", "Female"),
        VoiceStyleOption("choir", "Choir"),
    )

    fun findMood(id: String): MusicMood? = moods.firstOrNull { it.id == id }

    fun findGenre(id: String): MusicGenreOption? = genres.firstOrNull { it.id == id }

    fun findDuration(id: String): MusicDurationOption? = durations.firstOrNull { it.id == id }

    fun findVoiceStyle(id: String): VoiceStyleOption? = voiceStyles.firstOrNull { it.id == id }

    fun durationSeconds(id: String): Int = findDuration(id)?.seconds ?: 30

    fun titleFromPrompt(prompt: String): String {
        val words = prompt.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(4)
        if (words.isEmpty()) return "AI Music"
        return words.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { char -> char.uppercase() }
        }
    }
}
