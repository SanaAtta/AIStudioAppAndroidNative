package com.example.myapplication.controller

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.example.myapplication.R
import com.example.myapplication.model.StyleCatalog
import com.example.myapplication.network.AiApi
import com.example.myapplication.network.PromptStyles
import com.example.myapplication.safety.PromptSafety
import com.example.myapplication.util.AndroidFileHelper

class AiEditController(
    private val api: AiApi = AiApi(),
) {
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    var editText by mutableStateOf("")
        private set
    var selectedStyle by mutableStateOf(StyleCatalog.editStyles.first())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var resultUri by mutableStateOf<Uri?>(null)
        private set
    var resultBytes by mutableStateOf<ByteArray?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val styles: List<String> get() = StyleCatalog.editStyles
    val maxChars: Int = 500
    val hasImage: Boolean get() = selectedImageUri != null
    val canGenerate: Boolean get() = hasImage && editText.isNotBlank() && !isLoading

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
        resultUri = null
        resultBytes = null
    }

    fun onEditTextChange(value: String) {
        if (value.length <= maxChars) editText = value
    }

    fun onSuggest() {
        editText = "Make it more dramatic with cinematic lighting"
    }

    fun onStyleSelected(style: String) {
        selectedStyle = style
    }

    fun clearError() {
        errorMessage = null
    }

    suspend fun onGenerate(context: Context) {
        val source = selectedImageUri ?: return
        if (!canGenerate) return
        if (PromptSafety.containsUnsafeContent(editText)) {
            errorMessage = context.getString(R.string.error_prompt_unsafe)
            return
        }
        isLoading = true
        errorMessage = null
        try {
            val imageBytes = AndroidFileHelper.readBytes(context, source)
            val bytes = api.editImage(
                imageBytes = imageBytes,
                prompt = editText,
                styleHint = PromptStyles.editStyle(selectedStyle),
            ).getOrElse { throw it }
            val file = AndroidFileHelper.saveToCache(context, bytes, "edit_${System.currentTimeMillis()}.jpg")
            resultBytes = bytes
            resultUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Edit failed"
        } finally {
            isLoading = false
        }
    }
}
