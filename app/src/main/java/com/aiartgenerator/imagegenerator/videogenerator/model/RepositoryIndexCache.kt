package com.aiartgenerator.imagegenerator.videogenerator.model

import java.io.File

internal object RepositoryIndexCache {
    private val lastModifiedByPath = mutableMapOf<String, Long>()

    fun shouldReload(indexFile: File, hasItems: Boolean): Boolean {
        if (!hasItems) return true
        if (!indexFile.exists()) return true
        val path = indexFile.absolutePath
        val modified = indexFile.lastModified()
        val cached = lastModifiedByPath[path]
        if (cached == modified) return false
        lastModifiedByPath[path] = modified
        return true
    }

    fun invalidate(indexFile: File) {
        lastModifiedByPath.remove(indexFile.absolutePath)
    }
}
