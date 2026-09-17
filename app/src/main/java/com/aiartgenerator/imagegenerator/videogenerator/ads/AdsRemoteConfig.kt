package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.os.Handler
import android.os.Looper
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig
import com.aiartgenerator.imagegenerator.videogenerator.util.FirebaseBootstrap
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firebase Remote Config for ads — keys **`ads_control`** (or fallback `ads_config`) and **`ad_ids`**.
 * Same JSON shape as Video Downloader. Debug uses Google test ad units via [AdUnitIds].
 */
object AdsRemoteConfig {
    private const val RC_KEY_AD_IDS = "ad_ids"
    private const val RC_KEY_PREMIUM_SCREEN_TEST = "premium_screen_test"

    /** Embedded defaults so splash ads work even before Firebase activates. */
    private val DEFAULT_ADS_CONTROL_JSON =
        """
        {
          "all_ads_On_Off": 1,
          "ads_debug_log": 1,
          "app_open_resume": 1,
          "privacy_policy_url": "https://allvideodownloaderhdvideoplayer.blogspot.com/2026/05/privacy-policy.html",
          "splash": {
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "interstitial": 0,
            "app_open_ad": 1
          },
          "language": {
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "done_interstitial": 0
          },
          "onboarding": {
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "interstitial": 0
          },
          "home": {
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 0 }
          },
          "library": {
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 }
          },
          "wallpaper": {
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 }
          },
          "image_generator": {
            "interstitial": 1,
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 }
          },
          "video_generator": {
            "interstitial": 1,
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 }
          },
          "music_generator": {
            "interstitial": 1,
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 }
          },
          "generate_result": {
            "home_interstitial": 1,
            "rewarded_ad": 1,
            "rewarded_interstitial_ad": 1,
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 }
          },
          "exit": {
            "medium_rectangle_banner": 1,
            "medium_native_ad": 0,
            "adaptive_banner_ad": 0
          },
          "settings": { "interstitial": 1 },
          "premium": { "show_after_onboarding": 1 }
        }
        """.trimIndent()


    @Volatile
    private var fetchDone = false

    @Volatile
    private var cachedAdsConfigApplied = false

    fun isAdsConfigReady(): Boolean = cachedAdsConfigApplied || fetchDone

    fun ensureLocalDefaultsApplied() {
        if (cachedAdsConfigApplied) return
        AdsControl.applyAdsControlJson(DEFAULT_ADS_CONTROL_JSON)
        cachedAdsConfigApplied = true
    }

    suspend fun refreshAdsControlForSplash() {
        refreshAdsControlFromNetwork(forceFreshFetch = true)
    }

    suspend fun refreshAdsControlForSetupFlow() {
        refreshAdsControlFromNetwork(forceFreshFetch = true)
    }

    private suspend fun refreshAdsControlFromNetwork(forceFreshFetch: Boolean) {
        ensureLocalDefaultsApplied()
        val firebaseReady =
            runCatching { FirebaseApp.getInstance(); true }.getOrDefault(false) ||
                FirebaseBootstrap.awaitReady(timeoutMs = 8_000L)
        if (!firebaseReady) {
            AdsLoadLog.trace("ads_control", "firebase not ready — keeping local defaults")
            fetchDone = true
            bumpRevision()
            return
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val rc = FirebaseRemoteConfig.getInstance()
                val interval = if (BuildConfig.DEBUG || forceFreshFetch) 0L else 300L
                rc.setDefaultsAsync(
                    mapOf(
                        AdsControl.RC_KEY_ADS_CONTROL to DEFAULT_ADS_CONTROL_JSON,
                        RC_KEY_PREMIUM_SCREEN_TEST to "",
                    ),
                ).await()
                rc.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(interval)
                        .build(),
                ).await()
                val activated = rc.fetchAndActivate().await()
                val adsBlob = AdsControl.readAdsControlBlob(rc)
                PremiumScreenTestResolver.applyToAdsControl()
                AdUnitIds.applyAdIdsJson(rc.getString(RC_KEY_AD_IDS))
                if (adsBlob.isNotEmpty()) {
                    AdsControl.applyAdsControlJson(adsBlob)
                    cachedAdsConfigApplied = true
                }
                fetchDone = true
                AdsLoadLog.trace(
                    "ads_control",
                    "fetchAndActivate activated=$activated blobLen=${adsBlob.length} " +
                        "ready=${cachedAdsConfigApplied}",
                )
            }.onFailure {
                AdsLoadLog.trace("ads_control", "fetch failed: ${it.message}")
                fetchDone = true
            }
        }
        bumpRevision()
    }

    private fun bumpRevision() {
        val bump = Runnable { AdsConfigRevision.bump() }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            bump.run()
        } else {
            Handler(Looper.getMainLooper()).post(bump)
        }
    }
}
