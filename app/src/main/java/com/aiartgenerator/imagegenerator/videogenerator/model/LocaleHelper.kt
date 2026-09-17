package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun applyLocale(context: Context): Context {
        val languageId = LanguagePreferences.getSelectedLanguageId(context)
        val locale = localeForLanguageId(languageId)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    /** Maps app language ids to Android resource locale tags. */
    private fun localeForLanguageId(languageId: String): Locale = when (languageId) {
        // Indonesian uses values-in in Android resources (legacy ISO code).
        "id" -> Locale.forLanguageTag("in")
        else -> Locale.forLanguageTag(languageId)
    }
}
