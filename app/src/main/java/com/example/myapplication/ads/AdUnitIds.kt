package com.example.myapplication.ads

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.example.myapplication.BuildConfig

/**
 * Firebase Remote Config key: **`ad_ids`** (JSON).
 *
 * **Debug:** Google [AdMobGoogleTestUnits] for all slots unless `admob.use.production.units=true`.
 * **Release:** Firebase `ad_ids` only.
 */
object AdUnitIds {
    private val gson = Gson()

    @Volatile
    private var ids: AdIdsRemoteJson = AdIdsRemoteJson()

    @Volatile
    private var lastAppliedRaw: String? = null

    fun applyAdIdsJson(raw: String?) {
        lastAppliedRaw = raw?.trim()?.takeIf { it.isNotEmpty() }
        if (raw.isNullOrBlank()) {
            ids = AdIdsRemoteJson()
            return
        }
        ids =
            runCatching { gson.fromJson(raw.trim(), AdIdsRemoteJson::class.java) }
                .getOrElse { AdIdsRemoteJson() }
    }

    private enum class TestSlot {
        Native,
        Banner,
        MediumRectangle,
        Interstitial,
        AppOpen,
    }

    private fun useDebugTestUnits(): Boolean =
        BuildConfig.DEBUG && !BuildConfig.USE_PRODUCTION_AD_UNITS

    private fun resolve(
        rc: String?,
        slot: TestSlot,
    ): String {
        if (useDebugTestUnits() && slot == TestSlot.Native) {
            return AdMobGoogleTestUnits.NATIVE_ADVANCED
        }
        if (useDebugTestUnits() && slot == TestSlot.Banner) {
            return AdMobGoogleTestUnits.ADAPTIVE_BANNER
        }
        if (useDebugTestUnits() && slot == TestSlot.MediumRectangle) {
            return AdMobGoogleTestUnits.MEDIUM_RECTANGLE
        }
        if (useDebugTestUnits() && slot == TestSlot.Interstitial) {
            return AdMobGoogleTestUnits.INTERSTITIAL
        }
        if (useDebugTestUnits() && slot == TestSlot.AppOpen) {
            return AdMobGoogleTestUnits.APP_OPEN
        }
        val t = rc?.trim()
        if (!t.isNullOrEmpty()) return t
        if (!useDebugTestUnits()) return ""
        return when (slot) {
            TestSlot.Native -> AdMobGoogleTestUnits.NATIVE_ADVANCED
            TestSlot.Banner -> AdMobGoogleTestUnits.ADAPTIVE_BANNER
            TestSlot.MediumRectangle -> AdMobGoogleTestUnits.MEDIUM_RECTANGLE
            TestSlot.Interstitial -> AdMobGoogleTestUnits.INTERSTITIAL
            TestSlot.AppOpen -> AdMobGoogleTestUnits.APP_OPEN
        }
    }

    fun splashNative(): String = resolve(ids.splashNative, TestSlot.Native)

