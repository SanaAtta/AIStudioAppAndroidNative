package com.example.myapplication.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

object AndroidFileHelper {
    fun readBytes(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Unable to read selected file")
    }

    fun saveToCache(context: Context, bytes: ByteArray, fileName: String): File {
        val dir = File(context.cacheDir, "generated").apply { mkdirs() }
        return File(dir, fileName).also { file ->
            file.outputStream().use { it.write(bytes) }
        }
    }
}
