package com.example.myapplication.ads

import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.example.myapplication.BuildConfig

/**
 * Ad pipeline logs.
 *
 * Enabled when **debug build** ([BuildConfig.DEBUG]) or Firebase `ads_control.ads_debug_log: 1`.
 *
 * Logcat filters:
 * - **`AdsLoadBanner`** — banner REQUEST / LOADED / SHOW / FAILED (adaptive, large, MREC)
 * - **`AdsLoad`** — native, interstitial, app-open, slot traces, **REMOUNT** (native remount → new request)
 * Screen filter example: `AdsLoadBanner | screen=home`
 */
object AdsLoadLog {
    private const val TAG = "AdsLoad"

    /** Single logcat tag — filter this to see all banner load / show / fail lines. */
    const val BANNER_TAG = "AdsLoadBanner"

    private const val BANNER_LOG_TAG = BANNER_TAG

    enum class Event {
        REQUEST,
        LOADED,
        SHOW,
        FAILED,
        SHOW_FAILED,
        CLOSED,
        TRACE,
        REMOUNT,
    }

    private fun enabled(): Boolean = BuildConfig.DEBUG || AdsControl.adsDebugLogEnabled()

    private fun adSlotLabel(slot: String): String =
        if (slot.endsWith(" ad")) slot else "$slot ad"

    private fun resolveScreen(slot: String, rcParam: String): String {
        val hay = "$slot $rcParam".lowercase()
        return when {
            hay.contains("splash") -> "splash"
            hay.contains("language") -> "language"
            hay.contains("onboarding") -> "onboarding"
            hay.contains("settings") -> "settings"
            hay.contains("home") -> "home"
            hay.contains("reels") || hay.contains("shorts") -> "reels"
            hay.contains("exit") -> "exit"
            hay.contains("social") || hay.contains("platform") -> "social"
            hay.contains("video preview") || hay.contains("preview") -> "video_preview"
            hay.contains("resume") -> "resume"
            hay.contains("app open") -> "app_open"
            hay.contains("premium") || hay.contains("paywall") -> "premium"
            else -> "other"
        }
    }

    private fun line(
        screen: String,
        event: Event,
        slot: String,
        rcParam: String,
        unitId: String,
        detail: String = "",
    ): String {
        val base =
            buildString {
                append("| screen=")
                append(screen)
                append(" | event=")
                append(event.name)
                append(" | slot=")
                append(adSlotLabel(slot))
                if (rcParam.isNotBlank()) {
                    append(" | rc=")
                    append(rcParam)
                }
                if (unitId.isNotBlank()) {
                    append(" | unitId=")
                    append(unitId)
                }
                if (detail.isNotBlank()) {
                    append(" | ")
                    append(detail)
                }
            }
        return base
    }

    private fun logDebug(message: String) {
        if (!enabled()) return
        Log.d(TAG, message)
    }

    private fun logWarn(message: String) {
        if (!enabled()) return
        Log.w(TAG, message)
    }

    private fun logBannerDebug(message: String) {
        if (!enabled()) return
        Log.i(BANNER_LOG_TAG, message)
    }

    private fun logBannerWarn(message: String) {
        if (!enabled()) return
        Log.w(BANNER_LOG_TAG, message)
    }

    fun bannerRequesting(slot: String, rcParam: String, unitId: String) {
        logBannerDebug(
            line(resolveScreen(slot, rcParam), Event.REQUEST, slot, rcParam, unitId),
        )
    }

    fun bannerLoaded(slot: String, rcParam: String, unitId: String) {
        logBannerDebug(
            line(resolveScreen(slot, rcParam), Event.LOADED, slot, rcParam, unitId),
        )
    }

    fun bannerShown(slot: String, rcParam: String, unitId: String) {
        logBannerDebug(
            line(resolveScreen(slot, rcParam), Event.SHOW, slot, rcParam, unitId),
        )
    }

    fun bannerFailed(slot: String, rcParam: String, unitId: String, error: LoadAdError) {
        logBannerWarn(
            line(
                resolveScreen(slot, rcParam),
                Event.FAILED,
                slot,
                rcParam,
                unitId,
                detail = "code=${error.code} message=${error.message}",
            ),
        )
    }

    /** Pre-request failures (init timeout, shouldShowAds=false, blank unit). */
    fun bannerSkipped(slot: String, rcParam: String, unitId: String, reason: String) {
        logBannerWarn(
            line(
                resolveScreen(slot, rcParam),
                Event.FAILED,
                slot,
                rcParam,
                unitId,
                detail = reason,
            ),
        )
    }

    fun bannerTrace(slot: String, rcParam: String, unitId: String, detail: String) {
        logBannerDebug(
            line(resolveScreen(slot, rcParam), Event.TRACE, slot, rcParam, unitId, detail = detail),
        )
    }

    fun loaded(slot: String, rcParam: String, unitId: String) {
        logDebug(line(resolveScreen(slot, rcParam), Event.LOADED, slot, rcParam, unitId))
    }

    fun requesting(slot: String, rcParam: String, unitId: String) {
        logDebug(line(resolveScreen(slot, rcParam), Event.REQUEST, slot, rcParam, unitId))
    }

    /**
     * First time this native slot sends an AdMob request in this app process.
     */
    fun nativeInitialMount(
        slot: String,
        rcParam: String,
        unitId: String,
    ) {
        logDebug(
            line(
                resolveScreen(slot, rcParam),
                Event.TRACE,
                slot,
                rcParam,
                unitId,
                detail =
                    "MOUNT mountCount=1 adsConfigRev=${AdsConfigRevision.state.value} " +
                        "-> native AdMob request",
            ),
        )
    }

    /**
     * Slot mounted again after a prior request — filter logcat: `AdsLoad` + `REMOUNT`.
     */
    fun nativeRemount(
        slot: String,
        rcParam: String,
        unitId: String,
        mountCount: Int,
    ) {
        logWarn(
            line(
                resolveScreen(slot, rcParam),
                Event.REMOUNT,
                slot,
                rcParam,
                unitId,
                detail =
                    "mountCount=$mountCount adsConfigRev=${AdsConfigRevision.state.value} " +
                        "-> new native AdMob request (remount)",
            ),
        )
    }

    fun trace(slot: String, message: String) {
        logDebug(line(resolveScreen(slot, ""), Event.TRACE, slot, "", "", detail = message))
    }

    fun shown(slot: String, rcParam: String, unitId: String) {
        logDebug(line(resolveScreen(slot, rcParam), Event.SHOW, slot, rcParam, unitId))
    }

    fun failed(slot: String, rcParam: String, unitId: String, error: LoadAdError) {
        logWarn(
            line(
                resolveScreen(slot, rcParam),
                Event.FAILED,
                slot,
                rcParam,
                unitId,
                detail = "code=${error.code} message=${error.message}",
            ),
        )
    }

    fun showedAndClosed(slot: String, rcParam: String, unitId: String) {
        logDebug(line(resolveScreen(slot, rcParam), Event.CLOSED, slot, rcParam, unitId))
    }

    fun showFailed(slot: String, rcParam: String, unitId: String, error: AdError) {
        logWarn(
            line(
                resolveScreen(slot, rcParam),
                Event.SHOW_FAILED,
                slot,
                rcParam,
                unitId,
                detail = "code=${error.code} message=${error.message}",
            ),
        )
    }
}
