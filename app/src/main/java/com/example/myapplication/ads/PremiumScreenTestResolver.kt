package com.example.myapplication.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.example.myapplication.util.FirebaseBootstrap

/**
 * Reads `premium_screen_test` from activated Firebase RC on every paywall open.
 *
 * Primary blob: **`ads_control`** (debug + release). Fallback: `ads_control_debug` when primary omits the field.
 * Standalone Firebase param `premium_screen_test` is used only when the JSON blob omits it.
 */
object PremiumScreenTestResolver {
    private const val RC_PREMIUM_SCREEN_TEST = "premium_screen_test"

    /** `A`, `B`, `C`, or `null` → sticky local assignment. */
    fun resolve(): String? {
        applyToAdsControl()
        return normalize(AdsControl.premiumScreenTest())
    }

    fun applyToAdsControl() {
        if (!FirebaseBootstrap.isReady()) {
            FirebaseBootstrap.awaitReadyBlocking(timeoutMs = 5_000L)
        }
        if (!FirebaseBootstrap.isReady()) return
        val rc = FirebaseRemoteConfig.getInstance()
        val secondaryKey = AdsControl.RC_KEY_ADS_CONTROL_DEBUG

        // Prefer `ads_control`, fall back to `ads_config` (common Firebase naming mistake).
        val primaryBlob = AdsControl.readAdsControlBlob(rc)
        if (primaryBlob.isNotEmpty()) {
            AdsControl.applyAdsControlJson(primaryBlob)
            AdsControl.extractPremiumScreenTestFromBlob(primaryBlob)?.let {
                AdsControl.setPremiumScreenTestFromRemote(it)
            }
        }

        if (AdsControl.premiumScreenTestFromJson().isNullOrBlank()) {
            val secondaryBlob = rc.getString(secondaryKey).trim()
            AdsControl.extractPremiumScreenTestFromBlob(secondaryBlob)?.let {
                AdsControl.setPremiumScreenTestFromRemote(it)
            }
        }

        val jsonResolved = AdsControl.premiumScreenTestFromJson()
        val standalone = rc.getString(RC_PREMIUM_SCREEN_TEST).trim()
        if (jsonResolved.isNullOrBlank()) {
            AdsControl.setPremiumScreenTestOverride(standalone.takeIf { it.isNotEmpty() })
        } else {
            // `ads_control` JSON wins — clear stale standalone override (e.g. Firebase A/B on release).
            AdsControl.setPremiumScreenTestOverride(null)
        }
    }

    private fun normalize(raw: String?): String? {
        val trimmed = raw?.trim()?.uppercase() ?: return null
        return when (trimmed) {
            "A", "B", "C" -> trimmed
            "1" -> "A"
            "2" -> "B"
            "3" -> "C"
            "DEFAULT", "" -> null
            else -> null
        }
    }
}
