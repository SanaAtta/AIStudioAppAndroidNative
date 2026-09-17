package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.content.Context

internal object GdprConsentPrefs {
    private const val PREFS_NAME = "gdpr_consent"
    private const val KEY_IS_GDPR = "is_gdpr"

    fun setGdprCompleted(context: Context, completed: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_GDPR, completed)
            .apply()
    }

    fun isGdprCompleted(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_GDPR, false)
}
