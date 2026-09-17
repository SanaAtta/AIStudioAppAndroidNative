package com.aiartgenerator.imagegenerator.videogenerator.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess

/**
 * Firebase Remote Config ads JSON — always **`ads_control`** (debug + release).
 * Optional fallback: `ads_control_debug` when primary omits `premium_screen_test`.
 *
 * Unit ids: **`ad_ids`** ([AdUnitIds]).
 *
 * `1` = on, `0` = off. Native slots use nested `top` / `bottom` with format flags only (no `native_ad` master).
 *
 * **Adaptive banner slots** (`adaptive_banner_ad` under `top` / `bottom`, or `exit.adaptive_banner_ad`,
 * `settings.bottom_adaptive_banner`): strict format — banner wins when both banner and native flags are `1`;
 * no cross-format fallback on load failure.
 *
 * **Exit screen:** `exit.medium_rectangle_banner` (`1` = MREC 300×250, wins over `adaptive_banner_ad`).
 * **Social platform paste screens:** `social_platform.medium_rectangle_banner` (`1` = MREC 300×250).
 * `social_platform.native_ad` (`1` = large native via [NativeMediumEmbeddedAd] / `native_medium` layout).
 *
 * Splash / language / onboarding slots (`top` / `bottom`):
 * ```json
 * "top": { "adaptive_banner_ad": 1, "native_ad_without_media": 1, "native_ad_with_media": 0 }
 * ```
 * Slot is **on** if either format flag is `1`. Both `0` = slot off.
 *
 * **Strict RC:** Only formats explicitly set to `1` in Firebase load. No banner fallback when
 * native fails unless `adaptive_banner_ad` is also `1` for that slot (banner wins when both on).
 *
 * **Subscription A/B/C:** root `premium_screen_test`: `A` | `B` | `C` (empty = sticky random A/B).
 * JSON value wins over the standalone Firebase param `premium_screen_test`.
 *
 * Sample: `remote_config/ads_control.sample.json`, `remote_config/ads_control_debug.sample.json`
 *
 * **Debug:** root `ads_debug_log: 1` → Logcat tag `AdsLoad` prints every ad REQUEST / LOADED / SHOW / FAILED
 * screen-wise (`| screen=splash | event=LOADED | ...`). `0` or omitted = silent.
 */
object AdsControl {
    const val RC_KEY_ADS_CONTROL = "ads_control"
    /** Legacy / mistaken Firebase key — accepted as fallback only. Prefer [RC_KEY_ADS_CONTROL]. */
    const val RC_KEY_ADS_CONFIG = "ads_config"
    const val RC_KEY_ADS_CONTROL_DEBUG = "ads_control_debug"

    /** Always production blob — debug and release share `ads_control`. */
    fun adsControlRemoteConfigKey(): String = RC_KEY_ADS_CONTROL

    /** Last-activated ads JSON blob from Firebase (`ads_control`, else `ads_config`). */
    fun readAdsControlBlob(rc: FirebaseRemoteConfig): String {
        val primary = rc.getString(adsControlRemoteConfigKey()).trim()
        if (primary.isNotEmpty()) return primary
        return rc.getString(RC_KEY_ADS_CONFIG).trim()
    }

    fun hasAdsControlBlob(rc: FirebaseRemoteConfig): Boolean =
        readAdsControlBlob(rc).isNotEmpty()

    private val gson = Gson()

