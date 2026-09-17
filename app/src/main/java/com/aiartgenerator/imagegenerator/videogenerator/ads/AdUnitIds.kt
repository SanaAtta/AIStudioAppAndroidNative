package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig

/**
 * Firebase Remote Config key: **`ad_ids`** (JSON).
 *
 * **Debug:** Google [AdMobGoogleTestUnits] for all slots unless `admob.use.production.units=true`.
 * **Release:** Firebase `ad_ids` with local asset fallback.
 */
object AdUnitIds {
    private const val TAG = "AdUnitIds"
    private val gson = Gson()

    @Volatile
    private var ids: AdIdsRemoteJson = AdIdsRemoteJson()

    @Volatile
    private var fallbackIds: AdIdsRemoteJson = AdIdsRemoteJson()

    @Volatile
    private var lastAppliedRaw: String? = null

    fun init(context: Context) {
        runCatching {
            val assetJson = context.assets.open("ad_ids.json").bufferedReader().use { it.readText() }
            if (assetJson.isNotBlank()) {
                fallbackIds = gson.fromJson(assetJson.trim(), AdIdsRemoteJson::class.java) ?: AdIdsRemoteJson()
                if (lastAppliedRaw.isNullOrBlank()) {
                    ids = fallbackIds
                }
                Log.d(TAG, "Loaded fallback ad_ids.json from assets")
            }
        }.onFailure {
            Log.d(TAG, "No asset ad_ids.json found or failed to parse: ${it.message}")
        }
    }

    fun applyAdIdsJson(raw: String?) {
        lastAppliedRaw = raw?.trim()?.takeIf { it.isNotEmpty() }
        if (raw.isNullOrBlank()) {
            ids = fallbackIds
            return
        }
        val parsed = runCatching { gson.fromJson(raw.trim(), AdIdsRemoteJson::class.java) }.getOrNull()
        ids = if (parsed != null) {
            parsed.mergeWith(fallbackIds)
        } else {
            fallbackIds
        }
    }

    private enum class TestSlot {
        Native,
        Banner,
        MediumRectangle,
        Interstitial,
        Rewarded,
        RewardedInterstitial,
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
        if (useDebugTestUnits() && slot == TestSlot.Rewarded) {
            return AdMobGoogleTestUnits.REWARDED
        }
        if (useDebugTestUnits() && slot == TestSlot.RewardedInterstitial) {
            return AdMobGoogleTestUnits.REWARDED_INTERSTITIAL
        }
        val t = rc?.trim()
        if (!t.isNullOrEmpty()) return t
        if (!useDebugTestUnits()) return ""
        return when (slot) {
            TestSlot.Native -> AdMobGoogleTestUnits.NATIVE_ADVANCED
            TestSlot.Banner -> AdMobGoogleTestUnits.ADAPTIVE_BANNER
            TestSlot.MediumRectangle -> AdMobGoogleTestUnits.MEDIUM_RECTANGLE
            TestSlot.Interstitial -> AdMobGoogleTestUnits.INTERSTITIAL
            TestSlot.Rewarded -> AdMobGoogleTestUnits.REWARDED
            TestSlot.RewardedInterstitial -> AdMobGoogleTestUnits.REWARDED_INTERSTITIAL
            TestSlot.AppOpen -> AdMobGoogleTestUnits.APP_OPEN
        }
    }

    fun splashNative(): String = resolve(ids.splashNative, TestSlot.Native)

