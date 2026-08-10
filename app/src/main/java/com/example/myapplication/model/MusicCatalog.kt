package com.example.myapplication.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class MusicGenre(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val tileColor: Color,
)

data class VocalOption(
    val id: String,
    val shortLabel: String,
    val fullLabel: String,
)

object MusicCatalog {
    val genres = listOf(
        MusicGenre("none", "No Genre", Icons.Filled.Block, Color(0xFF111111)),
        MusicGenre("rock", "Rock", Icons.Filled.MusicNote, Color(0xFF5D4037)),
        MusicGenre("pop", "Pop", Icons.Filled.MusicNote, Color(0xFFAD1457)),
        MusicGenre("rap", "Rap", Icons.Filled.MusicNote, Color(0xFF1565C0)),
        MusicGenre("jazz", "Jazz", Icons.Filled.MusicNote, Color(0xFF6A1B9A)),
        MusicGenre("edm", "EDM", Icons.Filled.MusicNote, Color(0xFF00838F)),
    )

    val vocals = listOf(
        VocalOption("instrumental", "Ins", "Instrument"),
        VocalOption("female", "F", "Female"),
        VocalOption("male", "M", "Male"),
        VocalOption("random", "Rnd", "Random"),
    )
}
