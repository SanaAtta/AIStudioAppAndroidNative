package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusicStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusicsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicDurationOption
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicGenreOption
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicMood
import com.aiartgenerator.imagegenerator.videogenerator.model.VoiceStyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiProvider
import com.aiartgenerator.imagegenerator.videogenerator.model.network.PromptStyles
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits
import java.io.IOException

class AiMusicGeneratorController(
    private val api: AiApi = AiApi(),
) {
    private companion object {
        private const val TAG = "AiMusicGenerator"
    }

    var prompt by mutableStateOf(GeneratorDefaultPrompts.MUSIC)
        private set
    var selectedMoodId by mutableStateOf("relaxed")
        private set
    var selectedGenreId by mutableStateOf("lofi")
        private set
    var selectedDurationId by mutableStateOf("30")
        private set
    var selectedVoiceStyleId by mutableStateOf("no_voice")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val moods: List<MusicMood> get() = MusicCatalog.moods
    val genres: List<MusicGenreOption> get() = MusicCatalog.genres
    val durations: List<MusicDurationOption> get() = MusicCatalog.durations
    val voiceStyles: List<VoiceStyleOption> get() = MusicCatalog.voiceStyles
    val maxChars: Int = 1000
    val canGenerate: Boolean get() = prompt.isNotBlank() && !isLoading

    val selectedDurationLabel: String
        get() = MusicCatalog.findDuration(selectedDurationId)?.label ?: "30s"

    fun onPromptChange(value: String) {
        if (isLoading) return
        prompt = if (value.length > maxChars) value.take(maxChars) else value
    }

    fun onSuggest() {
        prompt = GeneratorDefaultPrompts.MUSIC
    }

    fun onMoodSelected(moodId: String) {
        selectedMoodId = moodId
    }

    fun onGenreSelected(genreId: String) {
        selectedGenreId = genreId
    }

    fun onDurationSelected(durationId: String) {
        selectedDurationId = durationId
    }

    fun onVoiceStyleSelected(voiceStyleId: String) {
        selectedVoiceStyleId = voiceStyleId
    }

    fun clearError() {
        errorMessage = null
    }

    suspend fun onGenerate(context: Context, onSuccess: () -> Unit) {
        if (isLoading) return

        if (prompt.isBlank()) {
            errorMessage = context.getString(R.string.error_prompt_required)
            return
        }
        if (PromptSafety.containsUnsafeContent(prompt)) {
            errorMessage = context.getString(R.string.error_prompt_unsafe)
            return
        }
        if (!FreeUsageLimits.canUse(FreeUsageKind.Music)) {
            errorMessage = context.getString(R.string.free_limit_reached)
            return
        }

        val mood = MusicCatalog.findMood(selectedMoodId)
        val genre = MusicCatalog.findGenre(selectedGenreId)
        val voiceStyle = MusicCatalog.findVoiceStyle(selectedVoiceStyleId)
        val durationSeconds = MusicCatalog.durationSeconds(selectedDurationId)
        val effectivePrompt = prompt.trim()

        if (!AiConfig.isMusicGenerationConfigured) {
            errorMessage = context.getString(R.string.error_deapi_key_missing)
            return
        }

        isLoading = true
        errorMessage = null
        try {
            val (vocalHint, instrumental) = PromptStyles.musicVoice(selectedVoiceStyleId)
            val styleHint = listOfNotNull(
                PromptStyles.musicMood(selectedMoodId).ifBlank { null },
                PromptStyles.musicGenre(selectedGenreId).ifBlank { null },
                vocalHint,
            ).joinToString(", ")

            Log.i(
                TAG,
                "Using API: ${ApiProvider.label(ApiProvider.DEAPI)} → text-to-music " +
                    "(duration=${durationSeconds}s, mood=$selectedMoodId, genre=$selectedGenreId)",
            )

            val bytes = api.generateMusic(
                prompt = effectivePrompt,
                genreHint = styleHint,
                instrumental = instrumental,
                durationSeconds = durationSeconds,
            ).getOrElse { throw it }

            Log.d(
                TAG,
                "Music API success: bytes=${bytes.size}, title=${MusicCatalog.titleFromPrompt(effectivePrompt)}",
            )

            val music = GeneratedMusicsRepository.add(
                context = context,
                bytes = bytes,
                prompt = effectivePrompt,
                moodLabel = mood?.label ?: "Relaxed",
                genreLabel = genre?.label ?: "Lo-fi",
                durationSeconds = durationSeconds,
                voiceStyleLabel = voiceStyle?.label ?: "No Voice",
            )
            val uri = GeneratedMusicsRepository.getUri(context, music)
                ?: throw IOException("Unable to access generated music")

            GeneratedMusicStore.current = GeneratedMusicStore.Payload(
                uri = uri,
                title = music.title,
                prompt = effectivePrompt,
                moodLabel = music.moodLabel,
                genreLabel = music.genreLabel,
                durationSeconds = durationSeconds,
                voiceStyleLabel = music.voiceStyleLabel,
                creationId = music.id,
            )
            FreeUsageLimits.recordSuccessfulUse(FreeUsageKind.Music)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Music generation failed: ${e.message}", e)
            errorMessage = e.message ?: "Music generation failed"
        } finally {
            isLoading = false
        }
    }
}
