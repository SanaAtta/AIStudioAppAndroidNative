package com.aiartgenerator.imagegenerator.videogenerator.model

data class AspectRatioOption(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
)

object AspectRatioCatalog {
    const val DEFAULT_ID = "9:16"

    val options = listOf(
        AspectRatioOption("1:1", "1:1", 1024, 1024),
        AspectRatioOption("9:16", "9:16", 768, 1344),
        AspectRatioOption("16:9", "16:9", 1344, 768),
        AspectRatioOption("4:3", "4:3", 1024, 768),
    )

    fun findById(id: String): AspectRatioOption? = options.find { it.id == id }

    fun default(): AspectRatioOption = findById(DEFAULT_ID) ?: options.first()
}
