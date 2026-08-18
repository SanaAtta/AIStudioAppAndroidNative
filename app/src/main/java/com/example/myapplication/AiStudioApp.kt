package com.example.myapplication

import android.app.Application
import com.example.myapplication.ads.AdsConsentManager
import com.example.myapplication.ads.AdsRemoteConfig
import com.example.myapplication.billing.PremiumAccess
import com.example.myapplication.util.FirebaseBootstrap
import kotlin.concurrent.thread

class AiStudioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PremiumAccess.init(this)
        AdsConsentManager.init(this)
        AdsRemoteConfig.ensureLocalDefaultsApplied()
        FirebaseBootstrap.ensureStarted(this)
        thread(name = "ads-consent-init", isDaemon = true) {
            AdsConsentManager.completeHeavyInit()
        }
    }
}
