package com.aiartgenerator.imagegenerator.videogenerator.model

import android.net.Uri

object GeneratedImageStore {
    data class Payload(
        val bytes: ByteArray?,
        val uri: Uri?,
        val prompt: String,
        val styleTitle: String,
        val aspectLabel: String,
        val width: Int,
        val height: Int,
        val creationId: String? = null,
    )

    var current: Payload? = null

    fun clear() {
        current = null
    }
}
