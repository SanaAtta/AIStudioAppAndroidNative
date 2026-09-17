package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object GeneratedVideosRepository {
    private const val DIR_NAME = "videos"
    private const val INDEX_FILE = "index.json"
    private const val MAX_ITEMS = 30

    private val _items = mutableStateOf<List<GeneratedVideo>>(emptyList())
    val items: List<GeneratedVideo> get() = _items.value

    fun observeItems(): androidx.compose.runtime.State<List<GeneratedVideo>> = _items

    fun load(context: Context) {
        val indexFile = File(context.filesDir, "$DIR_NAME/$INDEX_FILE")
        if (!RepositoryIndexCache.shouldReload(indexFile, _items.value.isNotEmpty())) return
        _items.value = readIndex(context)
    }

    fun recent(limit: Int = 5): List<GeneratedVideo> =
        _items.value.sortedByDescending { it.createdAt }.take(limit)

    fun add(
        context: Context,
        bytes: ByteArray,
        prompt: String,
        styleTitle: String,
        durationSeconds: Int,
        cameraMotion: String,
        aspectLabel: String,
    ): GeneratedVideo {
        val id = System.currentTimeMillis().toString()
        val fileName = "$id.mp4"
        val file = videoFile(context, fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)

        val title = prompt.take(40).ifBlank { "AI Video" }
        val video = GeneratedVideo(
            id = id,
            fileName = fileName,
            title = title,
            prompt = prompt,
            styleTitle = styleTitle,
            durationSeconds = durationSeconds,
            cameraMotion = cameraMotion,
            aspectLabel = aspectLabel,
            createdAt = id.toLongOrNull() ?: System.currentTimeMillis(),
        )
        val updated = (listOf(video) + _items.value)
            .sortedByDescending { it.createdAt }
            .take(MAX_ITEMS)
        _items.value = updated
        writeIndex(context, updated)
        return video
    }

    fun findById(id: String): GeneratedVideo? =
        _items.value.firstOrNull { it.id == id }

    fun toggleFavorite(context: Context, id: String): Boolean {
        var newValue = false
        val updated = _items.value.map { item ->
            if (item.id == id) {
                newValue = !item.isFavorite
                item.copy(isFavorite = newValue)
            } else {
                item
            }
        }
        _items.value = updated
        writeIndex(context, updated)
        return newValue
    }

    fun getFile(context: Context, video: GeneratedVideo): File =
        videoFile(context, video.fileName)

    fun getUri(context: Context, video: GeneratedVideo): Uri? {
        val file = getFile(context, video)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.getOrNull()
    }

    fun loadIntoStore(context: Context, video: GeneratedVideo) {
        GeneratedVideoStore.current = GeneratedVideoStore.Payload(
            uri = getUri(context, video),
            prompt = video.prompt,
            styleTitle = video.styleTitle,
            durationSeconds = video.durationSeconds,
            cameraMotion = video.cameraMotion,
            aspectLabel = video.aspectLabel,
            creationId = video.id,
        )
    }

    private fun videosDir(context: Context): File = File(context.filesDir, DIR_NAME)

    private fun videoFile(context: Context, fileName: String): File =
        File(videosDir(context), fileName)

    private fun indexFile(context: Context): File =
        File(videosDir(context), INDEX_FILE)

    private fun readIndex(context: Context): List<GeneratedVideo> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toVideo())
                }
            }
        }.getOrDefault(emptyList())
            .filter { videoFile(context, it.fileName).exists() }
            .sortedByDescending { it.createdAt }
    }

    private fun writeIndex(context: Context, items: List<GeneratedVideo>) {
        videosDir(context).mkdirs()
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        val text = array.toString() ?: "[]"
        indexFile(context).writeText(text)
    }

    private fun GeneratedVideo.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fileName", fileName)
        put("title", title)
        put("prompt", prompt)
        put("styleTitle", styleTitle)
        put("durationSeconds", durationSeconds)
        put("cameraMotion", cameraMotion)
        put("aspectLabel", aspectLabel)
        put("createdAt", createdAt)
        put("isFavorite", isFavorite)
    }

    private fun JSONObject.toVideo(): GeneratedVideo = GeneratedVideo(
        id = getString("id"),
        fileName = getString("fileName"),
        title = getString("title"),
        prompt = getString("prompt"),
        styleTitle = getString("styleTitle"),
        durationSeconds = getInt("durationSeconds"),
        cameraMotion = getString("cameraMotion"),
        aspectLabel = getString("aspectLabel"),
        createdAt = getLong("createdAt"),
        isFavorite = optBoolean("isFavorite", false),
    )
}
