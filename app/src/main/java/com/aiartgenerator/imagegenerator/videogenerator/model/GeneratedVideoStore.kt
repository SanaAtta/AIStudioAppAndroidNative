package com.aiartgenerator.imagegenerator.videogenerator.model

import android.net.Uri

data class GeneratedVideo(
    val id: String,
    val fileName: String,
    val title: String,
    val prompt: String,
    val styleTitle: String,
    val durationSeconds: Int,
    val cameraMotion: String,
    val aspectLabel: String,
    val createdAt: Long,
    val isFavorite: Boolean = false,
)

object GeneratedVideoStore {
    data class Payload(
        val uri: Uri?,
        val prompt: String,
        val styleTitle: String,
        val durationSeconds: Int,
        val cameraMotion: String,
        val aspectLabel: String,
        val creationId: String? = null,
    )

    var current: Payload? = null
    var editDraft: EditDraft? = null

    data class EditDraft(
        val prompt: String,
        val styleId: String,
        val durationId: String,
        val cameraMotionId: String,
        val aspectRatioId: String,
    )

    fun editDraftFrom(payload: Payload): EditDraft = EditDraft(
        prompt = payload.prompt,
        styleId = StyleCatalog.imageStyles.find { it.title == payload.styleTitle }?.id ?: "realistic",
        durationId = VideoCatalog.durations.find { it.seconds == payload.durationSeconds }?.id ?: "5",
        cameraMotionId = payload.cameraMotion.ifBlank { "zoom_in" },
        aspectRatioId = AspectRatioCatalog.options.find { it.label == payload.aspectLabel }?.id ?: "16:9",
    )

    fun clear() {
        current = null
    }
}
