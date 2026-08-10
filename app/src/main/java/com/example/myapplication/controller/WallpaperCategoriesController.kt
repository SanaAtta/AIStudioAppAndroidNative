package com.example.myapplication.controller

import com.example.myapplication.model.WallpaperCatalog
import com.example.myapplication.model.WallpaperCategory
import com.example.myapplication.model.WallpaperItem

class WallpaperCategoriesController {
    val categories: List<WallpaperCategory> get() = WallpaperCatalog.categories
}

class WallpaperCategoryDetailController(
    private val categoryId: String,
) {
    val category: WallpaperCategory? = WallpaperCatalog.categoryById(categoryId)
    val wallpapers: List<WallpaperItem> =
        WallpaperCatalog.wallpapersByCategory(categoryId)
}
