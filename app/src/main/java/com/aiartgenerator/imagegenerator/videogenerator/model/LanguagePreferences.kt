package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context

object LanguagePreferences {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_COMPLETED = "language_completed"
    private const val KEY_SELECTED = "selected_language"

    fun isCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }

    fun setCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .commit()
    }

    fun getSelectedLanguageId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED, LanguageCatalog.DEFAULT_ID)
            ?: LanguageCatalog.DEFAULT_ID
    }

    fun setSelectedLanguageId(context: Context, languageId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED, languageId)
            .commit()
    }
}
