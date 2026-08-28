package com.example.myapplication.controller

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.example.myapplication.R
import com.example.myapplication.model.StyleCatalog
import com.example.myapplication.model.StyleOption
import com.example.myapplication.model.VideoCatalog
import com.example.myapplication.model.VideoDuration
import com.example.myapplication.model.VideoResolution
import com.example.myapplication.network.AiApi
import com.example.myapplication.network.AiConfig
import com.example.myapplication.network.PromptStyles
import com.example.myapplication.safety.PromptSafety
import com.example.myapplication.util.AndroidFileHelper

class AiVideoGeneratorController(
    private val api: AiApi = AiApi(),
) {
    var prompt by mutableStateOf("")
        private set
    var selectedStyleId by mutableStateOf("none")
        private set
    var selectedResolutionId by mutableStateOf("720")
        private set
    var selectedDurationId by mutableStateOf("5")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var resultUri by mutableStateOf<Uri?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val styles: List<StyleOption> get() = StyleCatalog.imageStyles
    val resolutions: List<VideoResolution> get() = VideoCatalog.resolutions
    val durations: List<VideoDuration> get() = VideoCatalog.durations
    val maxChars: Int = 500
    val canGenerate: Boolean get() = prompt.isNotBlank() && !isLoading

    fun onPromptChange(value: String) {
        if (value.length <= maxChars) prompt = value
    }

    fun onSuggest() {
        prompt = "A cinematic drone shot flying over misty mountains at sunrise"
    }

    fun onStyleSelected(styleId: String) {
        selectedStyleId = styleId
    }

    fun onResolutionSelected(resolutionId: String) {
        selectedResolutionId = resolutionId
    }

    fun onDurationSelected(durationId: String) {
        selectedDurationId = durationId
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
            val (width, height, model) = videoSizeAndModel(selectedResolutionId)
            val bytes = api.generateVideo(
                prompt = prompt,
                styleHint = PromptStyles.imageStyle(selectedStyleId),
                durationSeconds = selectedDurationId.toIntOrNull() ?: 5,
                width = width,
                height = height,
                model = model,
            ).getOrElse { throw it }
            val file = AndroidFileHelper.saveToCache(context, bytes, "video_${System.currentTimeMillis()}.mp4")
            resultUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Video generation failed"
        } finally {
            isLoading = false
        }
    }

    private fun videoSizeAndModel(resolutionId: String): Triple<Int, Int, String> = when (resolutionId) {
        "360" -> Triple(640, 360, AiConfig.VIDEO_MODEL)
        "540" -> Triple(960, 540, AiConfig.VIDEO_MODEL)
        "1080" -> Triple(1920, 1080, "wan-pro-1080p")
        else -> Triple(1280, 720, AiConfig.VIDEO_MODEL)
    }
}
