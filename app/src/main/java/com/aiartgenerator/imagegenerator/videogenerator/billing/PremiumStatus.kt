package com.aiartgenerator.imagegenerator.videogenerator.billing

import android.content.Context

object PremiumStatus {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"

    fun isPremium(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setPremium(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_PREMIUM, enabled)
            .apply()
    }
}
