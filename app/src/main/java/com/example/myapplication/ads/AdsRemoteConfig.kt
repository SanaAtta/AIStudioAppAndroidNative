package com.example.myapplication.ads

import android.os.Handler
import android.os.Looper
import com.example.myapplication.BuildConfig
import com.example.myapplication.util.FirebaseBootstrap
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
          "splash": {
            "top": { "adaptive_banner_ad": 1, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "bottom": { "adaptive_banner_ad": 1, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "interstitial": 0,
            "app_open_ad": 1
          },
          "language": {
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "done_interstitial": 0,
            "top_premium_card": 0
          },
          "onboarding": {
            "top": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 0 },
            "bottom": { "adaptive_banner_ad": 0, "native_ad_without_media": 0, "native_ad_with_media": 1 },
            "interstitial": 0
          }
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
        if (!FirebaseBootstrap.awaitReady()) {
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
                        RC_KEY_AD_IDS to "{}",
                        RC_KEY_PREMIUM_SCREEN_TEST to "",
                    ),
                ).await()
                rc.setConfigSettingsAsync(
                    FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(interval)
                        .build(),
                ).await()
                rc.fetchAndActivate().await()
                val adsBlob = AdsControl.readAdsControlBlob(rc)
                if (adsBlob.isNotEmpty()) {
                    PremiumScreenTestResolver.applyToAdsControl()
                    AdUnitIds.applyAdIdsJson(rc.getString(RC_KEY_AD_IDS))
                    cachedAdsConfigApplied = true
                    fetchDone = true
                } else {
                    PremiumScreenTestResolver.applyToAdsControl()
                    AdUnitIds.applyAdIdsJson(rc.getString(RC_KEY_AD_IDS))
                }
            }.onFailure {
                AdsLoadLog.trace("ads_control", "fetch failed: ${it.message}")
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
