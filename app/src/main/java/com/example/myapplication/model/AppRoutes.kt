package com.example.myapplication.model

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
    const val Premium = "premium"
    const val WallpaperCategories = "wallpaper_categories"
    const val WallpaperCategoryDetail = "wallpaper_category/{categoryId}"
    const val WallpaperDetail = "wallpaper_detail/{wallpaperId}"

    fun categoryDetail(categoryId: String) = "wallpaper_category/$categoryId"
    fun wallpaperDetail(wallpaperId: String) = "wallpaper_detail/$wallpaperId"

    val bottomNavRoutes = setOf(
        Home,
        AiWallpapers,
        AiEdit,
        AiCover,
        Projects,
    )
}
