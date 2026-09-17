package com.aiartgenerator.imagegenerator.videogenerator.controller

import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperCategory
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperItem

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