    /** Shared unit for splash screen top and bottom adaptive banners. */
    fun splashAdaptiveBanner(): String =
        resolve(ids.splashAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    fun homeNative(): String = resolve(ids.homeNative, TestSlot.Native)

    fun homeAdaptiveBanner(): String = resolve(ids.homeAdaptiveBanner, TestSlot.Banner)

    fun languageNative(): String = resolve(ids.languageNative, TestSlot.Native)

    /** Shared unit for language screen top and bottom adaptive banners. */
    fun languageAdaptiveBanner(): String =
        resolve(ids.languageAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    fun onboardingNative(): String = resolve(ids.onboardingNative, TestSlot.Native)

    /** Shared unit for onboarding screen top and bottom adaptive banners. */
    fun onboardingAdaptiveBanner(): String =
        resolve(ids.onboardingAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    fun onboardingLastScreenNative(): String =
        resolve(ids.onboardingLastScreenNative, TestSlot.Native)

    fun exitNative(): String = resolve(ids.exitNative, TestSlot.Native)

    fun exitAdaptiveBanner(): String = resolve(ids.exitAdaptiveBanner, TestSlot.Banner)

    /** Exit confirmation screen — medium rectangle banner. */
    fun exitMediumRectangle(): String =
        resolve(
            ids.exitMediumRectangle ?: ids.socialPlatformMediumRectangle,
            TestSlot.MediumRectangle,
        )

    /** Platform paste screen — medium rectangle banner. */
    fun socialPlatformMediumRectangle(): String =
        resolve(ids.socialPlatformMediumRectangle, TestSlot.MediumRectangle)

    /** Platform paste screen native — RC `social_platform.native_ad`. */
    fun socialPlatformNative(): String = resolve(ids.exitNative, TestSlot.Native)

    /** @deprecated Use [socialPlatformNative]. */
    fun socialPlatformMediumNative(): String = socialPlatformNative()

    fun settingsAdaptiveBanner(): String =
        resolve(ids.settingsAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    fun reelsNative(): String = resolve(ids.reelsNative, TestSlot.Native)

    fun reelsAdaptiveBanner(): String =
        resolve(ids.reelsAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    fun reelsFullscreenNative(): String = resolve(ids.reelsFullscreenNative, TestSlot.Native)

    /** Same unit as [socialPlatformInterstitial] (Home social platform icon tap). */
    fun reelsBackInterstitial(): String = socialPlatformInterstitial()

    fun splashInterstitial(): String = resolve(ids.splashInterstitial, TestSlot.Interstitial)

    fun languageInterstitial(): String =
        resolve(ids.languageInterstitial, TestSlot.Interstitial)

    fun onboardingInterstitial(): String =
        resolve(ids.onboardingInterstitial, TestSlot.Interstitial)

    fun premiumInterstitial(): String =
        resolve(ids.premiumInterstitial, TestSlot.Interstitial)

    /** Video preview Download CTA — same unit as [genericInterstitial] (`Interstitial` in `ad_ids`). */
    fun videoPreviewDownloadInterstitial(): String = genericInterstitial()

    fun genericInterstitial(): String =
        resolve(ids.interstitial, TestSlot.Interstitial)

    fun socialPlatformInterstitial(): String {
        if (useDebugTestUnits()) return AdMobGoogleTestUnits.INTERSTITIAL
        val dedicated = ids.socialPlatformInterstitial?.trim()
        if (!dedicated.isNullOrEmpty()) return dedicated
        return genericInterstitial()
    }

    fun splashAppOpen(): String = resolve(ids.splashAppOpen, TestSlot.AppOpen)

    fun appOpenResume(): String = resolve(ids.appOpenResume, TestSlot.AppOpen)

    /** Firebase `ad_ids` JSON key used for [unitId] (for AdsLoad logging). */
    fun rcKeySocialPlatformInterstitial(): String {
        if (useDebugTestUnits()) return Rc.INTERSTITIAL
        val dedicated = ids.socialPlatformInterstitial?.trim()
        return if (!dedicated.isNullOrEmpty()) Rc.SOCIAL_PLATFORM_INTERSTITIAL else Rc.INTERSTITIAL
    }

    /** Reels back uses [socialPlatformInterstitial] unit id in production. */
    fun rcKeyReelsBackInterstitial(): String = rcKeySocialPlatformInterstitial()

    /** @deprecated Use [rcKeySocialPlatformMediumRectangle]. */
    fun rcKeySocialPlatformMediumNative(): String = Rc.EXIT_NATIVE

    fun rcKeySocialPlatformMediumRectangle(): String = Rc.SOCIAL_PLATFORM_MEDIUM_RECTANGLE

    fun rcKeyExitMediumRectangle(): String =
        if (ids.exitMediumRectangle?.trim().isNullOrEmpty()) {
            Rc.SOCIAL_PLATFORM_MEDIUM_RECTANGLE
        } else {
            Rc.EXIT_MEDIUM_RECTANGLE
        }

    fun configFingerprint(): String = lastAppliedRaw.orEmpty()

    /** Remote Config `ad_ids` JSON keys (see [AdIdsRemoteJson]). */
    object Rc {
        const val APP_OPEN_RESUME = "AppOpen_Resume"
        const val EXIT_NATIVE = "Exit_Native"
        const val EXIT_ADAPTIVE_BANNER = "Exit_Adaptive_Banner"
        const val EXIT_MEDIUM_RECTANGLE = "Exit_Medium_Rectangle"
        const val SPLASH_ADAPTIVE_BANNER = "Splash_Adaptive_Banner"
        const val SETTINGS_ADAPTIVE_BANNER = "Settings_Adaptive_Banner"
        const val SOCIAL_PLATFORM_MEDIUM_RECTANGLE = "Social_Platform_Medium_Rectangle"
        const val HOME_NATIVE = "Home_Native"
        const val HOME_ADAPTIVE_BANNER = "Home_Adaptive_Banner"
        const val INTERSTITIAL = "Interstitial"
        const val LANGUAGE_INTERSTITIAL = "Language_Interstitial"
        const val LANGUAGE_NATIVE = "Language_Native"
        const val LANGUAGE_ADAPTIVE_BANNER = "Language_Adaptive_Banner"
        const val ONBOARDING_INTERSTITIAL = "Onboarding_Interstitial"
        const val ONBOARDING_LAST_SCREEN_NATIVE = "Onboarding_LastScreen_Native"
        const val ONBOARDING_NATIVE = "Onboarding_Native"
        const val ONBOARDING_ADAPTIVE_BANNER = "Onboarding_Adaptive_Banner"
        const val PREMIUM_INTERSTITIAL = "Premium_Interstitial"
        const val REELS_FULLSCREEN_NATIVE = "Reels_FullScreen_Native"
        const val REELS_ADAPTIVE_BANNER = "Reels_Adaptive_Banner"
        const val REELS_NATIVE = "Reels_Native"
        const val SOCIAL_PLATFORM_INTERSTITIAL = "Social_Platform_Interstitial"
        const val SPLASH_APP_OPEN = "Splash_AppOpen"
        const val SPLASH_INTERSTITIAL = "Splash_Interstitial"
        const val SPLASH_NATIVE = "Splash_Native"
    }
}

/** Keys match Firebase `ad_ids` Remote Config (see `remote_config/ad_ids.sample.json`). */
private data class AdIdsRemoteJson(
    @SerializedName("AppOpen_Resume") val appOpenResume: String? = null,
    @SerializedName("Exit_Native") val exitNative: String? = null,
    @SerializedName("Exit_Adaptive_Banner") val exitAdaptiveBanner: String? = null,
    @SerializedName("Exit_Medium_Rectangle") val exitMediumRectangle: String? = null,
    @SerializedName("Splash_Adaptive_Banner") val splashAdaptiveBanner: String? = null,
    @SerializedName("Settings_Adaptive_Banner") val settingsAdaptiveBanner: String? = null,
    @SerializedName("Social_Platform_Medium_Rectangle") val socialPlatformMediumRectangle: String? = null,
    @SerializedName("Home_Native") val homeNative: String? = null,
    @SerializedName("Home_Adaptive_Banner") val homeAdaptiveBanner: String? = null,
    @SerializedName("Interstitial") val interstitial: String? = null,
    @SerializedName("Language_Interstitial") val languageInterstitial: String? = null,
    @SerializedName("Language_Native") val languageNative: String? = null,
    @SerializedName("Language_Adaptive_Banner") val languageAdaptiveBanner: String? = null,
    @SerializedName("Onboarding_Interstitial") val onboardingInterstitial: String? = null,
    @SerializedName("Onboarding_LastScreen_Native") val onboardingLastScreenNative: String? = null,
    @SerializedName("Onboarding_Native") val onboardingNative: String? = null,
    @SerializedName("Onboarding_Adaptive_Banner") val onboardingAdaptiveBanner: String? = null,
    @SerializedName("Premium_Interstitial") val premiumInterstitial: String? = null,
    @SerializedName("Reels_FullScreen_Native") val reelsFullscreenNative: String? = null,
    @SerializedName("Reels_Back_Interstitial") val reelsBackInterstitial: String? = null,
    @SerializedName("Reels_Adaptive_Banner") val reelsAdaptiveBanner: String? = null,
    @SerializedName("Reels_Native") val reelsNative: String? = null,
    @SerializedName("Splash_AppOpen") val splashAppOpen: String? = null,
    @SerializedName("Splash_Interstitial") val splashInterstitial: String? = null,
    @SerializedName("Splash_Native") val splashNative: String? = null,
    @SerializedName("Social_Platform_Interstitial") val socialPlatformInterstitial: String? = null,
)
