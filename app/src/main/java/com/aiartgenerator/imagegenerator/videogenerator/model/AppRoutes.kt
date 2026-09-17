package com.aiartgenerator.imagegenerator.videogenerator.model

object AppRoutes {
    const val Home = "home"
    const val AiWallpapers = "ai_wallpapers"
    const val AiEdit = "ai_edit"

    fun aiEdit(pickOnLaunch: Boolean = false): String = AiEdit
    const val AiEditResult = "ai_edit_result"
    const val AiCover = "ai_cover"
    const val CoverNowPlaying = "cover_now_playing"
    const val Projects = "projects"
    const val AiImageGenerator = "ai_image_generator"
    const val Generating = "generating/{type}"

    fun generating(type: String) = "generating/$type"
    const val ImageGenerationResult = "image_generation_result"
    const val AiVideoGenerator = "ai_video_generator"
    const val VideoGenerationResult = "video_generation_result"
    const val AiMusicGenerator = "ai_music_generator"
    const val MusicNowPlaying = "music_now_playing"
    const val AiChatbot = "ai_chatbot"
    const val Premium = "premium"
    const val Settings = "settings"
    const val SettingsLanguage = "settings_language"
    const val Favourites = "favourites"
    const val WallpaperCategories = "wallpaper_categories"
    const val WallpaperCategoryDetail = "wallpaper_category/{categoryId}"
    const val WallpaperDetail = "wallpaper_detail/{wallpaperId}"

    fun categoryDetail(categoryId: String) = "wallpaper_category/$categoryId"
    fun wallpaperDetail(wallpaperId: String) = "wallpaper_detail/$wallpaperId"

    val bottomNavRoutes = setOf(
        Home,
        AiWallpapers,
        AiCover,
        Projects,
    )
}
