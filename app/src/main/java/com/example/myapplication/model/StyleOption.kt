package com.example.myapplication.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class StyleOption(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val tileColor: Color,
)

object StyleCatalog {
    val imageStyles = listOf(
        StyleOption("none", "No Style", Icons.Filled.Block, Color(0xFF111111)),
        StyleOption("realistic", "Realistic", Icons.Filled.Person, Color(0xFF4A5568)),
        StyleOption("cartoon", "Cartoon", Icons.Filled.Person, Color(0xFF7E57C2)),
        StyleOption("anime", "Anime", Icons.Filled.Person, Color(0xFFE91E63)),
        StyleOption("oil", "Oil Paint", Icons.Filled.AutoAwesome, Color(0xFF8D6E63)),
        StyleOption("cyber", "Cyberpunk", Icons.Filled.AutoAwesome, Color(0xFF00838F)),
    )

    val editStyles = listOf("Auto", "Enhance", "Realistic", "Moody")
}
