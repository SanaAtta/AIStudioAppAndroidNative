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
import com.example.myapplication.network.AiApi
import com.example.myapplication.network.PromptStyles
import com.example.myapplication.safety.PromptSafety
import com.example.myapplication.util.AndroidFileHelper

class AiImageGeneratorController(
    private val api: AiApi = AiApi(),
) {
    var prompt by mutableStateOf("")
        private set
    var selectedStyleId by mutableStateOf("none")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var resultUri by mutableStateOf<Uri?>(null)
        private set
    var resultBytes by mutableStateOf<ByteArray?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val styles: List<StyleOption> get() = StyleCatalog.imageStyles
    val maxChars: Int = 500
    val canGenerate: Boolean get() = prompt.isNotBlank() && !isLoading

    fun onPromptChange(value: String) {
        if (value.length <= maxChars) prompt = value
    }

    fun onSuggest() {
        prompt = "A cinematic portrait of a fox in a misty forest at golden hour"
    }

    fun onStyleSelected(styleId: String) {
        selectedStyleId = styleId
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
            val bytes = api.generateImage(
                prompt = prompt,
                styleHint = PromptStyles.imageStyle(selectedStyleId),
            ).getOrElse { throw it }
            val file = AndroidFileHelper.saveToCache(context, bytes, "image_${System.currentTimeMillis()}.jpg")
            resultBytes = bytes
            resultUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Image generation failed"
        } finally {
            isLoading = false
        }
    }
}
