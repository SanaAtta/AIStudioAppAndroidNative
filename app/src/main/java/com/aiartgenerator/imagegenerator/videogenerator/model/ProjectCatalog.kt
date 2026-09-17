package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.compose.ui.graphics.Color

enum class ProjectType(val label: String) {
    AiImage("AI Image"),
    AiVideo("AI Video"),
    AiMusic("AI Music"),
    AiCover("AI Cover"),
    AiEdit("AI Edit"),
}

data class ProjectItem(
    val id: String,
    val title: String,
    val type: ProjectType,
    val color: Color,
)

object ProjectCatalog {
    val filterTypes = listOf(
        ProjectType.AiImage,
        ProjectType.AiVideo,
        ProjectType.AiMusic,
        ProjectType.AiCover,
        ProjectType.AiEdit,
    )

    val projects = listOf(
        ProjectItem("p1", "Tiny Hands", ProjectType.AiImage, Color(0xFF5D4037)),
        ProjectItem("p2", "Angry Birds", ProjectType.AiImage, Color(0xFFC62828)),
        ProjectItem("p3", "Neon Sky", ProjectType.AiImage, Color(0xFF283593)),
        ProjectItem("p4", "Forest Path", ProjectType.AiImage, Color(0xFF2E7D32)),
        ProjectItem("p5", "City Pulse", ProjectType.AiVideo, Color(0xFF1565C0)),
        ProjectItem("p6", "Sunset Reel", ProjectType.AiVideo, Color(0xFFEF6C00)),
        ProjectItem("p7", "Lo-fi Beat", ProjectType.AiMusic, Color(0xFF6A1B9A)),
        ProjectItem("p8", "Epic Score", ProjectType.AiMusic, Color(0xFF00838F)),
        ProjectItem("p9", "Robot Cover", ProjectType.AiCover, Color(0xFF455A64)),
        ProjectItem("p10", "Opera Night", ProjectType.AiCover, Color(0xFF4A148C)),
        ProjectItem("p11", "Moody Edit", ProjectType.AiEdit, Color(0xFF37474F)),
        ProjectItem("p12", "Enhance Shot", ProjectType.AiEdit, Color(0xFF546E7A)),
    )
}
