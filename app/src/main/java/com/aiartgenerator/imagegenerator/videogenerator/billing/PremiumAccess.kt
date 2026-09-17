package com.aiartgenerator.imagegenerator.videogenerator.billing

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.FeatureInterstitial
import com.aiartgenerator.imagegenerator.videogenerator.ads.GenerateProgressRewardedAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Premium gate used by ads and locked features.
 *
 * [PremiumStatus] is a local cache for instant UI before Play Billing answers.
 * [applySubscriptionActive] is the only writer and must be driven from store purchases.
 */
object PremiumAccess {
    private lateinit var appContext: Context
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUserFlow: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        _isPremiumUser.value = PremiumStatus.isPremium(appContext)
    }

    fun isPremiumUser(): Boolean = _isPremiumUser.value

    fun shouldShowAds(): Boolean = !isPremiumUser()

    fun applySubscriptionActive(active: Boolean) {
        val changed = _isPremiumUser.value != active
        _isPremiumUser.value = active
        if (::appContext.isInitialized) {
            PremiumStatus.setPremium(appContext, active)
        }
        if (!changed) return
        val onMain = {
            AdsConfigRevision.bump()
            if (active) {
                // Drop any ads already loaded for free users so they cannot show after unlock.
                FeatureInterstitial.clearPreload()
                GenerateProgressRewardedAds.clearPreload()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onMain()
        } else {
            mainHandler.post(onMain)
        }
    }
}
