package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.EditCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.EditResultStore
import com.aiartgenerator.imagegenerator.videogenerator.model.EditTool
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.PromptStyles
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits

class AiEditController(
    private val api: AiApi = AiApi(),
) {
    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var selectedToolId by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val tools: List<EditTool> get() = EditCatalog.tools

    fun filteredTools(context: Context): List<EditTool> {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) return tools
        return tools.filter { tool ->
            context.getString(tool.labelRes).lowercase().contains(query) ||
                tool.prompt.lowercase().contains(query) ||
                tool.id.replace('_', ' ').contains(query)
        }
    }

    val selectedTool: EditTool?
        get() = selectedToolId?.let(EditCatalog::findById)

    val hasImage: Boolean get() = selectedImageUri != null
    val canApply: Boolean get() = hasImage && selectedTool != null && !isLoading

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
    }

    fun onSearchChange(value: String) {
        searchQuery = value
    }

    fun onToolSelected(tool: EditTool) {
        selectedToolId = tool.id
    }

    fun clearError() {
        errorMessage = null
    }

    suspend fun onApply(context: Context, onSuccess: () -> Unit) {
        val source = selectedImageUri ?: return
        val tool = selectedTool ?: return
        if (isLoading) return
        if (!FreeUsageLimits.canUse(FreeUsageKind.Edit)) {
            errorMessage = context.getString(R.string.free_limit_reached)
            return
        }
        if (!AiConfig.isImageEditConfigured) {
            ApiRemoteConfig.ensureFreshConfig()
        }
        if (!AiConfig.isImageEditConfigured) {
            errorMessage = context.getString(R.string.error_pollinations_key_missing)
            return
        }

        isLoading = true
        errorMessage = null
        try {
            val bytes = api.editImage(
                context = context,
                imageUri = source,
                prompt = tool.prompt,
                styleHint = PromptStyles.editStyle(tool.styleHint),
            ).getOrElse { throw it }
            val file = api.saveToCache(context, bytes, "edit_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            EditResultStore.current = EditResultStore.Payload(
                originalUri = source,
                resultBytes = bytes,
                resultUri = uri,
                toolLabel = context.getString(tool.labelRes),
                toolPrompt = tool.prompt,
            )
            FreeUsageLimits.recordSuccessfulUse(FreeUsageKind.Edit)
            onSuccess()
        } catch (e: Exception) {
            errorMessage = e.message ?: context.getString(R.string.ai_edit_failed)
        } finally {
            isLoading = false
        }
    }

    suspend fun downloadFromPayload(context: Context, payload: EditResultStore.Payload): Boolean {
        val bytes = payload.resultBytes ?: return false
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return false
            saveBitmapToGallery(context, bitmap, "ai_edit_${System.currentTimeMillis()}.jpg")
        } catch (_: Exception) {
            false
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Editor")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) return false
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return true
    }
}
