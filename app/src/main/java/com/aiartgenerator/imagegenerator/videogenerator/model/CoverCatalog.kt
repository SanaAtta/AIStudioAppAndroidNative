package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.DrawableRes
import com.aiartgenerator.imagegenerator.videogenerator.R

data class SingerStyleOption(
    val id: String,
    val label: String,
    @DrawableRes val iconRes: Int,
)

data class CoverVoiceStyleOption(
    val id: String,
    val label: String,
)

object CoverCatalog {
    val singerStyles = listOf(
        SingerStyleOption("pop_star", "Pop Star", R.drawable.ic_singer_pop_star),
        SingerStyleOption("jazz", "Jazz", R.drawable.ic_singer_jazz),
        SingerStyleOption("rock", "Rock", R.drawable.ic_singer_rock),
        SingerStyleOption("rnb", "R&B", R.drawable.ic_singer_rnb),
    )

    val voiceStyles = listOf(
        CoverVoiceStyleOption("original", "Original"),
        CoverVoiceStyleOption("vibrato", "Vibrato"),
        CoverVoiceStyleOption("falsetto", "Falsetto"),
        CoverVoiceStyleOption("breathy", "Breathy"),
        CoverVoiceStyleOption("powerful", "Powerful"),
    )

    fun findSingerStyle(id: String): SingerStyleOption? =
        singerStyles.firstOrNull { it.id == id }

    fun findVoiceStyle(id: String): CoverVoiceStyleOption? =
        voiceStyles.firstOrNull { it.id == id }

    fun singerStyleHint(id: String): String = when (id) {
        "pop_star" -> "pop star singer voice style"
        "jazz" -> "jazz singer voice style"
        "rock" -> "rock singer voice style"
        "rnb" -> "r&b singer voice style"
        else -> "pop star singer voice style"
    }

    fun coverVoiceHint(id: String): String = when (id) {
        "vibrato" -> "with vibrato"
        "falsetto" -> "with falsetto"
        "breathy" -> "breathy vocal tone"
        "powerful" -> "powerful vocal delivery"
        "original" -> "natural original voice"
        else -> "natural original voice"
    }

    /** Prompt text sung by TTS — driven by selected singer + voice options. */
    fun coverPerformanceLyrics(singerStyleId: String, voiceStyleId: String): String {
        val voice = coverVoiceHint(voiceStyleId)
        return when (singerStyleId) {
            "jazz" ->
                "Singing a smooth jazz cover, soft swing and velvet tone, $voice. " +
                    "Moonlight melodies drifting through the night."
            "rock" ->
                "Singing a bold rock cover, raw energy and drive, $voice. " +
                    "Thunder in my heart, fire in every line."
            "rnb" ->
                "Singing a soulful R and B cover, warm groove and emotion, $voice. " +
                    "Late night feelings, velvet rhythm, hold me close."
            else ->
                "Singing a bright pop cover, radio ready and catchy, $voice. " +
                    "Lights up high, dancing through the neon sky."
        }
    }

    fun titleFromSongName(name: String, singerLabel: String): String {
        val base = name.substringBeforeLast('.').take(28).ifBlank { "My Song" }
        return "$base Cover"
    }

    fun styleSubtitle(singerLabel: String, voiceLabel: String): String {
        val voice = voiceLabel.trim().takeIf { it.isNotEmpty() && !it.equals("Original", ignoreCase = true) }
        return if (voice == null) "AI Cover · $singerLabel" else "AI Cover · $singerLabel · $voice"
    }

    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(minutes, secs)
    }
}
