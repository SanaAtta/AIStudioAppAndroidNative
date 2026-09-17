package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperFilterChip
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperItem

class AiWallpaperController {
    var searchQuery by mutableStateOf("")
        private set
    var selectedCategoryId by mutableStateOf("all")
        private set

    val filterChips: List<WallpaperFilterChip> = WallpaperCatalog.filterChips

    val displayedWallpapers: List<WallpaperItem>
        get() {
            var items = WallpaperCatalog.allWallpapers
            if (selectedCategoryId != "all") {
                items = items.filter { it.categoryId == selectedCategoryId }
            }
            val query = searchQuery.trim().lowercase()
            if (query.isNotEmpty()) {
                items = items.filter { item ->
                    item.title.lowercase().contains(query) ||
                        item.prompt.lowercase().contains(query) ||
                        item.categoryId.lowercase().contains(query)
                }
            }
            return items
        }

    fun onSearchChange(value: String) {
        searchQuery = value
    }

    fun onCategorySelected(categoryId: String) {
        selectedCategoryId = categoryId
    }
}
