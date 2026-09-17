package com.aiartgenerator.imagegenerator.videogenerator

import android.app.Application
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConsentManager
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ads.MobileAdsInitializer
import com.aiartgenerator.imagegenerator.videogenerator.ads.ResumeAppOpenCoordinator
import com.aiartgenerator.imagegenerator.videogenerator.analytics.AppAnalytics
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.billing.SubscriptionSync
import com.aiartgenerator.imagegenerator.videogenerator.controller.PromptSafety
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiKeyPool
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiRemoteConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AiApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        PromptSafety.init(this)
        PremiumAccess.init(this)
        FreeUsageLimits.init(this)
        SubscriptionSync.init(this)
        AdsConsentManager.init(this)
        AdUnitIds.init(this)
        AdsRemoteConfig.ensureLocalDefaultsApplied()
        ResumeAppOpenCoordinator.init(this)
        MobileAdsInitializer.start(this)
        ApiKeyPool.initialize(this)
        ApiRemoteConfig.initialize(this)
        appScope.launch {
            ApiRemoteConfig.fetchAndActivate()
        }
        AppAnalytics.init(this)
        FirebaseAnalytics.getInstance(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
    }
}
