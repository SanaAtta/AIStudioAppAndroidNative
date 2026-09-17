package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.AspectRatioCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedImageStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import com.aiartgenerator.imagegenerator.videogenerator.model.StyleCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.StyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.PromptStyles
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits

class AiImageGeneratorController(
    private val api: AiApi = AiApi(),
) {
    private companion object {
        private const val TAG = "AiImageGenerator"

        /** Reads real pixel size from final image bytes (not requested API size). */
        private fun decodeActualImageSize(bytes: ByteArray): Pair<Int, Int> {
            val options =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            val width = options.outWidth
            val height = options.outHeight
            return if (width > 0 && height > 0) {
                width to height
            } else {
                0 to 0
            }
        }
    }
    var prompt by mutableStateOf(GeneratorDefaultPrompts.forImageStyle("realistic"))
        private set
    var selectedStyleId by mutableStateOf("realistic")
        private set
    var selectedAspectRatioId by mutableStateOf(AspectRatioCatalog.DEFAULT_ID)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var resultUri by mutableStateOf<Uri?>(null)
        private set
    var resultBytes by mutableStateOf<ByteArray?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var lastStyleDefaultPrompt: String = prompt

    val styles: List<StyleOption> get() = StyleCatalog.imageStyles
    val aspectRatios get() = AspectRatioCatalog.options
    val maxChars: Int = 1000
    val canGenerate: Boolean get() = prompt.isNotBlank() && !isLoading

    fun onPromptChange(value: String) {
        if (isLoading) return
        prompt = if (value.length > maxChars) value.take(maxChars) else value
    }

    fun onSuggest() {
        val suggested = GeneratorDefaultPrompts.forImageStyle(selectedStyleId)
        prompt = suggested
        lastStyleDefaultPrompt = suggested
    }

    fun onStyleSelected(styleId: String) {
        val nextDefault = GeneratorDefaultPrompts.forImageStyle(styleId)
        // Keep the user's custom prompt unless they are still on the previous style example.
        if (prompt.isBlank() || prompt == lastStyleDefaultPrompt) {
            prompt = nextDefault
            lastStyleDefaultPrompt = nextDefault
        }
        selectedStyleId = styleId
    }

    fun onAspectRatioSelected(aspectRatioId: String) {
        selectedAspectRatioId = aspectRatioId
    }

    fun clearError() {
        errorMessage = null
    }

    suspend fun onGenerate(context: Context, onSuccess: () -> Unit) {
        if (!canGenerate) {
            if (prompt.isBlank()) {
                errorMessage = context.getString(R.string.error_prompt_required)
            }
            return
        }
        if (PromptSafety.containsUnsafeContent(prompt)) {
            errorMessage = context.getString(R.string.error_prompt_unsafe)
            return
        }
        if (!FreeUsageLimits.canUse(FreeUsageKind.Image)) {
            errorMessage = context.getString(R.string.free_limit_reached)
            return
        }
        if (!AiConfig.isImageGenerationConfigured) {
            Log.w(TAG, "Pollinations key missing — refreshing Remote Config")
            ApiRemoteConfig.ensureFreshConfig()
        }
        if (!AiConfig.isImageGenerationConfigured) {
            Log.e(TAG, "Pollinations key still missing after Remote Config refresh")
            errorMessage = context.getString(R.string.error_pollinations_key_missing)
            return
        }
        Log.d(TAG, "Starting image generation promptLength=${prompt.length}")
        isLoading = true
        errorMessage = null
        // Drop any previous result so UI never shows a stale / unwanted image.
        GeneratedImageStore.clear()
        resultUri = null
        resultBytes = null
        try {
            val aspect = AspectRatioCatalog.findById(selectedAspectRatioId) ?: AspectRatioCatalog.default()
            val style = StyleCatalog.findById(selectedStyleId)
            val styleHint = PromptStyles.imageStyle(selectedStyleId)

            val bytes = api.generateImage(
                prompt = prompt,
                styleHint = styleHint,
                width = aspect.width,
                height = aspect.height,
            ).getOrElse { throw it }

            val (actualWidth, actualHeight) = decodeActualImageSize(bytes)
            if (actualWidth <= 0 || actualHeight <= 0) {
                throw IllegalStateException("Generated image has invalid dimensions")
            }
            Log.d(TAG, "Actual image size=${actualWidth}x$actualHeight (requested ${aspect.width}x${aspect.height})")

            val file = api.saveToCache(context, bytes, "image_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            val aspectLabel = aspect.label
            val styleTitle = style?.title ?: "Custom"
            val creation = GeneratedCreationsRepository.add(
                context = context,
                bytes = bytes,
                prompt = prompt,
                styleTitle = styleTitle,
                aspectLabel = aspectLabel,
                width = actualWidth,
                height = actualHeight,
            )

            resultBytes = bytes
            resultUri = uri
            GeneratedImageStore.current = GeneratedImageStore.Payload(
                bytes = bytes,
                uri = uri,
                prompt = prompt,
                styleTitle = styleTitle,
                aspectLabel = aspectLabel,
                width = actualWidth,
                height = actualHeight,
                creationId = creation.id,
            )
            FreeUsageLimits.recordSuccessfulUse(FreeUsageKind.Image)
            onSuccess()
            Log.d(TAG, "Image generation succeeded bytes=${bytes.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed: ${e.message}", e)
            errorMessage = e.message ?: "Image generation failed"
        } finally {
            isLoading = false
        }
    }
}
