package com.example.myapplication.billing

import android.content.Context
import com.example.myapplication.ads.AdsConfigRevision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Premium gate stub — always show ads until billing is wired. */
object PremiumAccess {
    private const val PREFS_NAME = "premium_access"
    private const val KEY_PREMIUM_ACTIVE = "premium_active"

    private lateinit var appContext: Context
    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUserFlow: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        _isPremiumUser.value =
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PREMIUM_ACTIVE, false)
    }

    fun isPremiumUser(): Boolean = _isPremiumUser.value

    fun shouldShowAds(): Boolean = !isPremiumUser()

    fun applySubscriptionActive(active: Boolean) {
        if (!::appContext.isInitialized) return
        val changed = _isPremiumUser.value != active
        _isPremiumUser.value = active
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREMIUM_ACTIVE, active)
            .apply()
        if (changed) AdsConfigRevision.bump()
    }
}
