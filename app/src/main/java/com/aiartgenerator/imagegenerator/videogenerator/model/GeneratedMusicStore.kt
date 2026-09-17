package com.aiartgenerator.imagegenerator.videogenerator.model

import android.net.Uri

data class GeneratedMusic(
    val id: String,
    val fileName: String,
    val title: String,
    val prompt: String,
    val moodLabel: String,
    val genreLabel: String,
    val durationSeconds: Int,
    val voiceStyleLabel: String,
    val createdAt: Long,
    val isFavorite: Boolean = false,
)

object GeneratedMusicStore {
    data class Payload(
        val uri: Uri?,
        val title: String,
        val prompt: String,
        val moodLabel: String,
        val genreLabel: String,
        val durationSeconds: Int,
        val voiceStyleLabel: String,
        val creationId: String? = null,
        val isFavorite: Boolean = false,
    )

    var current: Payload? = null

    fun clear() {
        current = null
    }
}
