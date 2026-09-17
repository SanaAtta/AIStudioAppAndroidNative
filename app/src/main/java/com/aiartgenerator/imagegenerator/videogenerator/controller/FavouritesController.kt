package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.FavouritesFilter

class FavouritesController {
    var selectedFilter by mutableStateOf(FavouritesFilter.All)
        private set

    val filters: List<FavouritesFilter> = FavouritesFilter.entries

    fun onFilterSelected(filter: FavouritesFilter) {
        selectedFilter = filter
    }
}
