package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryFilter

class LibraryController {
    var selectedFilter by mutableStateOf(LibraryFilter.All)
        private set

    val filters: List<LibraryFilter> = LibraryFilter.entries

    fun onFilterSelected(filter: LibraryFilter) {
        selectedFilter = filter
    }
}
