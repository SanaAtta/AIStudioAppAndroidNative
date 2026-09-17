package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.aiartgenerator.imagegenerator.videogenerator.R

data class StyleOption(
    val id: String,
    val title: String,
    @DrawableRes val previewRes: Int,
    val tileColor: Color,
    val previewEndColor: Color,
)

object StyleCatalog {
    val imageStyles = listOf(
        StyleOption(
            id = "realistic",
            title = "Realistic",
            previewRes = R.drawable.ic_realistic,
            tileColor = Color(0xFF4A2030),
            previewEndColor = Color(0xFF1A1018),
        ),
        StyleOption(
            id = "anime",
            title = "Anime",
            previewRes = R.drawable.ic_anime,
            tileColor = Color(0xFF3D8BD9),
            previewEndColor = Color(0xFF1B4F8A),
        ),
        StyleOption(
            id = "oil",
            title = "Oil Paint",
            previewRes = R.drawable.ic_paint,
            tileColor = Color(0xFFE07A45),
            previewEndColor = Color(0xFF8B3D67),
        ),
        StyleOption(
            id = "cartoon",
            title = "Cartoon",
            previewRes = R.drawable.ic_minimal,
            tileColor = Color(0xFF7E57C2),
            previewEndColor = Color(0xFF4527A0),
        ),
        StyleOption(
            id = "cyber",
            title = "Cyberpunk",
            previewRes = R.drawable.ic_cyberpunk,
            tileColor = Color(0xFF00838F),
            previewEndColor = Color(0xFF004D57),
        ),
    )

    fun findById(id: String): StyleOption? = imageStyles.find { it.id == id }

    val editStyles = listOf("Auto", "Enhance", "Realistic", "Moody")
}