    /** Shared unit for splash screen top and bottom adaptive banners. */
    fun splashAdaptiveBanner(): String =
        resolve(ids.splashAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    fun homeNative(): String = resolve(ids.homeNative, TestSlot.Native)

    fun homeAdaptiveBanner(): String = resolve(ids.homeAdaptiveBanner, TestSlot.Banner)

    fun libraryNative(): String =
        resolve(ids.libraryNative ?: ids.languageNative ?: ids.homeNative, TestSlot.Native)

    fun libraryAdaptiveBanner(): String =
        resolve(
            ids.libraryAdaptiveBanner ?: ids.languageAdaptiveBanner ?: ids.homeAdaptiveBanner,
            TestSlot.Banner,
        )

    fun wallpaperNative(): String =
        resolve(ids.wallpaperNative ?: ids.languageNative ?: ids.homeNative, TestSlot.Native)

    fun wallpaperAdaptiveBanner(): String =
        resolve(
            ids.wallpaperAdaptiveBanner ?: ids.languageAdaptiveBanner ?: ids.homeAdaptiveBanner,
            TestSlot.Banner,
        )

    fun imageResultNative(): String =
        resolve(ids.imageResultNative ?: ids.homeNative ?: ids.languageNative, TestSlot.Native)

    fun imageResultAdaptiveBanner(): String =
        resolve(
            ids.imageResultAdaptiveBanner ?: ids.homeAdaptiveBanner ?: ids.languageAdaptiveBanner,
            TestSlot.Banner,
        )

    fun videoResultNative(): String =
        resolve(ids.videoResultNative ?: ids.homeNative ?: ids.languageNative, TestSlot.Native)

    fun videoResultAdaptiveBanner(): String =
        resolve(
            ids.videoResultAdaptiveBanner ?: ids.homeAdaptiveBanner ?: ids.languageAdaptiveBanner,
            TestSlot.Banner,
        )

    fun musicResultNative(): String =
        resolve(ids.musicResultNative ?: ids.homeNative ?: ids.languageNative, TestSlot.Native)

    fun musicResultAdaptiveBanner(): String =
        resolve(
            ids.musicResultAdaptiveBanner ?: ids.homeAdaptiveBanner ?: ids.languageAdaptiveBanner,
            TestSlot.Banner,
        )


    fun languageNative(): String = resolve(ids.languageNative, TestSlot.Native)

    /** Shared unit for language screen top and bottom adaptive banners. */
    fun languageAdaptiveBanner(): String =
        resolve(ids.languageAdaptiveBanner ?: ids.homeAdaptiveBanner, TestSlot.Banner)

    /** Generating / progress screen — falls back to language units. */
    fun generatingNative(): String =
        resolve(ids.generatingNative ?: ids.languageNative ?: ids.homeNative, TestSlot.Native)

    fun generatingAdaptiveBanner(): String =
        resolve(
            ids.generatingAdaptiveBanner ?: ids.languageAdaptiveBanner ?: ids.homeAdaptiveBanner,
            TestSlot.Banner,
        )

    /** Image generation progress — rewarded first. */
    fun generatingRewarded(): String = resolve(ids.generatingRewarded, TestSlot.Rewarded)

    /** Image generation progress — rewarded interstitial fallback only. */
    fun generatingRewardedInterstitial(): String =
        resolve(ids.generatingRewardedInterstitial, TestSlot.RewardedInterstitial)

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
        const val LIBRARY_NATIVE = "Library_Native"
        const val LIBRARY_ADAPTIVE_BANNER = "Library_Adaptive_Banner"
        const val WALLPAPER_NATIVE = "Wallpaper_Native"
        const val WALLPAPER_ADAPTIVE_BANNER = "Wallpaper_Adaptive_Banner"
        const val IMAGE_RESULT_NATIVE = "Image_Result_Native"
        const val IMAGE_RESULT_ADAPTIVE_BANNER = "Image_Result_Adaptive_Banner"
        const val VIDEO_RESULT_NATIVE = "Video_Result_Native"
        const val VIDEO_RESULT_ADAPTIVE_BANNER = "Video_Result_Adaptive_Banner"
        const val MUSIC_RESULT_NATIVE = "Music_Result_Native"
        const val MUSIC_RESULT_ADAPTIVE_BANNER = "Music_Result_Adaptive_Banner"
        const val INTERSTITIAL = "Interstitial"
        const val LANGUAGE_INTERSTITIAL = "Language_Interstitial"
        const val LANGUAGE_NATIVE = "Language_Native"
        const val LANGUAGE_ADAPTIVE_BANNER = "Language_Adaptive_Banner"
        const val GENERATING_NATIVE = "Generating_Native"
        const val GENERATING_ADAPTIVE_BANNER = "Generating_Adaptive_Banner"
        const val GENERATING_REWARDED = "Generating_Rewarded"
        const val GENERATING_REWARDED_INTERSTITIAL = "Generating_Rewarded_Interstitial"
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
    @SerializedName("Library_Native") val libraryNative: String? = null,
    @SerializedName("Library_Adaptive_Banner") val libraryAdaptiveBanner: String? = null,
    @SerializedName("Wallpaper_Native") val wallpaperNative: String? = null,
    @SerializedName("Wallpaper_Adaptive_Banner") val wallpaperAdaptiveBanner: String? = null,
    @SerializedName("Image_Result_Native") val imageResultNative: String? = null,
    @SerializedName("Image_Result_Adaptive_Banner") val imageResultAdaptiveBanner: String? = null,
    @SerializedName("Video_Result_Native") val videoResultNative: String? = null,
    @SerializedName("Video_Result_Adaptive_Banner") val videoResultAdaptiveBanner: String? = null,
    @SerializedName("Music_Result_Native") val musicResultNative: String? = null,
    @SerializedName("Music_Result_Adaptive_Banner") val musicResultAdaptiveBanner: String? = null,
    @SerializedName("Interstitial") val interstitial: String? = null,
    @SerializedName("Language_Interstitial") val languageInterstitial: String? = null,
    @SerializedName("Language_Native") val languageNative: String? = null,
    @SerializedName("Language_Adaptive_Banner") val languageAdaptiveBanner: String? = null,
    @SerializedName("Generating_Native") val generatingNative: String? = null,
    @SerializedName("Generating_Adaptive_Banner") val generatingAdaptiveBanner: String? = null,
    @SerializedName("Generating_Rewarded") val generatingRewarded: String? = null,
    @SerializedName("Generating_Rewarded_Interstitial") val generatingRewardedInterstitial: String? = null,
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
) {
    fun mergeWith(fallback: AdIdsRemoteJson): AdIdsRemoteJson =
        AdIdsRemoteJson(
            appOpenResume = this.appOpenResume?.takeIf { it.isNotBlank() } ?: fallback.appOpenResume,
            exitNative = this.exitNative?.takeIf { it.isNotBlank() } ?: fallback.exitNative,
            exitAdaptiveBanner = this.exitAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.exitAdaptiveBanner,
            exitMediumRectangle = this.exitMediumRectangle?.takeIf { it.isNotBlank() } ?: fallback.exitMediumRectangle,
            splashAdaptiveBanner = this.splashAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.splashAdaptiveBanner,
            settingsAdaptiveBanner = this.settingsAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.settingsAdaptiveBanner,
            socialPlatformMediumRectangle = this.socialPlatformMediumRectangle?.takeIf { it.isNotBlank() } ?: fallback.socialPlatformMediumRectangle,
            homeNative = this.homeNative?.takeIf { it.isNotBlank() } ?: fallback.homeNative,
            homeAdaptiveBanner = this.homeAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.homeAdaptiveBanner,
            libraryNative = this.libraryNative?.takeIf { it.isNotBlank() } ?: fallback.libraryNative,
            libraryAdaptiveBanner = this.libraryAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.libraryAdaptiveBanner,
            wallpaperNative = this.wallpaperNative?.takeIf { it.isNotBlank() } ?: fallback.wallpaperNative,
            wallpaperAdaptiveBanner = this.wallpaperAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.wallpaperAdaptiveBanner,
            imageResultNative = this.imageResultNative?.takeIf { it.isNotBlank() } ?: fallback.imageResultNative,
            imageResultAdaptiveBanner = this.imageResultAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.imageResultAdaptiveBanner,
            videoResultNative = this.videoResultNative?.takeIf { it.isNotBlank() } ?: fallback.videoResultNative,
            videoResultAdaptiveBanner = this.videoResultAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.videoResultAdaptiveBanner,
            musicResultNative = this.musicResultNative?.takeIf { it.isNotBlank() } ?: fallback.musicResultNative,
            musicResultAdaptiveBanner = this.musicResultAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.musicResultAdaptiveBanner,
            interstitial = this.interstitial?.takeIf { it.isNotBlank() } ?: fallback.interstitial,
            languageInterstitial = this.languageInterstitial?.takeIf { it.isNotBlank() } ?: fallback.languageInterstitial,
            languageNative = this.languageNative?.takeIf { it.isNotBlank() } ?: fallback.languageNative,
            languageAdaptiveBanner = this.languageAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.languageAdaptiveBanner,
            generatingNative = this.generatingNative?.takeIf { it.isNotBlank() } ?: fallback.generatingNative,
            generatingAdaptiveBanner = this.generatingAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.generatingAdaptiveBanner,
            generatingRewarded = this.generatingRewarded?.takeIf { it.isNotBlank() } ?: fallback.generatingRewarded,
            generatingRewardedInterstitial = this.generatingRewardedInterstitial?.takeIf { it.isNotBlank() } ?: fallback.generatingRewardedInterstitial,
            onboardingInterstitial = this.onboardingInterstitial?.takeIf { it.isNotBlank() } ?: fallback.onboardingInterstitial,
            onboardingLastScreenNative = this.onboardingLastScreenNative?.takeIf { it.isNotBlank() } ?: fallback.onboardingLastScreenNative,
            onboardingNative = this.onboardingNative?.takeIf { it.isNotBlank() } ?: fallback.onboardingNative,
            onboardingAdaptiveBanner = this.onboardingAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.onboardingAdaptiveBanner,
            premiumInterstitial = this.premiumInterstitial?.takeIf { it.isNotBlank() } ?: fallback.premiumInterstitial,
            reelsFullscreenNative = this.reelsFullscreenNative?.takeIf { it.isNotBlank() } ?: fallback.reelsFullscreenNative,
            reelsBackInterstitial = this.reelsBackInterstitial?.takeIf { it.isNotBlank() } ?: fallback.reelsBackInterstitial,
            reelsAdaptiveBanner = this.reelsAdaptiveBanner?.takeIf { it.isNotBlank() } ?: fallback.reelsAdaptiveBanner,
            reelsNative = this.reelsNative?.takeIf { it.isNotBlank() } ?: fallback.reelsNative,
            splashAppOpen = this.splashAppOpen?.takeIf { it.isNotBlank() } ?: fallback.splashAppOpen,
            splashInterstitial = this.splashInterstitial?.takeIf { it.isNotBlank() } ?: fallback.splashInterstitial,
            splashNative = this.splashNative?.takeIf { it.isNotBlank() } ?: fallback.splashNative,
            socialPlatformInterstitial = this.socialPlatformInterstitial?.takeIf { it.isNotBlank() } ?: fallback.socialPlatformInterstitial,
        )
}
