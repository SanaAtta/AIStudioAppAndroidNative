package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.content.Context
import com.google.android.gms.ads.AdSize

/**
 * Google’s documented **sample** ad unit IDs (safe for development only).
 *
 * Debug: all slots use these unless `admob.use.production.units=true` in `local.properties`.
 * Release builds must use real IDs from Firebase **`ad_ids`** only.
 */
object AdMobGoogleTestUnits {
    const val NATIVE_ADVANCED = "ca-app-pub-3940256099942544/2247696110"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    /** Shared banner / adaptive / MREC test unit for all banner slots in debug. */
    const val MEDIUM_RECTANGLE = "ca-app-pub-3940256099942544/2435281174"
    /** Google sample unit for fixed BANNER / LARGE_BANNER (320x50, 320x100). */
    const val STANDARD_BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val ADAPTIVE_BANNER = MEDIUM_RECTANGLE
    const val BANNER = MEDIUM_RECTANGLE

    fun isMediumRectangleUnit(unitId: String): Boolean = unitId == MEDIUM_RECTANGLE

    /** Banner-format unit for [AdSize.LARGE_BANNER] loads (MREC test id is the wrong format). */
    fun largeBannerLoadUnitId(unitId: String): String =
        if (isMediumRectangleUnit(unitId)) STANDARD_BANNER else unitId

    /** Fixed 320x100 dp banner ([AdSize.LARGE_BANNER]). */
    fun resolvedLargeBannerAdSize(): AdSize = AdSize.LARGE_BANNER

    /** [widthDp] = ad slot width in dp (matches Google Compose banner samples). */
    fun resolvedBannerAdSize(context: Context, unitId: String, widthDp: Int): AdSize =
        if (isMediumRectangleUnit(unitId)) {
            AdSize.MEDIUM_RECTANGLE
        } else {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                widthDp.coerceAtLeast(320),
            )
        }
}
