package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object GeneratedCreationsRepository {
    private const val DIR_NAME = "creations"
    private const val INDEX_FILE = "index.json"
    private const val MAX_ITEMS = 50

    private val _items = mutableStateOf<List<GeneratedCreation>>(emptyList())
    val items: List<GeneratedCreation> get() = _items.value

    fun observeItems(): androidx.compose.runtime.State<List<GeneratedCreation>> = _items

    fun load(context: Context) {
        val indexFile = File(context.filesDir, "$DIR_NAME/$INDEX_FILE")
        if (!RepositoryIndexCache.shouldReload(indexFile, _items.value.isNotEmpty())) return
        _items.value = readIndex(context)
    }

    fun recentImages(limit: Int = 4): List<GeneratedCreation> =
        _items.value
            .sortedByDescending { it.createdAt }
            .take(limit)

    fun add(
        context: Context,
        bytes: ByteArray,
        prompt: String,
        styleTitle: String,
        aspectLabel: String,
        width: Int,
        height: Int,
    ): GeneratedCreation {
        val id = System.currentTimeMillis().toString()
        val fileName = "$id.jpg"
        val file = creationFile(context, fileName)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)

        val creation = GeneratedCreation(
            id = id,
            fileName = fileName,
            prompt = prompt,
            styleTitle = styleTitle,
            aspectLabel = aspectLabel,
            width = width,
            height = height,
            createdAt = id.toLongOrNull() ?: System.currentTimeMillis(),
        )
        val updated = (listOf(creation) + _items.value)
            .sortedByDescending { it.createdAt }
            .take(MAX_ITEMS)
        _items.value = updated
        writeIndex(context, updated)
        return creation
    }

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

    fun findById(id: String): GeneratedCreation? =
        _items.value.firstOrNull { it.id == id }

    fun getFile(context: Context, creation: GeneratedCreation): File =
        creationFile(context, creation.fileName)

    fun getUri(context: Context, creation: GeneratedCreation): Uri? {
        val file = getFile(context, creation)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.getOrNull()
    }

    fun loadIntoStore(context: Context, creation: GeneratedCreation) {
        val file = getFile(context, creation)
        if (!file.exists()) return
        val bytes = file.readBytes()
        val uri = getUri(context, creation)
        GeneratedImageStore.current = GeneratedImageStore.Payload(
            bytes = bytes,
            uri = uri,
            prompt = creation.prompt,
            styleTitle = creation.styleTitle,
            aspectLabel = creation.aspectLabel,
            width = creation.width,
            height = creation.height,
            creationId = creation.id,
        )
    }

    private fun creationsDir(context: Context): File =
        File(context.filesDir, DIR_NAME)

    private fun creationFile(context: Context, fileName: String): File =
        File(creationsDir(context), fileName)

    private fun indexFile(context: Context): File =
        File(creationsDir(context), INDEX_FILE)

    private fun readIndex(context: Context): List<GeneratedCreation> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toCreation())
                }
            }
        }.getOrDefault(emptyList())
            .filter { creationFile(context, it.fileName).exists() }
            .sortedByDescending { it.createdAt }
    }

    private fun writeIndex(context: Context, items: List<GeneratedCreation>) {
        val dir = creationsDir(context)
        dir.mkdirs()
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        val text = array.toString() ?: "[]"
        indexFile(context).writeText(text)
    }

    private fun GeneratedCreation.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fileName", fileName)
        put("prompt", prompt)
        put("styleTitle", styleTitle)
        put("aspectLabel", aspectLabel)
        put("width", width)
        put("height", height)
        put("createdAt", createdAt)
        put("isFavorite", isFavorite)
    }

    private fun JSONObject.toCreation(): GeneratedCreation = GeneratedCreation(
        id = getString("id"),
        fileName = getString("fileName"),
        prompt = getString("prompt"),
        styleTitle = getString("styleTitle"),
        aspectLabel = getString("aspectLabel"),
        width = getInt("width"),
        height = getInt("height"),
        createdAt = getLong("createdAt"),
        isFavorite = optBoolean("isFavorite", false),
    )
}
