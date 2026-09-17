package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CreationActions {

    fun share(context: Context, creation: GeneratedCreation) {
        val uri = GeneratedCreationsRepository.getUri(context, creation)
        if (uri == null) {
            runCatching { Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show() }
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share image"))
    }

    fun share(context: Context, item: LibraryItem) {
        when (item.source) {
            LibrarySource.Creation -> {
                val creation = GeneratedCreationsRepository.findById(item.id)
                if (creation != null) {
                    share(context, creation)
                } else {
                    runCatching { Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show() }
                }
            }
            LibrarySource.Video -> {
                val video = GeneratedVideosRepository.findById(item.id)
                if (video != null) {
                    val uri = GeneratedVideosRepository.getUri(context, video)
                    if (uri == null) {
                        runCatching { Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show() }
                        return
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/mp4"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                } else {
                    runCatching { Toast.makeText(context, "Video not found", Toast.LENGTH_SHORT).show() }
                }
            }
            LibrarySource.Music -> {
                val music = GeneratedMusicsRepository.findById(item.id)
                if (music != null) {
                    val uri = GeneratedMusicsRepository.getUri(context, music)
                    if (uri == null) {
                        runCatching { Toast.makeText(context, "Audio not found", Toast.LENGTH_SHORT).show() }
                        return
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/mpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share music"))
                } else {
                    runCatching { Toast.makeText(context, "Audio not found", Toast.LENGTH_SHORT).show() }
                }
            }
            LibrarySource.Cover -> {
                val cover = GeneratedCoversRepository.findById(item.id)
                if (cover != null) {
                    val uri = GeneratedCoversRepository.getUri(context, cover)
                    if (uri == null) {
                        runCatching { Toast.makeText(context, "Audio not found", Toast.LENGTH_SHORT).show() }
                        return
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/mpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share cover"))
                } else {
                    runCatching { Toast.makeText(context, "Audio not found", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    suspend fun saveImageBytesToGallery(
        context: Context,
        imageBytes: ByteArray,
        fileNamePrefix: String = "ai_image",
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) return@withContext false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, it)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext false

        val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ai Studio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        val written = runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(imageBytes)
                output.flush()
                true
            } ?: false
        }.getOrDefault(false)

        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return@withContext false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            scanMediaFile(context, uri, "image/jpeg")
        }
        true
    }

    suspend fun saveVideoBytesToGallery(
        context: Context,
        videoBytes: ByteArray,
        fileNamePrefix: String = "ai_video",
    ): Boolean = withContext(Dispatchers.IO) {
        if (videoBytes.isEmpty()) return@withContext false

        val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Ai Studio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        val written = runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(videoBytes)
                output.flush()
                true
            } ?: false
        }.getOrDefault(false)

        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return@withContext false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            scanMediaFile(context, uri, "video/mp4")
        }
        true
    }

    private fun scanMediaFile(context: Context, uri: Uri, mimeType: String) {
        runCatching {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(0)
                    if (!path.isNullOrBlank()) {
                        MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
                    }
                }
            }
        }
    }

    suspend fun downloadToGallery(context: Context, creation: GeneratedCreation): Boolean =
        withContext(Dispatchers.IO) {
            val file = GeneratedCreationsRepository.getFile(context, creation)
            if (!file.exists()) return@withContext false
            val imageBytes = runCatching { file.readBytes() }.getOrNull()
            if (imageBytes == null || imageBytes.isEmpty()) return@withContext false
            saveImageBytesToGallery(context, imageBytes, "ai_studio_${creation.id}")
        }

    suspend fun downloadVideoToGallery(context: Context, video: GeneratedVideo): Boolean =
        withContext(Dispatchers.IO) {
            val file = GeneratedVideosRepository.getFile(context, video)
            if (!file.exists()) return@withContext false
            val videoBytes = runCatching { file.readBytes() }.getOrNull()
            if (videoBytes == null || videoBytes.isEmpty()) return@withContext false
            saveVideoBytesToGallery(context, videoBytes, "ai_studio_${video.id}")
        }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? = runCatching {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            java.net.URL(uri.toString()).openStream().use { it.readBytes() }
        } else {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }.getOrNull()

    suspend fun downloadImageFromPayload(
        context: Context,
        payload: GeneratedImageStore.Payload,
    ): Boolean = withContext(Dispatchers.IO) {
        if (payload.creationId != null) {
            val creation = GeneratedCreationsRepository.findById(payload.creationId)
            if (creation != null) {
                return@withContext downloadToGallery(context, creation)
            }
        }
        if (payload.bytes != null && payload.bytes.isNotEmpty()) {
            return@withContext saveImageBytesToGallery(
                context = context,
                imageBytes = payload.bytes,
                fileNamePrefix = "ai_image_${payload.creationId ?: System.currentTimeMillis()}",
            )
        }
        if (payload.uri != null) {
            val bytes = readBytesFromUri(context, payload.uri)
            if (bytes != null && bytes.isNotEmpty()) {
                return@withContext saveImageBytesToGallery(
                    context = context,
                    imageBytes = bytes,
                    fileNamePrefix = "ai_image_${payload.creationId ?: System.currentTimeMillis()}",
                )
            }
        }
        false
    }

    suspend fun downloadVideoFromPayload(
        context: Context,
        payload: GeneratedVideoStore.Payload,
    ): Boolean = withContext(Dispatchers.IO) {
        if (payload.creationId != null) {
            val video = GeneratedVideosRepository.findById(payload.creationId)
            if (video != null) {
                return@withContext downloadVideoToGallery(context, video)
            }
        }
        if (payload.uri != null) {
            val bytes = readBytesFromUri(context, payload.uri)
            if (bytes != null && bytes.isNotEmpty()) {
                return@withContext saveVideoBytesToGallery(
                    context = context,
                    videoBytes = bytes,
                    fileNamePrefix = "ai_video_${payload.creationId ?: System.currentTimeMillis()}",
                )
            }
        }
        false
    }

    suspend fun downloadAudioToStorage(context: Context, file: File, id: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext false
            val bytes = runCatching { file.readBytes() }.getOrNull()
            if (bytes == null || bytes.isEmpty()) return@withContext false

            val fileName = "ai_studio_${id}.mp3"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Ai Studio")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }

    suspend fun downloadToGallery(context: Context, item: LibraryItem): Boolean =
        when (item.source) {
            LibrarySource.Creation -> {
                val creation = GeneratedCreationsRepository.findById(item.id)
                if (creation != null) {
                    downloadToGallery(context, creation)
                } else if (item.previewUri != null) {
                    val bytes = readBytesFromUri(context, item.previewUri)
                    if (bytes != null && bytes.isNotEmpty()) {
                        saveImageBytesToGallery(context, bytes, "ai_studio_${item.id}")
                    } else false
                } else false
            }
            LibrarySource.Video -> {
                val video = GeneratedVideosRepository.findById(item.id)
                if (video != null) {
                    downloadVideoToGallery(context, video)
                } else if (item.previewUri != null) {
                    val bytes = readBytesFromUri(context, item.previewUri)
                    if (bytes != null && bytes.isNotEmpty()) {
                        saveVideoBytesToGallery(context, bytes, "ai_studio_${item.id}")
                    } else false
                } else false
            }
            LibrarySource.Music -> {
                val music = GeneratedMusicsRepository.findById(item.id)
                if (music != null) {
                    val file = GeneratedMusicsRepository.getFile(context, music)
                    downloadAudioToStorage(context, file, music.id)
                } else false
            }
            LibrarySource.Cover -> {
                val cover = GeneratedCoversRepository.findById(item.id)
                if (cover != null) {
                    val file = GeneratedCoversRepository.getFile(context, cover)
                    downloadAudioToStorage(context, file, cover.id)
                } else false
            }
        }

    fun toggleFavorite(context: Context, creation: GeneratedCreation): Boolean {
        val isFavorite = GeneratedCreationsRepository.toggleFavorite(context, creation.id)
        val message = if (isFavorite) "Added to favorites" else "Removed from favorites"
        runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        return isFavorite
    }

    fun toggleFavorite(context: Context, item: LibraryItem): Boolean =
        when (item.source) {
            LibrarySource.Creation -> {
                val isFav = GeneratedCreationsRepository.toggleFavorite(context, item.id)
                val message = if (isFav) "Added to favorites" else "Removed from favorites"
                runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                isFav
            }
            LibrarySource.Video -> {
                val isFav = GeneratedVideosRepository.toggleFavorite(context, item.id)
                val message = if (isFav) "Added to favorites" else "Removed from favorites"
                runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                isFav
            }
            LibrarySource.Music -> {
                val music = GeneratedMusicsRepository.findById(item.id)
                if (music != null) {
                    val isFav = GeneratedMusicsRepository.toggleFavorite(context, music)
                    val message = if (isFav) "Added to favorites" else "Removed from favorites"
                    runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                    isFav
                } else false
            }
            LibrarySource.Cover -> {
                val cover = GeneratedCoversRepository.findById(item.id)
                if (cover != null) {
                    val isFav = GeneratedCoversRepository.toggleFavorite(context, cover)
                    val message = if (isFav) "Added to favorites" else "Removed from favorites"
                    runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
                    isFav
                } else false
            }
        }
}
