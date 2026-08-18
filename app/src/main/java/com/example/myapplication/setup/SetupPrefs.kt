package com.example.myapplication.setup

import android.content.Context

object SetupPrefs {
    private const val PREFS = "ai_studio_setup"
    private const val KEY_LANGUAGE_DONE = "language_done"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isLanguageDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LANGUAGE_DONE, false)

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun markLanguageDone(context: Context, languageTag: String) {
        prefs(context).edit()
            .putBoolean(KEY_LANGUAGE_DONE, true)
            .putString(KEY_LANGUAGE_TAG, languageTag)
            .apply()
    }

    fun markOnboardingDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun needsSetup(context: Context): Boolean =
        !isLanguageDone(context) || !isOnboardingDone(context)
}
