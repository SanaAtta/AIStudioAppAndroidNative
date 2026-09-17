package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.compose.ui.graphics.Color
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi

data class WallpaperCategory(
    val id: String,
    val title: String,
    val color: Color,
)

data class WallpaperFilterChip(
    val id: String,
    val label: String,
)

data class WallpaperItem(
    val id: String,
    val title: String,
    val categoryId: String,
    val color: Color,
    val prompt: String,
) {
    fun imageUrl(
        api: AiApi = AiApi(),
        width: Int = 1080,
        height: Int = 1920,
    ): String = api.imageUrl(prompt = prompt, width = width, height = height)
}

enum class WallpaperApplyTarget(val label: String) {
    Home("Home Screen"),
    Lock("Lock Screen"),
    Both("Home & Lock Screen"),
}

object WallpaperCatalog {
    val filterChips = listOf(
        WallpaperFilterChip("all", "All"),
        WallpaperFilterChip("nature", "Nature"),
        WallpaperFilterChip("space", "Space"),
        WallpaperFilterChip("anime", "Anime"),
        WallpaperFilterChip("cars", "Cars"),
    )

    val categories = listOf(
        WallpaperCategory("nature", "Nature", Color(0xFF1B5E4A)),
        WallpaperCategory("3d", "3D", Color(0xFF8B4513)),
        WallpaperCategory("anime", "Anime", Color(0xFF5C6BC0)),
        WallpaperCategory("love", "Love", Color(0xFFAD1457)),
        WallpaperCategory("christmas", "Christmas", Color(0xFF1B5E20)),
        WallpaperCategory("cars", "Cars", Color(0xFF37474F)),
        WallpaperCategory("space", "Space", Color(0xFF0D47A1)),
        WallpaperCategory("abstract", "Abstract", Color(0xFF6A1B9A)),
    )

    val allWallpapers = listOf(
        WallpaperItem("n1", "Palm Reflection", "nature", Color(0xFF00695C), "tropical palm trees reflected in clear turquoise water, mobile wallpaper"),
        WallpaperItem("n2", "Misty Peak", "nature", Color(0xFF2E7D32), "misty mountain peak at dawn, cinematic nature wallpaper"),
        WallpaperItem("n3", "Ocean Glow", "nature", Color(0xFF0277BD), "glowing ocean waves at sunset, vertical wallpaper"),
        WallpaperItem("n4", "Desert Dune", "nature", Color(0xFFEF6C00), "golden desert dunes under dramatic sky, phone wallpaper"),
        WallpaperItem("d1", "Gold Leaf", "3d", Color(0xFFBF360C), "3d rendered golden leaves on dark geometric background"),
        WallpaperItem("d2", "Crystal Orb", "3d", Color(0xFF6A1B9A), "3d crystal orb with soft reflections, dark background"),
        WallpaperItem("d3", "Neon Geometry", "3d", Color(0xFF1565C0), "3d neon geometric shapes, cyber aesthetic wallpaper"),
        WallpaperItem("d4", "Soft Render", "3d", Color(0xFF455A64), "soft pastel 3d abstract render, clean wallpaper"),
        WallpaperItem("a1", "Lantern Girl", "anime", Color(0xFF5C6BC0), "anime girl with lanterns in magical forest, detailed illustration"),
        WallpaperItem("a2", "Sakura Path", "anime", Color(0xFFEC407A), "anime sakura path in spring, soft lighting wallpaper"),
        WallpaperItem("a3", "Night Fox", "anime", Color(0xFF3949AB), "anime night fox under moonlight, vivid colors"),
        WallpaperItem("a4", "Sky Temple", "anime", Color(0xFF7E57C2), "anime floating sky temple above clouds"),
        WallpaperItem("l1", "Heart Tree", "love", Color(0xFFC2185B), "glowing pink heart shaped tree in field at dusk"),
        WallpaperItem("l2", "Soft Blush", "love", Color(0xFFE91E63), "soft romantic blush aesthetic wallpaper"),
        WallpaperItem("l3", "Rose Glow", "love", Color(0xFFAD1457), "romantic roses with soft neon glow"),
        WallpaperItem("l4", "Twilight Love", "love", Color(0xFF880E4F), "couple silhouette at twilight romantic wallpaper"),
        WallpaperItem("c1", "Snowy Tree", "christmas", Color(0xFF1B5E20), "bright christmas tree in snowy forest at night"),
        WallpaperItem("c2", "Winter Lights", "christmas", Color(0xFF0D47A1), "winter holiday lights, cozy christmas wallpaper"),
        WallpaperItem("c3", "Cozy Cabin", "christmas", Color(0xFF4E342E), "cozy snow cabin with warm christmas lights"),
        WallpaperItem("c4", "Frost Night", "christmas", Color(0xFF263238), "frosty christmas night sky with snowflakes"),
        WallpaperItem("car1", "Neon Street", "cars", Color(0xFF212121), "futuristic sports car on neon lit city street at night"),
        WallpaperItem("car2", "Midnight Drive", "cars", Color(0xFF37474F), "luxury car midnight drive, cinematic wallpaper"),
        WallpaperItem("car3", "Red Flash", "cars", Color(0xFFB71C1C), "red sports car motion blur, dark background"),
        WallpaperItem("car4", "City Cruise", "cars", Color(0xFF455A64), "sleek car cruising through rainy city lights"),
        WallpaperItem("s1", "Galaxy Edge", "space", Color(0xFF0D47A1), "deep space galaxy edge, stars and nebula wallpaper"),
        WallpaperItem("s2", "Nebula Drift", "space", Color(0xFF4A148C), "colorful nebula drift in outer space"),
        WallpaperItem("s3", "Moon Dust", "space", Color(0xFF1A237E), "astronaut moon dust scene, dark space wallpaper"),
        WallpaperItem("s4", "Star Trail", "space", Color(0xFF311B92), "star trails over cosmic landscape"),
        WallpaperItem("ab1", "Fluid Art", "abstract", Color(0xFF6A1B9A), "abstract fluid art colorful swirls wallpaper"),
        WallpaperItem("ab2", "Color Wave", "abstract", Color(0xFF00838F), "abstract color wave gradient wallpaper"),
        WallpaperItem("ab3", "Glitch Mesh", "abstract", Color(0xFF4527A0), "abstract glitch mesh neon digital art"),
        WallpaperItem("ab4", "Ink Bloom", "abstract", Color(0xFFAD1457), "abstract ink bloom on dark canvas"),
    )

    val trending: List<WallpaperItem>
        get() = allWallpapers.take(9)

    fun categoryById(id: String): WallpaperCategory? =
        categories.firstOrNull { it.id == id }

    fun wallpaperById(id: String): WallpaperItem? =
        allWallpapers.firstOrNull { it.id == id }

    fun wallpapersByCategory(categoryId: String): List<WallpaperItem> =
        allWallpapers.filter { it.categoryId == categoryId }
}
