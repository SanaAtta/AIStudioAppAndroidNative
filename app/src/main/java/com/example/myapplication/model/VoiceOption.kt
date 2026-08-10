package com.example.myapplication.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class VoiceOption(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val tileColor: Color,
)

object VoiceCatalog {
    val voices = listOf(
        VoiceOption("no_voice", "No Voice", Icons.Filled.Block, Color(0xFF111111)),
        VoiceOption("boy", "Boy", Icons.Filled.Person, Color(0xFF3D5A80)),
        VoiceOption("girl", "Girl", Icons.Filled.Person, Color(0xFF9B59B6)),
        VoiceOption("violin", "Violin", Icons.Filled.MusicNote, Color(0xFF8B4513)),
        VoiceOption("robot", "Robot", Icons.Filled.SmartToy, Color(0xFF607D8B)),
        VoiceOption("zombie", "Zombie", Icons.Filled.Person, Color(0xFF4A7C59)),
        VoiceOption("opera", "Opera", Icons.Filled.Mic, Color(0xFF6A1B9A)),
        VoiceOption("rap", "Rap", Icons.Filled.Mic, Color(0xFF1565C0)),
        VoiceOption("choir", "Choir", Icons.Filled.MusicNote, Color(0xFF00897B)),
    )
}
