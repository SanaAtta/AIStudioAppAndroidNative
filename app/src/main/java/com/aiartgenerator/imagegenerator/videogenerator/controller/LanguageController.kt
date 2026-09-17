package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguageCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguageOption

class LanguageController(
    initialSelectedId: String = LanguageCatalog.DEFAULT_ID,
) {
    var selectedLanguageId by mutableStateOf(initialSelectedId)
        private set

    val languages: List<LanguageOption> = LanguageCatalog.languages

    fun selectLanguage(languageId: String) {
        selectedLanguageId = languageId
    }

    fun selectedLanguage(): LanguageOption {
        return LanguageCatalog.findById(selectedLanguageId)
            ?: languages.first { it.id == LanguageCatalog.DEFAULT_ID }
    }
}
