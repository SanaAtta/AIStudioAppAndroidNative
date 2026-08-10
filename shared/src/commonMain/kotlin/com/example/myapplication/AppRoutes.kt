package com.example.myapplication

object AppRoutes {
    const val Home = "home"
    const val AiWallpapers = "ai_wallpapers"
    const val AiEdit = "ai_edit"
    const val AiCover = "ai_cover"
    const val Projects = "projects"
    const val AiImageGenerator = "ai_image_generator"
    const val AiVideoGenerator = "ai_video_generator"
    const val AiMusicGenerator = "ai_music_generator"
    const val AiChatbot = "ai_chatbot"

    val bottomNavRoutes = setOf(
        Home,
        AiWallpapers,
        AiEdit,
        AiCover,
        Projects,
    )
}