    private val premiumScreenTestPattern =
        Regex(
            """"premium_screen_test"\s*:\s*"?([A-Za-z0-9]+)"?""",
            RegexOption.IGNORE_CASE,
        )
    private val showAfterOnboardingPattern =
        Regex(
            """"show_after_onboarding"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val exitDealDialogPattern =
        Regex(
            """"exit_deal_dialog"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val giftFabPattern =
        Regex(
            """"gift_fab"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val promosEnabledPattern =
        Regex(
            """"promos_enabled"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val subscriptionScreenAbPattern =
        Regex(
            """"subscription_screen_ab"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val reelsBackInterstitialPattern =
        Regex(
            """"back_interstitial"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val homeSocialPlatformInterstitialPattern =
        Regex(
            """"social_platform_interstitial"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val homeBottomAdaptiveBannerPattern =
        Regex(
            """"home"\s*:\s*\{[\s\S]*?"bottom"\s*:\s*\{[^{}]*"adaptive_banner_ad"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val homeBottomAdaptiveBannerFlatPattern =
        Regex(
            """"home"\s*:\s*\{[\s\S]*?"bottom_adaptive_banner"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val homeTopAdaptiveBannerPattern =
        Regex(
            """"home"\s*:\s*\{[\s\S]*?"top"\s*:\s*\{[^{}]*"adaptive_banner_ad"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val socialPlatformSectionInterstitialPattern =
        Regex(
            """"social_platform"[\s\S]*?"interstitial"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val socialPlatformMediumRectangleBannerPattern =
        Regex(
            """"social_platform"\s*:\s*\{[\s\S]*?"medium_rectangle_banner"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val exitMediumRectangleBannerPattern =
        Regex(
            """"exit"\s*:\s*\{[\s\S]*?"medium_rectangle_banner"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )
    private val adsDebugLogPattern =
        Regex(
            """"ads_debug_log"\s*:\s*(\d)""",
            RegexOption.IGNORE_CASE,
        )

    @Volatile
    private var root: AdsControlRoot = AdsControlRoot()

    @Volatile
    private var lastAdsControlRaw: String? = null

    /** Standalone Firebase RC key `premium_screen_test` — used only when absent from `ads_control` JSON. */
    @Volatile
    private var premiumScreenTestOverride: String? = null

    private fun premiumScreenTestMatchesInRaw(raw: String): List<String> =
        premiumScreenTestPattern
            .findAll(raw)
            .mapNotNull { it.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } }
            .toList()

    private fun premiumScreenTestFromRegexFirst(raw: String): String? =
        premiumScreenTestMatchesInRaw(raw).firstOrNull()

    /** Logcat: why `premium_screen_test` resolved to a given letter. */
    fun premiumScreenTestDiagnostics(): String {
        val raw = lastAdsControlRaw.orEmpty()
        return "gsonRoot=${root.premiumScreenTestRoot} gsonPremium=${root.premium?.premiumScreenTest} " +
            "standaloneOverride=$premiumScreenTestOverride " +
            "regexFirst=${premiumScreenTestFromRegexFirst(raw)} regexAll=${premiumScreenTestMatchesInRaw(raw)} " +
            "snippet=${premiumScreenTestRawSnippet(raw)} " +
            "subscriptionAb=${root.premium?.subscriptionScreenAb} " +
            "hash=${appliedConfigFingerprint()} len=${raw.length}"
    }

    private fun premiumScreenTestRawSnippet(raw: String): String {
        val idx = raw.indexOf("premium_screen_test", ignoreCase = true)
        if (idx < 0) return "key_missing"
        val end = (idx + 50).coerceAtMost(raw.length)
        return raw.substring(idx, end).replace('\n', ' ')
    }

    fun applyAdsControlJson(raw: String?) {
        if (raw.isNullOrBlank()) {
            root = AdsControlRoot()
            lastAdsControlRaw = null
            return
        }
        val trimmed = decodeAdsControlJson(raw)
        lastAdsControlRaw = trimmed
        val parsed =
            runCatching { gson.fromJson(trimmed, AdsControlRoot::class.java) }
                .getOrElse { AdsControlRoot() }
        val fromGson =
            parsed.premiumScreenTestRoot?.trim()?.takeIf { it.isNotEmpty() }
                ?: parsed.premium?.premiumScreenTest?.trim()?.takeIf { it.isNotEmpty() }
        val fromRegex = premiumScreenTestFromRegexFirst(trimmed)
        val mergedTest = fromGson ?: fromRegex
        root =
            if (mergedTest.isNullOrBlank()) {
                parsed
            } else {
                parsed.copy(premiumScreenTestRoot = mergedTest)
            }
        NativeAdCtaStyles.applyFromAdsControl(root.nativeAdCta)
        AdsLoadLog.trace(
            "ads_control applied",
            "rcKey=${adsControlRemoteConfigKey()} hash=${adsControlHash(trimmed)} len=${trimmed.length} " +
                "adsDebugLog=${adsDebugLogEnabled()} " +
                "premium_screen_test=${premiumScreenTest()} " +
                "exit_deal=${showPremiumExitDealDialog()} gift_fab=${showPremiumGiftFab()} " +
                "${setupFlowRcDebugSnapshot()} " +
                "${homeBottomRcDebugSnapshot()} | raw=${homeAdsControlRawHomeSection()}",
        )
    }

    private fun setupFlowRcDebugSnapshot(): String =
        "language.done_interstitial.rc=${readAdsControlNestedSectionIntFlag("language", "done_interstitial")} " +
            "enabled=${languageDoneInterstitialEnabled()} " +
            "onboarding.interstitial.rc=${readAdsControlNestedSectionIntFlag("onboarding", "interstitial")} " +
            "onboarding.last_screen_native_ad.rc=${readAdsControlNestedSectionIntFlag("onboarding", "last_screen_native_ad")} " +
            "lastScreen=${onboardingLastScreenNativeAdRcEnabled()}"

    /** Firebase `ads_control.ads_debug_log` — `1` prints ad logs in release; debug builds always log. */
    fun adsDebugLogEnabled(): Boolean {
        if (root.adsDebugLog == 1) return true
        return lastAdsControlRaw?.let { adsDebugLogPattern.find(it)?.groupValues?.getOrNull(1) == "1" } == true
    }

    private fun adsControlHash(raw: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.take(4).joinToString("") { "%02x".format(it) }
    }

    /** Fingerprint of last applied `ads_control` JSON — for deduping [AdsConfigRevision] bumps. */
    fun appliedConfigFingerprint(): String =
        lastAdsControlRaw?.let { adsControlHash(it) }.orEmpty()

    /** Read `section.slot` int flag from raw JSON — explicit `0` wins over stale Gson. */
    private fun readNestedSlotIntFlag(section: String, slot: String, key: String): Int? {
        val raw = lastAdsControlRaw ?: return null
        val pattern =
            Regex(
                """"$section"\s*:\s*\{[\s\S]*?"$slot"\s*:\s*\{[^{}]*"$key"\s*:\s*(\d)""",
                RegexOption.IGNORE_CASE,
            )
        return pattern.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    /** Read `home.top` / `home.bottom` int flag from raw JSON (matches [homeAdsControlRawHomeSection]). */
    private fun readHomeSlotIntFlag(slot: String, key: String): Int? =
        readNestedSlotIntFlag("home", slot, key)

    /** Read flat `social_platform` / `exit` keys from raw JSON — explicit `0` must win over Gson. */
    private fun readAdsControlSectionIntFlag(section: String, key: String): Int? {
        val raw = lastAdsControlRaw ?: return null
        val pattern =
            Regex(
                """"$section"\s*:\s*\{[^{}]*"$key"\s*:\s*(\d)""",
                RegexOption.IGNORE_CASE,
            )
        return pattern.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    /**
     * Read a scalar inside a section that also has nested `top` / `bottom` blocks
     * (e.g. `language.done_interstitial`, `onboarding.interstitial`).
     * Parses only the `{...}` body of the section key so root-level duplicates cannot win.
     */
    private fun readAdsControlNestedSectionIntFlag(section: String, key: String): Int? {
        val raw = lastAdsControlRaw ?: return null
        val sectionBody = extractJsonObjectBody(raw, section) ?: return null
        return readJsonIntFlag(sectionBody, key)
    }

    /** Reads `0`/`1`, quoted `"0"`/`"1"`, and `true`/`false` from a JSON object fragment. */
    private fun readJsonIntFlag(jsonFragment: String, key: String): Int? {
        val patterns =
            listOf(
                Regex(""""$key"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE),
                Regex(""""$key"\s*:\s*"(\d+)"""", RegexOption.IGNORE_CASE),
                Regex(""""$key"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE),
            )
        for (pattern in patterns) {
            val match = pattern.findAll(jsonFragment).lastOrNull() ?: continue
            val raw = match.groupValues.getOrNull(1) ?: continue
            return when (raw.lowercase()) {
                "1", "true" -> 1
                "0", "false" -> 0
                else -> raw.toIntOrNull()
            }
        }
        return null
    }

    private fun extractJsonObjectBody(raw: String, sectionKey: String): String? {
        val marker = "\"$sectionKey\""
        var searchFrom = 0
        while (searchFrom < raw.length) {
            val keyStart = raw.indexOf(marker, searchFrom, ignoreCase = true)
            if (keyStart < 0) return null
            val braceStart = raw.indexOf('{', keyStart + marker.length)
            if (braceStart < 0) return null
            var depth = 0
            for (i in braceStart until raw.length) {
                when (raw[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            return raw.substring(braceStart, i + 1)
                        }
                    }
                }
            }
            return null
        }
        return null
    }

    private fun nestedSlotNativeEnabled(section: String, slot: String, config: NativeAdSlotConfig?): Boolean {
        if (readNestedSlotIntFlag(section, slot, "native_ad_with_media") == 1) return true
        if (readNestedSlotIntFlag(section, slot, "native_ad_without_media") == 1) return true
        if (
            readNestedSlotIntFlag(section, slot, "native_ad_with_media") == 0 &&
            readNestedSlotIntFlag(section, slot, "native_ad_without_media") == 0
        ) {
            return false
        }
        return nativeSlotEnabled(config)
    }

    private fun homeSlotNativeEnabled(slot: String): Boolean {
        val config = if (slot == "bottom") root.home?.bottom else root.home?.top
        return nestedSlotNativeEnabled("home", slot, config)
    }

    private fun resolveNestedAdaptiveBannerOn(section: String, slot: String, config: NativeAdSlotConfig?): Boolean {
        when (readNestedSlotIntFlag(section, slot, "adaptive_banner_ad")) {
            1 -> return true
            0 -> return false
        }
        return config?.adaptiveBannerAd == 1
    }

    private fun nestedSlotAdFormat(section: String, slot: String, config: NativeAdSlotConfig?): HomeAdSlotFormat {
        if (!adsAllowed()) return homeAdSlotHidden()
        if (resolveNestedAdaptiveBannerOn(section, slot, config)) {
            return HomeAdSlotFormat(
                visible = true,
                adaptiveBanner = true,
                nativeTemplate = NativeAdTemplate.DISABLED,
            )
        }
        if (!nestedSlotNativeEnabled(section, slot, config)) return homeAdSlotHidden()
        val native =
            when {
                readNestedSlotIntFlag(section, slot, "native_ad_with_media") == 1 ->
                    NativeAdTemplate.WITH_MEDIA
                readNestedSlotIntFlag(section, slot, "native_ad_without_media") == 1 ->
                    NativeAdTemplate.WITHOUT_MEDIA
                else -> nativeSlotTemplate(config)
            }
        if (native == NativeAdTemplate.DISABLED) return homeAdSlotHidden()
        return HomeAdSlotFormat(
            visible = true,
            adaptiveBanner = false,
            nativeTemplate = native,
        )
    }

    /** Firebase may store `ads_control` as a JSON-escaped string — unwrap before Gson. */
    private fun decodeAdsControlJson(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("\"")) return trimmed
        return runCatching { gson.fromJson(trimmed, String::class.java) }.getOrDefault(trimmed)
    }

    /** Standalone RC param — `A` / `B` / `C`; `default` / empty does not override JSON. */
    fun setPremiumScreenTestOverride(value: String?) {
        premiumScreenTestOverride =
            value?.trim()?.uppercase()?.takeIf { it == "A" || it == "B" || it == "C" }
    }

    /** Reads `premium_screen_test` from a raw ads_control JSON blob without applying the rest. */
    fun extractPremiumScreenTestFromBlob(raw: String): String? {
        val trimmed = decodeAdsControlJson(raw).trim()
        if (trimmed.isEmpty()) return null
        val parsed =
            runCatching { gson.fromJson(trimmed, AdsControlRoot::class.java) }
                .getOrNull()
        return parsed?.premiumScreenTestRoot?.trim()?.takeIf { it.isNotEmpty() }
            ?: parsed?.premium?.premiumScreenTest?.trim()?.takeIf { it.isNotEmpty() }
            ?: premiumScreenTestFromRegexFirst(trimmed)
    }

    fun setPremiumScreenTestFromRemote(value: String?) {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return
        root = root.copy(premiumScreenTestRoot = normalized)
    }

    fun privacyPolicyUrlFromAdsControl(): String? =
        root.privacyPolicyUrl?.trim()?.takeIf { it.isNotBlank() }

    fun termsUrlFromAdsControl(): String? =
        root.termsAndConditionsUrl?.trim()?.takeIf { it.isNotBlank() }

    /** Resolved privacy policy URL from RC (`privacy_policy_url`). */
    fun privacyPolicyUrl(
        fallback: String = "https://allvideodownloaderhdvideoplayer.blogspot.com/2026/05/privacy-policy.html",
    ): String = privacyPolicyUrlFromAdsControl() ?: fallback

    /** Terms URL from RC, else same as privacy policy. */
    fun termsUrl(): String =
        termsUrlFromAdsControl() ?: privacyPolicyUrl()

    private fun Int?.isOn(defaultOn: Boolean = true): Boolean {
        if (this == null) return defaultOn
        return this == 1
    }

    fun allAdsOn(): Boolean = root.allAdsOnOff.isOn(true)

    /** RC master switch only (ignores subscription). */
    private fun adsRcAllowed(): Boolean =
        AdsRemoteConfig.isAdsConfigReady() && allAdsOn()

    /**
     * True when ads may load/show: remote config allows ads and user is not subscribed.
     * Subscribers never see ads even if RC has all slots on.
     */
    fun shouldShowAds(): Boolean {
        if (!adsRcAllowed() || !PremiumAccess.shouldShowAds()) return false
        if (AdsConsentManager.canRequestAds()) return true
        if (AdsConsentManager.hasGatheredConsentThisSession()) return true
        // Debug: Google test units still load when UMP has not resolved yet.
        return BuildConfig.DEBUG
    }

    private fun adsAllowed(): Boolean = shouldShowAds()

    // --- Splash ---
    fun splashTopAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("splash", "top", root.splash?.top)

    fun splashBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("splash", "bottom", root.splash?.bottom)

    fun splashTopAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && splashTopAdSlotFormat().adaptiveBanner

    fun splashBottomAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && splashBottomAdSlotFormat().adaptiveBanner

    fun splashTopSlotEnabled(): Boolean = splashTopAdSlotFormat().visible

    fun splashBottomSlotEnabled(): Boolean = splashBottomAdSlotFormat().visible

    fun splashNativeTopEnabled(): Boolean {
        val f = splashTopAdSlotFormat()
        return f.visible && !f.adaptiveBanner && f.nativeTemplate != NativeAdTemplate.DISABLED
    }

    fun splashNativeBottomEnabled(): Boolean {
        val f = splashBottomAdSlotFormat()
        return f.visible && !f.adaptiveBanner && f.nativeTemplate != NativeAdTemplate.DISABLED
    }

    fun splashNativeTopTemplate(): NativeAdTemplate = splashTopAdSlotFormat().nativeTemplate

    fun splashNativeBottomTemplate(): NativeAdTemplate = splashBottomAdSlotFormat().nativeTemplate

    /** At most one splash ad slot; bottom wins when both top and bottom are on in RC. */
    fun splashShowTopAdSlot(): Boolean =
        topAdVisible(splashTopAdSlotFormat().visible, splashBottomAdSlotFormat().visible)

    fun splashShowBottomAdSlot(): Boolean = splashBottomAdSlotFormat().visible

    fun splashInterstitialEnabled(): Boolean = adsAllowed() && root.splash?.interstitial.isOn(false)

    fun splashAppOpenEnabled(): Boolean = adsAllowed() && root.splash?.appOpenAd.isOn(false)

    /** Splash uses interstitial *or* app-open, not both — avoids a wasted app-open request (often NO_FILL). */
    fun splashShouldPreloadAppOpen(): Boolean =
        splashAppOpenEnabled() && !splashInterstitialEnabled()

    fun resumeAppOpenEnabled(): Boolean = adsAllowed() && root.appOpenResume.isOn(false)

    // --- Language ---
    fun languageTopAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("language", "top", root.language?.top)

    fun languageBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("language", "bottom", root.language?.bottom)

    fun languageTopAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && languageTopAdSlotFormat().adaptiveBanner

    fun languageBottomAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && languageBottomAdSlotFormat().adaptiveBanner

    fun languageTopSlotEnabled(): Boolean = languageTopAdSlotFormat().visible

    fun languageBottomSlotEnabled(): Boolean = languageBottomAdSlotFormat().visible

    fun languageNativeTopEnabled(): Boolean {
        val f = languageTopAdSlotFormat()
        return f.visible && !f.adaptiveBanner && f.nativeTemplate != NativeAdTemplate.DISABLED
    }

    fun languageNativeBottomEnabled(): Boolean {
        val f = languageBottomAdSlotFormat()
        return f.visible && !f.adaptiveBanner && f.nativeTemplate != NativeAdTemplate.DISABLED
    }

    fun languageNativeTopTemplate(): NativeAdTemplate = languageTopAdSlotFormat().nativeTemplate

    fun languageNativeBottomTemplate(): NativeAdTemplate = languageBottomAdSlotFormat().nativeTemplate

    /** Bottom slot — legacy alias. */
    fun languageNativeEnabled(): Boolean = languageNativeBottomEnabled()

    /** At most one language ad slot; bottom wins when both are on in RC. */
    fun languageShowTopAdSlot(): Boolean =
        topAdVisible(languageTopAdSlotFormat().visible, languageBottomAdSlotFormat().visible)

    fun languageShowBottomAdSlot(): Boolean = languageBottomAdSlotFormat().visible

    fun languageTopPremiumCardEnabled(): Boolean =
        premiumPromosEnabled() && root.language?.topPremiumCard.isOn(true)

    // --- Generating / progress — RC: `generate_result.top` / `generate_result.bottom` ---
    fun generatingTopAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("generate_result", "top", root.generateResult?.top)

    fun generatingBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("generate_result", "bottom", root.generateResult?.bottom)

    fun generatingShowTopAdSlot(): Boolean = generatingTopAdSlotFormat().visible

    fun generatingShowBottomAdSlot(): Boolean = generatingBottomAdSlotFormat().visible

    // --- Library (bottom slot like language) ---
    fun libraryBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("library", "bottom", root.library?.bottom)

    fun libraryShowBottomAdSlot(): Boolean = libraryBottomAdSlotFormat().visible

    // --- Wallpaper (bottom slot like language) ---
    fun wallpaperBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("wallpaper", "bottom", root.wallpaper?.bottom)

    fun wallpaperShowBottomAdSlot(): Boolean = wallpaperBottomAdSlotFormat().visible

    /**
     * Home mid-scroll slot above "Recent AI Creations".
     * RC: `home.top` (banner / without_media / with_media) — independent of home.bottom lane.
     */
    fun homeRecentSectionAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("home", "top", root.home?.top)

    fun homeShowRecentSectionAdSlot(): Boolean = homeRecentSectionAdSlotFormat().visible

    // --- Feature click interstitials ---
    fun imageGeneratorInterstitialEnabled(): Boolean =
        adsAllowed() && root.imageGenerator?.interstitial.isOn(false)

    fun videoGeneratorInterstitialEnabled(): Boolean =
        adsAllowed() && root.videoGenerator?.interstitial.isOn(false)

    fun musicGeneratorInterstitialEnabled(): Boolean =
        adsAllowed() && root.musicGenerator?.interstitial.isOn(false)

    /** All generator result screens — Home button. RC: `generate_result.home_interstitial`. */
    fun generateResultHomeInterstitialEnabled(): Boolean =
        adsAllowed() && root.generateResult?.homeInterstitial.isOn(false)

    /**
     * Image generation progress — request rewarded first.
     * RC: `generate_result.rewarded_ad`.
     */
    fun generateProgressRewardedEnabled(): Boolean =
        adsAllowed() && root.generateResult?.rewardedAd.isOn(false)

    /**
     * Image generation progress — rewarded interstitial only after rewarded fails.
     * RC: `generate_result.rewarded_interstitial_ad`.
     */
    fun generateProgressRewardedInterstitialEnabled(): Boolean =
        adsAllowed() && root.generateResult?.rewardedInterstitialAd.isOn(false)

    // --- Generator result screens (bottom native / banner) ---
    fun imageGeneratorResultBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("image_generator", "bottom", root.imageGenerator?.bottom)

    fun imageGeneratorShowResultBottomAd(): Boolean =
        imageGeneratorResultBottomAdSlotFormat().visible

    fun videoGeneratorResultBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("video_generator", "bottom", root.videoGenerator?.bottom)

    fun videoGeneratorShowResultBottomAd(): Boolean =
        videoGeneratorResultBottomAdSlotFormat().visible

    fun musicGeneratorResultBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("music_generator", "bottom", root.musicGenerator?.bottom)

    fun musicGeneratorShowResultBottomAd(): Boolean =
        musicGeneratorResultBottomAdSlotFormat().visible

    fun settingsInterstitialEnabled(): Boolean =
        adsAllowed() && root.settings?.interstitial.isOn(false)

    // --- Onboarding ---
    fun onboardingTopAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("onboarding", "top", root.onboarding?.top)

    fun onboardingBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("onboarding", "bottom", root.onboarding?.bottom)

    fun onboardingTopAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && onboardingTopAdSlotFormat().adaptiveBanner

    fun onboardingBottomAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && onboardingBottomAdSlotFormat().adaptiveBanner

    fun onboardingTopSlotEnabled(): Boolean = onboardingTopAdSlotFormat().visible

    fun onboardingBottomSlotEnabled(): Boolean = onboardingBottomAdSlotFormat().visible

    fun onboardingNativeTopEnabled(): Boolean {
        val f = onboardingTopAdSlotFormat()
        return f.visible && !f.adaptiveBanner && f.nativeTemplate != NativeAdTemplate.DISABLED
    }

    fun onboardingNativeBottomEnabled(): Boolean {
        val f = onboardingBottomAdSlotFormat()
        return f.visible && !f.adaptiveBanner && f.nativeTemplate != NativeAdTemplate.DISABLED
    }

    fun onboardingNativeTopTemplate(): NativeAdTemplate = onboardingTopAdSlotFormat().nativeTemplate

    fun onboardingNativeBottomTemplate(): NativeAdTemplate = onboardingBottomAdSlotFormat().nativeTemplate

    /** At most one onboarding ad slot; bottom wins when both are on in RC. */
    fun onboardingShowTopAdSlot(): Boolean =
        topAdVisible(onboardingTopAdSlotFormat().visible, onboardingBottomAdSlotFormat().visible)

    fun onboardingShowBottomAdSlot(): Boolean = onboardingBottomAdSlotFormat().visible

    /**
     * Permanently disabled in app code (not controlled by RC).
     * RC key `onboarding.last_screen_native_ad` is ignored.
     */
    fun onboardingLastScreenNativeAdRcEnabled(): Boolean = false

    /**
     * Onboarding page 6 (full-screen native intro). RC: `onboarding.last_screen_native_ad`
     * (`1` = show page + native, `0` = skip page 6; missing = off).
     */
    fun onboardingLastScreenNativeAdEnabled(): Boolean =
        adsAllowed() && onboardingLastScreenNativeAdRcEnabled()

    // --- Home ---
    /**
     * Resolved home ad lane — one format per vertical slot; bottom wins over top when both are on in RC.
     */
    data class HomeAdSlotFormat(
        val visible: Boolean,
        val adaptiveBanner: Boolean,
        val nativeTemplate: NativeAdTemplate,
    )

    fun hiddenAdSlotFormat(): HomeAdSlotFormat = homeAdSlotHidden()

    private fun homeAdSlotHidden(): HomeAdSlotFormat =
        HomeAdSlotFormat(
            visible = false,
            adaptiveBanner = false,
            nativeTemplate = NativeAdTemplate.DISABLED,
        )

    private fun resolveHomeBottomAdaptiveBannerOn(): Boolean {
        when (readHomeSlotIntFlag("bottom", "adaptive_banner_ad")) {
            1 -> return true
            0 -> return false
        }
        if (root.home?.bottom?.adaptiveBannerAd == 1) return true
        if (root.home?.bottomAdaptiveBanner == 1) return true
        if (
            readPremiumIntFlag(
                gsonValue = root.home?.bottom?.adaptiveBannerAd,
                pattern = homeBottomAdaptiveBannerPattern,
                defaultOn = false,
            )
        ) {
            return true
        }
        return readPremiumIntFlag(
            gsonValue = null,
            pattern = homeBottomAdaptiveBannerFlatPattern,
            defaultOn = false,
        )
    }

    private fun resolveHomeTopAdaptiveBannerOn(): Boolean {
        when (readHomeSlotIntFlag("top", "adaptive_banner_ad")) {
            1 -> return true
            0 -> return false
        }
        if (root.home?.top?.adaptiveBannerAd == 1) return true
        return readPremiumIntFlag(
            gsonValue = root.home?.top?.adaptiveBannerAd,
            pattern = homeTopAdaptiveBannerPattern,
            defaultOn = false,
        )
    }

    /** Bottom visual slot uses banner when top or bottom adaptive flag is on (banner beats native). */
    private fun resolveHomeBottomBannerOn(): Boolean =
        resolveHomeBottomAdaptiveBannerOn() || resolveHomeTopAdaptiveBannerOn()

    private fun homeBottomLaneActive(): Boolean =
        resolveHomeBottomBannerOn() || homeSlotNativeEnabled("bottom")

    /** RC + subscription only — consent is waited inside ad composables. */
    private fun homeAdsSlotAllowed(): Boolean =
        allAdsOn() && PremiumAccess.shouldShowAds()

    fun homeTopAdaptiveBannerEnabled(): Boolean =
        homeAdsSlotAllowed() && resolveHomeTopAdaptiveBannerOn()

    fun homeBottomAdaptiveBannerEnabled(): Boolean =
        homeAdsSlotAllowed() && resolveHomeBottomAdaptiveBannerOn()

    /** RC flag only — for debug logging. */
    fun homeBottomAdaptiveBannerRcOn(): Boolean = resolveHomeBottomAdaptiveBannerOn()

    fun homeTopAdaptiveBannerRcOn(): Boolean = resolveHomeTopAdaptiveBannerOn()

    fun homeBottomRcDebugSnapshot(): String {
        val top = root.home?.top
        val bottom = root.home?.bottom
        return buildString {
            append("top.adaptive=${top?.adaptiveBannerAd}")
            append(" top.nativeW=${top?.nativeAdWithMedia}")
            append(" top.nativeWo=${top?.nativeAdWithoutMedia}")
            append(" | bottom.adaptive=${bottom?.adaptiveBannerAd}")
            append(" bottom.nativeW=${bottom?.nativeAdWithMedia}")
            append(" bottom.nativeWo=${bottom?.nativeAdWithoutMedia}")
            append(" | bottomBanner=${resolveHomeBottomBannerOn()}")
        }
    }

    /** Raw `home` block from last applied `ads_control` JSON (debug). */
    fun homeAdsControlRawHomeSection(): String {
        val raw = lastAdsControlRaw ?: return "ads_control raw=null"
        val key = "\"home\""
        val start = raw.indexOf(key)
        if (start < 0) return "home key missing in ads_control"
        return raw.substring(start, minOf(start + 320, raw.length)).replace('\n', ' ')
    }

    fun homeTopSlotEnabled(): Boolean =
        homeAdsSlotAllowed() &&
            (resolveHomeTopAdaptiveBannerOn() || homeSlotNativeEnabled("top"))

    fun homeBottomSlotEnabled(): Boolean =
        homeAdsSlotAllowed() && homeBottomLaneActive()

    fun homeTopAdSlotFormat(): HomeAdSlotFormat {
        if (!homeAdsSlotAllowed()) return homeAdSlotHidden()
        if (homeBottomLaneActive()) return homeAdSlotHidden()
        if (resolveHomeTopAdaptiveBannerOn()) {
            return HomeAdSlotFormat(
                visible = true,
                adaptiveBanner = true,
                nativeTemplate = NativeAdTemplate.DISABLED,
            )
        }
        if (!homeSlotNativeEnabled("top")) return homeAdSlotHidden()
        val native =
            when {
                readHomeSlotIntFlag("top", "native_ad_with_media") == 1 ->
                    NativeAdTemplate.WITH_MEDIA
                readHomeSlotIntFlag("top", "native_ad_without_media") == 1 ->
                    NativeAdTemplate.WITHOUT_MEDIA
                else -> nativeSlotTemplate(root.home?.top)
            }
        if (native != NativeAdTemplate.DISABLED) {
            return HomeAdSlotFormat(
                visible = true,
                adaptiveBanner = false,
                nativeTemplate = native,
            )
        }
        return homeAdSlotHidden()
    }

    /**
     * @deprecated Home bottom banner is RC-controlled via [homeBottomAdSlotFormat].
     */
    @Deprecated("Use homeBottomAdSlotFormat()")
    fun homeScreenFixedBottomBannerFormat(): HomeAdSlotFormat = homeBottomAdSlotFormat()

    fun homeBottomAdSlotFormat(): HomeAdSlotFormat {
        if (!homeAdsSlotAllowed()) return homeAdSlotHidden()
        if (resolveHomeBottomBannerOn()) {
            return HomeAdSlotFormat(
                visible = true,
                adaptiveBanner = true,
                nativeTemplate = NativeAdTemplate.DISABLED,
            )
        }
        if (!homeSlotNativeEnabled("bottom")) return homeAdSlotHidden()
        val native =
            when {
                readHomeSlotIntFlag("bottom", "native_ad_with_media") == 1 ->
                    NativeAdTemplate.WITH_MEDIA
                readHomeSlotIntFlag("bottom", "native_ad_without_media") == 1 ->
                    NativeAdTemplate.WITHOUT_MEDIA
                else -> nativeSlotTemplate(root.home?.bottom)
            }
        if (native != NativeAdTemplate.DISABLED) {
            return HomeAdSlotFormat(
                visible = true,
                adaptiveBanner = false,
                nativeTemplate = native,
            )
        }
        return homeAdSlotHidden()
    }

    fun homeNativeTopTemplate(): NativeAdTemplate =
        if (adsAllowed()) nativeSlotTemplate(root.home?.top) else NativeAdTemplate.DISABLED

    fun homeNativeBottomTemplate(): NativeAdTemplate =
        if (adsAllowed()) nativeSlotTemplate(root.home?.bottom) else NativeAdTemplate.DISABLED

    /** Legacy — true when any home slot is configured. */
    fun homeNativeEnabled(): Boolean = homeTopSlotEnabled() || homeBottomSlotEnabled()

    /** Top hidden when bottom lane is on in RC (bottom wins). */
    fun homeShowTopAdSlot(): Boolean = homeTopAdSlotFormat().visible

    fun homeShowBottomAdSlot(): Boolean = homeBottomAdSlotFormat().visible

    /**
     * Interstitial before opening a social platform paste screen.
     * RC: `social_platform.interstitial` (preferred), legacy `home.social_platform_interstitial`.
     */
    fun socialPlatformInterstitialEnabled(): Boolean {
        if (!adsAllowed()) return false
        root.socialPlatform?.interstitial?.let { return it == 1 }
        return readPremiumIntFlag(
            gsonValue = root.home?.socialPlatformInterstitial,
            pattern = homeSocialPlatformInterstitialPattern,
            defaultOn = false,
        ) ||
            readSocialPlatformSectionInterstitialFlag()
    }

    private fun readSocialPlatformSectionInterstitialFlag(): Boolean {
        val fromRegex =
            lastAdsControlRaw
                ?.let { socialPlatformSectionInterstitialPattern.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
        return when (fromRegex) {
            1 -> true
            0 -> false
            else -> false
        }
    }

    /** @deprecated Use [socialPlatformInterstitialEnabled]. */
    fun homeSocialPlatformInterstitialEnabled(): Boolean = socialPlatformInterstitialEnabled()

    // --- Interstitials (screen actions) ---
    /** RC-only — `onboarding.interstitial` (`1` on, `0` off; missing = off). */
    fun onboardingLastPageInterstitialRcEnabled(): Boolean {
        when (readAdsControlNestedSectionIntFlag("onboarding", "interstitial")) {
            1 -> return true
            0 -> return false
        }
        return root.onboarding?.interstitial == 1
    }

    fun onboardingLastPageInterstitialEnabled(): Boolean =
        adsAllowed() && onboardingLastPageInterstitialRcEnabled()

    /** Permanently disabled in app code (not controlled by RC). RC key `language.done_interstitial` is ignored. */
    fun languageDoneInterstitialRcEnabled(): Boolean = false

    fun languageDoneInterstitialEnabled(): Boolean =
        adsAllowed() && languageDoneInterstitialRcEnabled()

    /** RC: `premium.close_interstitial` (`1` on, `0` off; missing = off). */
    fun premiumCloseInterstitialEnabled(): Boolean =
        adsAllowed() && root.premium?.closeInterstitial.isOn(defaultOn = false)

    /** Interstitial when user taps Download on video preview (before format selection). */
    fun videoPreviewDownloadInterstitialEnabled(): Boolean =
        adsAllowed() && root.videoPreview?.downloadInterstitial.isOn(false)

    fun reelsFullscreenNativeAdEnabled(): Boolean =
        adsAllowed() && root.reels?.fullscreenNativeAd.isOn(false)

    /** RC: `reels.back_interstitial` — same ad unit as [socialPlatformInterstitialEnabled]. */
    fun reelsBackInterstitialEnabled(): Boolean {
        if (!adsAllowed()) return false
        if (
            readPremiumIntFlag(
                gsonValue = root.reels?.backInterstitial,
                pattern = reelsBackInterstitialPattern,
                defaultOn = false,
            )
        ) {
            return true
        }
        return socialPlatformInterstitialEnabled()
    }

    fun reelsBottomAdSlotFormat(): HomeAdSlotFormat =
        nestedSlotAdFormat("reels", "bottom", root.reels?.bottom)

    /** @deprecated Use [reelsBottomAdSlotFormat]. */
    fun reelsBottomSlotEnabled(): Boolean = reelsBottomAdSlotFormat().visible

    /** @deprecated Use [reelsBottomAdSlotFormat]. */
    fun reelsNativeBottomTemplate(): NativeAdTemplate = reelsBottomAdSlotFormat().nativeTemplate

    // --- Settings ---
    fun settingsTopPremiumCardEnabled(): Boolean =
        premiumPromosEnabled() && root.settings?.topPremiumCard.isOn(true)

    fun settingsBottomNativeEnabled(): Boolean {
        if (!adsAllowed()) return false
        if (settingsBottomAdaptiveBannerEnabled()) return false
        return root.settings?.bottomNativeAd.isOn(false)
    }

    fun settingsBottomAdaptiveBannerEnabled(): Boolean =
        adsAllowed() && root.settings?.bottomAdaptiveBanner.isOn(false)

    fun settingsBottomSlotEnabled(): Boolean =
        settingsBottomAdaptiveBannerEnabled() || settingsBottomNativeEnabled()

    fun socialPlatformMediumRectangleBannerEnabled(): Boolean {
        if (!adsRcAllowed() || !PremiumAccess.shouldShowAds()) return false
        when (readAdsControlSectionIntFlag("social_platform", "medium_rectangle_banner")) {
            1 -> return true
            0 -> return false
        }
        if (root.socialPlatform?.mediumRectangleBanner == 1) return true
        if (root.socialPlatform?.mediumRectangleBanner == 0) return false
        return readPremiumIntFlag(
            gsonValue = null,
            pattern = socialPlatformMediumRectangleBannerPattern,
            defaultOn = false,
        )
    }

    /** `social_platform.native_ad` — MREC wins when both are `1`. */
    fun socialPlatformNativeEnabled(): Boolean {
        if (!adsRcAllowed() || !PremiumAccess.shouldShowAds()) return false
        if (socialPlatformMediumRectangleBannerEnabled()) return false
        when (readAdsControlSectionIntFlag("social_platform", "native_ad")) {
            1 -> return true
            0 -> return false
        }
        return root.socialPlatform?.nativeAd == 1
    }

    fun socialPlatformNativeTemplate(): NativeAdTemplate {
        if (!socialPlatformNativeEnabled()) return NativeAdTemplate.DISABLED
        when (readAdsControlSectionIntFlag("social_platform", "native_ad_with_media")) {
            1 -> return NativeAdTemplate.WITH_MEDIA
        }
        when (readAdsControlSectionIntFlag("social_platform", "native_ad_without_media")) {
            1 -> return NativeAdTemplate.WITHOUT_MEDIA
        }
        root.socialPlatform?.nativeAdWithMedia?.let { if (it == 1) return NativeAdTemplate.WITH_MEDIA }
        root.socialPlatform?.nativeAdWithoutMedia?.let { if (it == 1) return NativeAdTemplate.WITHOUT_MEDIA }
        // Legacy `native_ad: 1` — large native with media (~158dp).
        return NativeAdTemplate.WITH_MEDIA
    }

    private fun resolveSocialPlatformMediumRectangleOn(): Boolean =
        socialPlatformMediumRectangleBannerEnabled()

    fun exitScreenMediumRectangleBannerEnabled(): Boolean {
        if (!adsRcAllowed() || !PremiumAccess.shouldShowAds()) return false
        when (readAdsControlSectionIntFlag("exit", "medium_rectangle_banner")) {
            1 -> return true
            0 -> return false
        }
        if (root.exit?.mediumRectangleBanner == 1) return true
        if (root.exit?.mediumRectangleBanner == 0) return false
        return readPremiumIntFlag(
            gsonValue = null,
            pattern = exitMediumRectangleBannerPattern,
            defaultOn = false,
        )
    }

    fun exitScreenMediumNativeEnabled(): Boolean {
        if (!adsRcAllowed() || !PremiumAccess.shouldShowAds()) return false
        if (exitScreenMediumRectangleBannerEnabled()) return false
        when (readAdsControlSectionIntFlag("exit", "medium_native_ad")) {
            1 -> return true
            0 -> return false
        }
        return root.exit?.mediumNativeAd == 1
    }

    private fun resolveExitMediumRectangleOn(): Boolean = exitScreenMediumRectangleBannerEnabled()

    fun exitScreenAdaptiveBannerEnabled(): Boolean {
        if (!adsAllowed()) return false
        if (resolveExitMediumRectangleOn()) return false
        if (exitScreenMediumNativeEnabled()) return false
        when (readAdsControlSectionIntFlag("exit", "adaptive_banner_ad")) {
            1 -> return true
            0 -> return false
        }
        return root.exit?.adaptiveBannerAd.isOn(false)
    }

    // --- Premium ---
    private fun premiumPromosEnabled(): Boolean =
        !PremiumAccess.isPremiumUser() &&
            readPremiumIntFlag(
                gsonValue = root.premium?.promosEnabled,
                pattern = promosEnabledPattern,
                defaultOn = true,
            )

    fun showPremiumAfterOnboarding(): Boolean =
        premiumPromosEnabled() &&
            readPremiumIntFlag(
                gsonValue = root.premium?.showAfterOnboarding,
                pattern = showAfterOnboardingPattern,
                defaultOn = true,
            )

    fun showHomeAppBarPro(): Boolean =
        premiumPromosEnabled() && root.premium?.homeAppBarPro.isOn(true)

    /** Settings top “Go Premium” / upgrade banner — hidden for subscribers / when promos off. */
    fun showSettingsUpgradeCard(): Boolean = settingsTopPremiumCardEnabled()

    fun showHomeSocialPremiumCard(): Boolean =
        premiumPromosEnabled() && root.premium?.homeSocialPremiumCard.isOn(true)

    fun showReelsNoAdsPromo(): Boolean = showHomeSocialPremiumCard()

    /** Exit deal dialog + gift FAB + flash sale. */
    fun showPremiumExitPromo(): Boolean = premiumPromosEnabled()

    fun showPremiumExitDealDialog(): Boolean =
        premiumPromosEnabled() &&
            readPremiumIntFlag(
                gsonValue = root.premium?.exitDealDialog,
                pattern = exitDealDialogPattern,
                defaultOn = true,
            )

    fun showPremiumGiftFab(): Boolean =
        premiumPromosEnabled() &&
            readPremiumIntFlag(
                gsonValue = root.premium?.giftFab,
                pattern = giftFabPattern,
                defaultOn = true,
            )

    /**
     * RC sources (first match wins):
     * 1. `ads_control.premium_screen_test` (root of JSON blob)
     * 2. `ads_control.premium.premium_screen_test`
     * 3. Regex on raw JSON blob
     * 4. `ads_control.premium.subscription_screen_ab` (`1`→A, `2`→B, `3`→C)
     * 5. Standalone Firebase key `premium_screen_test` — only when JSON omits the field
     */
    fun premiumScreenTestFromJson(): String? {
        val raw = lastAdsControlRaw
        val fromGsonRoot = root.premiumScreenTestRoot?.trim()?.takeIf { it.isNotEmpty() }
        val fromGsonPremium = root.premium?.premiumScreenTest?.trim()?.takeIf { it.isNotEmpty() }
        val fromRegexFirst = raw?.let { premiumScreenTestFromRegexFirst(it) }
        return fromGsonRoot
            ?: fromGsonPremium
            ?: fromRegexFirst
            ?: subscriptionScreenAbTestLetter()
    }

    /** JSON blob wins; standalone Firebase `premium_screen_test` is fallback only. */
    fun premiumScreenTest(): String? =
        premiumScreenTestFromJson() ?: premiumScreenTestOverride

    /** Debug only — standalone RC param value after last apply. */
    fun premiumScreenTestOverrideForDebug(): String? = premiumScreenTestOverride

    private fun subscriptionScreenAbTestLetter(): String? {
        val fromGson =
            when (root.premium?.subscriptionScreenAb) {
                1 -> "A"
                2 -> "B"
                3 -> "C"
                else -> null
            }
        if (fromGson != null) return fromGson
        val fromRegex =
            lastAdsControlRaw
                ?.let { subscriptionScreenAbPattern.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
        return when (fromRegex) {
            1 -> "A"
            2 -> "B"
            3 -> "C"
            else -> null
        }
    }

    fun subscriptionScreenAbVariant(): Int =
        when (premiumScreenTest()?.uppercase()) {
            "A" -> 1
            "B" -> 2
            "C" -> 3
            else -> 0
        }

    /** Gson first; regex on raw `ads_control` JSON catches values Gson missed. */
    private fun readPremiumIntFlag(
        gsonValue: Int?,
        pattern: Regex,
        defaultOn: Boolean,
    ): Boolean {
        val fromRegex =
            lastAdsControlRaw
                ?.let { pattern.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
        return when (fromRegex) {
            1 -> true
            0 -> false
            else -> gsonValue.isOn(defaultOn = defaultOn)
        }
    }

    /** One vertical ad at a time; if both slots are enabled in RC, show bottom only. */
    private fun topAdVisible(topSlotEnabled: Boolean, bottomSlotEnabled: Boolean): Boolean =
        topSlotEnabled && !bottomSlotEnabled

    /** Slot on when at least one format flag is `1`. */
    private fun nativeSlotEnabled(slot: NativeAdSlotConfig?): Boolean {
        if (slot == null) return false
        return slot.nativeAdWithMedia.isOn(defaultOn = false) ||
            slot.nativeAdWithoutMedia.isOn(defaultOn = false)
    }

    private fun nativeSlotTemplate(slot: NativeAdSlotConfig?): NativeAdTemplate =
        resolveNativeTemplate(
            sectionEnabled = nativeSlotEnabled(slot),
            withMedia = slot?.nativeAdWithMedia,
            withoutMedia = slot?.nativeAdWithoutMedia,
        )

    private fun resolveNativeTemplate(
        sectionEnabled: Boolean,
        withMedia: Int?,
        withoutMedia: Int?,
        defaultWithoutOn: Boolean = false,
    ): NativeAdTemplate {
        if (!sectionEnabled) return NativeAdTemplate.DISABLED
        val with = withMedia.isOn(defaultOn = false)
        val without = withoutMedia.isOn(defaultOn = defaultWithoutOn)
        return when {
            with && without -> NativeAdTemplate.WITH_MEDIA
            with -> NativeAdTemplate.WITH_MEDIA
            without -> NativeAdTemplate.WITHOUT_MEDIA
            else -> NativeAdTemplate.DISABLED
        }
    }
}

/**
 * Per-slot ad toggles under `language.top` / `language.bottom` (and splash native-only slots).
 * `adaptive_banner_ad`: anchored adaptive banner when `1` (no native fallback on failure).
 */
private data class NativeAdSlotConfig(
    @SerializedName("adaptive_banner_ad") val adaptiveBannerAd: Int? = null,
    @SerializedName("native_ad_with_media") val nativeAdWithMedia: Int? = null,
    @SerializedName("native_ad_without_media") val nativeAdWithoutMedia: Int? = null,
)

private data class AdsControlRoot(
    @SerializedName("premium_screen_test") val premiumScreenTestRoot: String? = null,
    @SerializedName("all_ads_On_Off") val allAdsOnOff: Int? = null,
    @SerializedName("ads_debug_log") val adsDebugLog: Int? = null,
    @SerializedName("app_open_resume") val appOpenResume: Int? = null,
    @SerializedName("privacy_policy_url") val privacyPolicyUrl: String? = null,
    @SerializedName("terms_and_conditions_url") val termsAndConditionsUrl: String? = null,
    @SerializedName("splash") val splash: SplashSection? = null,
    @SerializedName("language") val language: LanguageSection? = null,
    @SerializedName("onboarding") val onboarding: OnboardingSection? = null,
    @SerializedName("home") val home: HomeSection? = null,
    @SerializedName("library") val library: LibrarySection? = null,
    @SerializedName("wallpaper") val wallpaper: WallpaperSection? = null,
    @SerializedName("image_generator") val imageGenerator: FeatureInterstitialSection? = null,
    @SerializedName("video_generator") val videoGenerator: FeatureInterstitialSection? = null,
    @SerializedName("music_generator") val musicGenerator: FeatureInterstitialSection? = null,
    @SerializedName("generate_result") val generateResult: GenerateResultSection? = null,
    @SerializedName("reels") val reels: ReelsSection? = null,
    @SerializedName("settings") val settings: SettingsSection? = null,
    @SerializedName("social_platform") val socialPlatform: SocialPlatformSection? = null,
    @SerializedName("exit") val exit: ExitSection? = null,
    @SerializedName("premium") val premium: PremiumSection? = null,
    @SerializedName("video_preview") val videoPreview: VideoPreviewSection? = null,
    @SerializedName("native_ad_cta") val nativeAdCta: NativeAdCtaRoot? = null,
)

private data class SplashSection(
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
    @SerializedName("interstitial") val interstitial: Int? = null,
    @SerializedName("app_open_ad") val appOpenAd: Int? = null,
)

private data class LanguageSection(
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
    @SerializedName("done_interstitial") val doneInterstitial: Int? = null,
    @SerializedName("top_premium_card") val topPremiumCard: Int? = null,
)

private data class OnboardingSection(
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
    @SerializedName("interstitial") val interstitial: Int? = null,
    @SerializedName("last_screen_native_ad") val lastScreenNativeAd: Int? = null,
)

private data class HomeSection(
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
    @SerializedName("bottom_adaptive_banner") val bottomAdaptiveBanner: Int? = null,
    @SerializedName("social_platform_interstitial") val socialPlatformInterstitial: Int? = null,
)

private data class ReelsSection(
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
    @SerializedName("fullscreen_native_ad") val fullscreenNativeAd: Int? = null,
    @SerializedName("back_interstitial") val backInterstitial: Int? = null,
)

private data class SettingsSection(
    @SerializedName("top_premium_card") val topPremiumCard: Int? = null,
    @SerializedName("bottom_native_ad") val bottomNativeAd: Int? = null,
    @SerializedName("bottom_adaptive_banner") val bottomAdaptiveBanner: Int? = null,
    @SerializedName("interstitial") val interstitial: Int? = null,
)

private data class LibrarySection(
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
)

private data class WallpaperSection(
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
)

private data class FeatureInterstitialSection(
    @SerializedName("interstitial") val interstitial: Int? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
)

private data class GenerateResultSection(
    @SerializedName("home_interstitial") val homeInterstitial: Int? = null,
    /** Image progress: request rewarded first. */
    @SerializedName("rewarded_ad") val rewardedAd: Int? = null,
    /** Image progress: rewarded interstitial only if rewarded fails. */
    @SerializedName("rewarded_interstitial_ad") val rewardedInterstitialAd: Int? = null,
    /** Progress screen (image / video / music / cover) — top + bottom like language. */
    @SerializedName("top") val top: NativeAdSlotConfig? = null,
    @SerializedName("bottom") val bottom: NativeAdSlotConfig? = null,
)

private data class SocialPlatformSection(
    @SerializedName("native_ad") val nativeAd: Int? = null,
    @SerializedName("native_ad_with_media") val nativeAdWithMedia: Int? = null,
    @SerializedName("native_ad_without_media") val nativeAdWithoutMedia: Int? = null,
    @SerializedName("medium_rectangle_banner") val mediumRectangleBanner: Int? = null,
    @SerializedName("interstitial") val interstitial: Int? = null,
)

private data class ExitSection(
    @SerializedName("medium_native_ad") val mediumNativeAd: Int? = null,
    @SerializedName("medium_rectangle_banner") val mediumRectangleBanner: Int? = null,
    @SerializedName("adaptive_banner_ad") val adaptiveBannerAd: Int? = null,
)

private data class PremiumSection(
    @SerializedName("promos_enabled") val promosEnabled: Int? = null,
    @SerializedName("show_after_onboarding") val showAfterOnboarding: Int? = null,
    @SerializedName("close_interstitial") val closeInterstitial: Int? = null,
    @SerializedName("home_app_bar_pro") val homeAppBarPro: Int? = null,
    @SerializedName("home_social_premium_card") val homeSocialPremiumCard: Int? = null,
    @SerializedName("exit_deal_dialog") val exitDealDialog: Int? = null,
    @SerializedName("gift_fab") val giftFab: Int? = null,
    @SerializedName("subscription_screen_ab") val subscriptionScreenAb: Int? = null,
    @SerializedName("premium_screen_test") val premiumScreenTest: String? = null,
)

private data class VideoPreviewSection(
    @SerializedName("download_interstitial") val downloadInterstitial: Int? = null,
)
