package com.example.myapplication.controller

import com.example.myapplication.model.WallpaperCatalog
import com.example.myapplication.model.WallpaperCategory
import com.example.myapplication.model.WallpaperItem

class AiWallpaperController {
    val categories: List<WallpaperCategory> get() = WallpaperCatalog.categories
    val trending: List<WallpaperItem> get() = WallpaperCatalog.trending

    val visibleCategories: List<WallpaperCategory>
        get() = categories.take(3)
}
