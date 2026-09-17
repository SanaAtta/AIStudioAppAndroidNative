package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.AspectRatioCatalog
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideoStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import com.aiartgenerator.imagegenerator.videogenerator.model.StyleCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.StyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.VideoCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiProvider
import com.aiartgenerator.imagegenerator.videogenerator.model.network.DeApiClient
import com.aiartgenerator.imagegenerator.videogenerator.model.network.PromptStyles
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits
import java.io.IOException

class AiVideoGeneratorController(
    private val api: AiApi = AiApi(),
) {
    private companion object {
        private const val TAG = "AiVideoGenerator"
    }

    var prompt by mutableStateOf(GeneratorDefaultPrompts.VIDEO)
        private set
    var selectedStyleId by mutableStateOf("realistic")
        private set
    var selectedAspectRatioId by mutableStateOf("16:9")
        private set
    var selectedDurationId by mutableStateOf("5")
        private set
    var selectedCameraMotionId by mutableStateOf("zoom_in")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val styles: List<StyleOption> get() = StyleCatalog.imageStyles
    val aspectRatios get() = AspectRatioCatalog.options
    val durations get() = VideoCatalog.durations
    val cameraMotions get() = VideoCatalog.cameraMotions
    val maxChars: Int = 1000
    val canGenerate: Boolean get() = prompt.isNotBlank() && !isLoading

    fun onPromptChange(value: String) {
        if (isLoading) return
        prompt = if (value.length > maxChars) value.take(maxChars) else value
    }

    fun onSuggest() {
        prompt = GeneratorDefaultPrompts.VIDEO
    }

    fun onStyleSelected(styleId: String) {
        selectedStyleId = styleId
    }

    fun onAspectRatioSelected(aspectRatioId: String) {
        selectedAspectRatioId = aspectRatioId
    }

    fun onDurationSelected(durationId: String) {
        selectedDurationId = durationId
    }

    fun onCameraMotionSelected(motionId: String) {
        selectedCameraMotionId = motionId
    }

    fun clearError() {
        errorMessage = null
    }

    fun applyEditDraftIfAny() {
        GeneratedVideoStore.editDraft?.let { draft ->
            prompt = draft.prompt
            selectedStyleId = draft.styleId
            selectedDurationId = draft.durationId
            selectedCameraMotionId = draft.cameraMotionId
            selectedAspectRatioId = draft.aspectRatioId
            GeneratedVideoStore.editDraft = null
        }
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
        if (!FreeUsageLimits.canUse(FreeUsageKind.Video)) {
            errorMessage = context.getString(R.string.free_limit_reached)
            return
        }

        val aspect = AspectRatioCatalog.findById(selectedAspectRatioId)
            ?: AspectRatioCatalog.options.first { it.id == "16:9" }
        val style = StyleCatalog.findById(selectedStyleId)
        val styleTitle = style?.title ?: "Realistic"
        val durationSeconds = VideoCatalog.durationSeconds(selectedDurationId)
        val effectivePrompt = prompt.trim()

        if (!AiConfig.isVideoGenerationConfigured) {
            errorMessage = context.getString(R.string.error_deapi_key_missing)
            return
        }

        isLoading = true
        errorMessage = null
        try {
            val styleHint = buildString {
                append(PromptStyles.imageStyle(selectedStyleId))
                val motionHint = VideoCatalog.cameraMotionHint(selectedCameraMotionId)
                if (motionHint.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(motionHint)
                }
            }
            val maxSupported = DeApiClient.maxVideoDurationSeconds(AiConfig.DEAPI_VIDEO_MODEL)
            val effectiveDurationSeconds = durationSeconds.coerceIn(2, maxSupported)
            val mappedAspectRatio = VideoCatalog.videoAspectRatio(aspect.id)
            Log.i(
                TAG,
                "Using API: ${ApiProvider.label(ApiProvider.DEAPI)} → text-to-video " +
                    "(duration=${effectiveDurationSeconds}s, aspect=$mappedAspectRatio)",
            )
            val bytes = api.generateVideo(
                prompt = effectivePrompt,
                styleHint = styleHint,
                durationSeconds = effectiveDurationSeconds,
                aspectRatio = mappedAspectRatio,
            ).getOrElse { throw it }

            val storedDurationSeconds = effectiveDurationSeconds
            val video = GeneratedVideosRepository.add(
                context = context,
                bytes = bytes,
                prompt = effectivePrompt,
                styleTitle = styleTitle,
                durationSeconds = storedDurationSeconds,
                cameraMotion = selectedCameraMotionId,
                aspectLabel = aspect.label,
            )
            val uri = GeneratedVideosRepository.getUri(context, video)
                ?: throw IOException("Unable to access generated video")

            GeneratedVideoStore.current = GeneratedVideoStore.Payload(
                uri = uri,
                prompt = effectivePrompt,
                styleTitle = styleTitle,
                durationSeconds = storedDurationSeconds,
                cameraMotion = selectedCameraMotionId,
                aspectLabel = aspect.label,
                creationId = video.id,
            )
            FreeUsageLimits.recordSuccessfulUse(FreeUsageKind.Video)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Video generation failed: ${e.message}", e)
            errorMessage = e.message ?: "Video generation failed"
        } finally {
            isLoading = false
        }
    }
}
