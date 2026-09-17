package com.aiartgenerator.imagegenerator.videogenerator.model

data class GeneratedCreation(
    val id: String,
    val fileName: String,
    val prompt: String,
    val styleTitle: String,
    val aspectLabel: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
    val isFavorite: Boolean = false,
)
