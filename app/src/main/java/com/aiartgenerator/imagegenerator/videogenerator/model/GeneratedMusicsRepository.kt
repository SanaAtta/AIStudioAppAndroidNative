package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object GeneratedMusicsRepository {
    private const val DIR_NAME = "music"
    private const val INDEX_FILE = "index.json"
    private const val MAX_ITEMS = 30

    private val _items = mutableStateOf<List<GeneratedMusic>>(emptyList())
    val items: List<GeneratedMusic> get() = _items.value

    fun observeItems(): androidx.compose.runtime.State<List<GeneratedMusic>> = _items

    fun load(context: Context) {
        val indexFile = File(context.filesDir, "$DIR_NAME/$INDEX_FILE")
        if (!RepositoryIndexCache.shouldReload(indexFile, _items.value.isNotEmpty())) return
        _items.value = readIndex(context)
    }

    fun recent(limit: Int = 5): List<GeneratedMusic> =
        _items.value.sortedByDescending { it.createdAt }.take(limit)

    fun add(
        context: Context,
        bytes: ByteArray,
        prompt: String,
        moodLabel: String,
        genreLabel: String,
        durationSeconds: Int,
        voiceStyleLabel: String,
    ): GeneratedMusic {
        val id = System.currentTimeMillis().toString()
        val fileName = "$id.mp3"
        val file = musicFile(context, fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)

        val music = GeneratedMusic(
            id = id,
            fileName = fileName,
            title = MusicCatalog.titleFromPrompt(prompt),
            prompt = prompt,
            moodLabel = moodLabel,
            genreLabel = genreLabel,
            durationSeconds = durationSeconds,
            voiceStyleLabel = voiceStyleLabel,
            createdAt = id.toLongOrNull() ?: System.currentTimeMillis(),
        )
        val updated = (listOf(music) + _items.value)
            .sortedByDescending { it.createdAt }
            .take(MAX_ITEMS)
        _items.value = updated
        writeIndex(context, updated)
        return music
    }

    fun findById(id: String): GeneratedMusic? =
        _items.value.firstOrNull { it.id == id }

    fun toggleFavorite(context: Context, music: GeneratedMusic): Boolean {
        val updatedFavorite = !music.isFavorite
        val updated = _items.value.map { item ->
            if (item.id == music.id) item.copy(isFavorite = updatedFavorite) else item
        }
        _items.value = updated
        writeIndex(context, updated)
        return updatedFavorite
    }

    fun getFile(context: Context, music: GeneratedMusic): File =
        musicFile(context, music.fileName)

    fun getUri(context: Context, music: GeneratedMusic): Uri? {
        val file = getFile(context, music)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun loadIntoStore(context: Context, music: GeneratedMusic) {
        GeneratedMusicStore.current = GeneratedMusicStore.Payload(
            uri = getUri(context, music),
            title = music.title,
            prompt = music.prompt,
            moodLabel = music.moodLabel,
            genreLabel = music.genreLabel,
            durationSeconds = music.durationSeconds,
            voiceStyleLabel = music.voiceStyleLabel,
            creationId = music.id,
            isFavorite = music.isFavorite,
        )
    }

    private fun musicDir(context: Context): File = File(context.filesDir, DIR_NAME)

    private fun musicFile(context: Context, fileName: String): File =
        File(musicDir(context), fileName)

    private fun indexFile(context: Context): File =
        File(musicDir(context), INDEX_FILE)

    private fun readIndex(context: Context): List<GeneratedMusic> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toMusic())
                }
            }
        }.getOrDefault(emptyList())
            .filter { musicFile(context, it.fileName).exists() }
            .sortedByDescending { it.createdAt }
    }

    private fun writeIndex(context: Context, items: List<GeneratedMusic>) {
        musicDir(context).mkdirs()
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        indexFile(context).writeText(array.toString())
    }

    private fun GeneratedMusic.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fileName", fileName)
        put("title", title)
        put("prompt", prompt)
        put("moodLabel", moodLabel)
        put("genreLabel", genreLabel)
        put("durationSeconds", durationSeconds)
        put("voiceStyleLabel", voiceStyleLabel)
        put("createdAt", createdAt)
        put("isFavorite", isFavorite)
    }

    private fun JSONObject.toMusic(): GeneratedMusic = GeneratedMusic(
        id = getString("id"),
        fileName = getString("fileName"),
        title = getString("title"),
        prompt = getString("prompt"),
        moodLabel = getString("moodLabel"),
        genreLabel = getString("genreLabel"),
        durationSeconds = getInt("durationSeconds"),
        voiceStyleLabel = getString("voiceStyleLabel"),
        createdAt = getLong("createdAt"),
        isFavorite = optBoolean("isFavorite", false),
    )
}
