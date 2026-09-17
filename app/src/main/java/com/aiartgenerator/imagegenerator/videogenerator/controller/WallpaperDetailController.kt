package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperApplyTarget
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperItem
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperDetailController(
    wallpaperId: String,
    private val api: AiApi = AiApi(),
) {
    val wallpaper: WallpaperItem? = WallpaperCatalog.wallpaperById(wallpaperId)
    val previewUrl: String? = wallpaper?.imageUrl(api)

    var showApplyDialog by mutableStateOf(false)
        private set
    var selectedTarget by mutableStateOf(WallpaperApplyTarget.Home)
        private set
    var isDownloading by mutableStateOf(false)
        private set
    var isSharing by mutableStateOf(false)
        private set
    var isApplying by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun openApplyDialog() {
        showApplyDialog = true
    }

    fun dismissApplyDialog() {
        showApplyDialog = false
    }

    fun selectTarget(target: WallpaperApplyTarget) {
        selectedTarget = target
    }

    fun clearStatus() {
        statusMessage = null
    }

    suspend fun download(context: Context): Boolean {
        val item = wallpaper ?: return false
        isDownloading = true
        return try {
            val bitmap = loadWallpaperBitmap(item)
            val saved = withContext(Dispatchers.IO) {
                saveBitmapToGallery(context, bitmap, "wallapp_${item.id}.jpg")
            }
            statusMessage = if (saved) "Wallpaper downloaded to gallery" else "Download failed"
            saved
        } catch (e: Exception) {
            statusMessage = "Download failed: ${e.message}"
            false
        } finally {
            isDownloading = false
        }
    }

    suspend fun share(context: Context): Boolean {
        val item = wallpaper ?: return false
        if (isSharing) return false
        isSharing = true
        return try {
            val bitmap = loadWallpaperBitmap(item)
            val uri = withContext(Dispatchers.IO) {
                writeShareCache(context, bitmap, "share_${item.id}.jpg")
            }
            if (uri == null) {
                statusMessage = "Share failed"
                return false
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                putExtra(Intent.EXTRA_TEXT, item.title)
                clipData = ClipData.newUri(context.contentResolver, item.title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share wallpaper"))
            true
        } catch (e: Exception) {
            statusMessage = "Share failed: ${e.message}"
            false
        } finally {
            isSharing = false
        }
    }

    suspend fun applyWallpaper(context: Context): Boolean {
        val item = wallpaper ?: return false
        isApplying = true
        return try {
            val bitmap = loadWallpaperBitmap(item)
            val success = withContext(Dispatchers.IO) {
                setSystemWallpaper(context, bitmap, selectedTarget)
            }
            showApplyDialog = false
            statusMessage = if (success) {
                "Wallpaper applied to ${selectedTarget.label}"
            } else {
                "Failed to apply wallpaper"
            }
            success
        } catch (e: Exception) {
            statusMessage = "Apply failed: ${e.message}"
            false
        } finally {
            isApplying = false
        }
    }

    private suspend fun loadWallpaperBitmap(item: WallpaperItem): Bitmap {
        val bytes = api.downloadUrl(item.imageUrl(api)).getOrElse { throw it }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Unable to decode wallpaper image")
    }

    private fun writeShareCache(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val dir = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) return null
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Wallpapers")
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

    private fun setSystemWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperApplyTarget,
    ): Boolean {
        val manager = WallpaperManager.getInstance(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val flags = when (target) {
                WallpaperApplyTarget.Home -> WallpaperManager.FLAG_SYSTEM
                WallpaperApplyTarget.Lock -> WallpaperManager.FLAG_LOCK
                WallpaperApplyTarget.Both -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            manager.setBitmap(bitmap, null, true, flags)
            true
        } else {
            manager.setBitmap(bitmap)
            true
        }
    }
}
