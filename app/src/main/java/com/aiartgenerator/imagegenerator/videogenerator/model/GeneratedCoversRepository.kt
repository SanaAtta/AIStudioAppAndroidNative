package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object GeneratedCoversRepository {
    private const val DIR_NAME = "covers"
    private const val INDEX_FILE = "index.json"
    private const val MAX_ITEMS = 30

    private val _items = mutableStateOf<List<GeneratedCover>>(emptyList())
    val items: List<GeneratedCover> get() = _items.value

    fun observeItems(): androidx.compose.runtime.State<List<GeneratedCover>> = _items

    fun load(context: Context) {
        val indexFile = File(context.filesDir, "$DIR_NAME/$INDEX_FILE")
        if (!RepositoryIndexCache.shouldReload(indexFile, _items.value.isNotEmpty())) return
        _items.value = readIndex(context)
    }

    fun add(
        context: Context,
        bytes: ByteArray,
        songName: String,
        singerStyleLabel: String,
        voiceStyleLabel: String,
        durationSeconds: Int,
    ): GeneratedCover {
        val id = System.currentTimeMillis().toString()
        val fileName = "$id.mp3"
        val file = coverFile(context, fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)

        val cover = GeneratedCover(
            id = id,
            fileName = fileName,
            title = CoverCatalog.titleFromSongName(songName, singerStyleLabel),
            songName = songName,
            singerStyleLabel = singerStyleLabel,
            voiceStyleLabel = voiceStyleLabel,
            durationSeconds = durationSeconds,
            createdAt = id.toLongOrNull() ?: System.currentTimeMillis(),
        )
        val updated = (listOf(cover) + _items.value)
            .sortedByDescending { it.createdAt }
            .take(MAX_ITEMS)
        _items.value = updated
        writeIndex(context, updated)
        return cover
    }

    fun addPreviewEntry(
        context: Context,
        songName: String,
        singerStyleLabel: String,
        voiceStyleLabel: String,
        durationSeconds: Int,
    ): GeneratedCover {
        val id = System.currentTimeMillis().toString()
        val cover = GeneratedCover(
            id = id,
            fileName = "",
            title = CoverCatalog.titleFromSongName(songName, singerStyleLabel),
            songName = songName,
            singerStyleLabel = singerStyleLabel,
            voiceStyleLabel = voiceStyleLabel,
            durationSeconds = durationSeconds,
            createdAt = id.toLongOrNull() ?: System.currentTimeMillis(),
        )
        return cover
    }

    fun findById(id: String): GeneratedCover? =
        _items.value.firstOrNull { it.id == id }

    fun toggleFavorite(context: Context, cover: GeneratedCover): Boolean {
        val updatedFavorite = !cover.isFavorite
        val updated = _items.value.map { item ->
            if (item.id == cover.id) item.copy(isFavorite = updatedFavorite) else item
        }
        _items.value = updated
        writeIndex(context, updated)
        return updatedFavorite
    }

    fun getFile(context: Context, cover: GeneratedCover): File =
        coverFile(context, cover.fileName)

    fun getUri(context: Context, cover: GeneratedCover): Uri? {
        if (cover.fileName.isBlank()) return null
        val file = getFile(context, cover)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun loadIntoStore(context: Context, cover: GeneratedCover) {
        GeneratedCoverStore.current = GeneratedCoverStore.Payload(
            uri = getUri(context, cover),
            title = cover.title,
            songName = cover.songName,
            singerStyleLabel = cover.singerStyleLabel,
            voiceStyleLabel = cover.voiceStyleLabel,
            durationSeconds = cover.durationSeconds,
            creationId = cover.id,
            isFavorite = cover.isFavorite,
        )
    }

    private fun coversDir(context: Context): File = File(context.filesDir, DIR_NAME)

    private fun coverFile(context: Context, fileName: String): File =
        File(coversDir(context), fileName)

    private fun indexFile(context: Context): File =
        File(coversDir(context), INDEX_FILE)

    private fun readIndex(context: Context): List<GeneratedCover> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toCover())
                }
            }
        }.getOrDefault(emptyList())
            .filter { item ->
                item.fileName.isBlank() || coverFile(context, item.fileName).exists()
            }
            .sortedByDescending { it.createdAt }
    }

    private fun writeIndex(context: Context, items: List<GeneratedCover>) {
        coversDir(context).mkdirs()
        val array = JSONArray()
        items.filter { it.fileName.isNotBlank() }.forEach { array.put(it.toJson()) }
        indexFile(context).writeText(array.toString())
    }

    private fun GeneratedCover.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fileName", fileName)
        put("title", title)
        put("songName", songName)
        put("singerStyleLabel", singerStyleLabel)
        put("voiceStyleLabel", voiceStyleLabel)
        put("durationSeconds", durationSeconds)
        put("createdAt", createdAt)
        put("isFavorite", isFavorite)
    }

    private fun JSONObject.toCover(): GeneratedCover = GeneratedCover(
        id = getString("id"),
        fileName = getString("fileName"),
        title = getString("title"),
        songName = getString("songName"),
        singerStyleLabel = getString("singerStyleLabel"),
        voiceStyleLabel = getString("voiceStyleLabel"),
        durationSeconds = getInt("durationSeconds"),
        createdAt = getLong("createdAt"),
        isFavorite = optBoolean("isFavorite", false),
    )
}
