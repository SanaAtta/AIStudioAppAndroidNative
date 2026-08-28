package com.example.myapplication.controller

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.example.myapplication.R
import com.example.myapplication.model.MusicCatalog
import com.example.myapplication.model.MusicGenre
import com.example.myapplication.model.VocalOption
import com.example.myapplication.network.AiApi
import com.example.myapplication.network.PromptStyles
import com.example.myapplication.safety.PromptSafety
import com.example.myapplication.util.AndroidFileHelper

class AiMusicGeneratorController(
    private val api: AiApi = AiApi(),
) {
    var prompt by mutableStateOf("")
        private set
    var selectedGenreId by mutableStateOf("none")
        private set
    var selectedVocalId by mutableStateOf("instrumental")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var resultUri by mutableStateOf<Uri?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val genres: List<MusicGenre> get() = MusicCatalog.genres
    val vocals: List<VocalOption> get() = MusicCatalog.vocals
    val maxChars: Int = 500
    val canGenerate: Boolean get() = prompt.isNotBlank() && !isLoading

    fun onPromptChange(value: String) {
        if (value.length <= maxChars) prompt = value
    }

    fun onClearPrompt() {
        prompt = ""
    }

    fun onSurpriseMe() {
        prompt = "A chill song in rainy weather with soft piano and distant thunder"
    }

    fun onGenreSelected(genreId: String) {
        selectedGenreId = genreId
    }

    fun onVocalSelected(vocalId: String) {
        selectedVocalId = vocalId
    }

    fun clearError() {
        errorMessage = null
    }

    suspend fun onGenerate(context: Context) {
        if (!canGenerate) return
        if (PromptSafety.containsUnsafeContent(prompt)) {
            errorMessage = context.getString(R.string.error_prompt_unsafe)
            return
        }
        isLoading = true
        errorMessage = null
        try {
            val (vocalHint, instrumental) = PromptStyles.musicVocal(selectedVocalId)
            val genreHint = listOfNotNull(
                PromptStyles.musicGenre(selectedGenreId).ifBlank { null },
                vocalHint,
            ).joinToString(", ")
            val bytes = api.generateMusic(
                prompt = prompt,
                genreHint = genreHint,
                instrumental = instrumental,
            ).getOrElse { throw it }
            val file = AndroidFileHelper.saveToCache(context, bytes, "music_${System.currentTimeMillis()}.mp3")
            resultUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Music generation failed"
        } finally {
            isLoading = false
        }
    }
}
