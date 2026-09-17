package com.aiartgenerator.imagegenerator.videogenerator.model

import android.net.Uri

data class GeneratedCover(
    val id: String,
    val fileName: String,
    val title: String,
    val songName: String,
    val singerStyleLabel: String,
    val voiceStyleLabel: String,
    val durationSeconds: Int,
    val createdAt: Long,
    val isFavorite: Boolean = false,
)

object GeneratedCoverStore {
    data class Payload(
        val uri: Uri?,
        val title: String,
        val songName: String,
        val singerStyleLabel: String,
        val voiceStyleLabel: String,
        val durationSeconds: Int,
        val creationId: String? = null,
        val isFavorite: Boolean = false,
    )

    var current: Payload? = null

    fun clear() {
        current = null
    }
}
