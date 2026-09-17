package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig

/**
 * Ad pipeline logs — Logcat filter: **`AdsLoad`**
 *
 * Always prints in debug builds. Release: Firebase `ads_control.ads_debug_log: 1`.
 *
 * Example:
 * `LOADED  | Interstitial | generate result home interstitial | screen=result | unit=ca-app-pub-...`
 */
object AdsLoadLog {
    const val TAG = "AdsLoad"

    /** Kept so existing banner filters still work; same lines also go to [TAG]. */
    const val BANNER_TAG = "AdsLoadBanner"

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

    private fun enabled(): Boolean = true

    private fun adSlotLabel(slot: String): String =
        if (slot.endsWith(" ad")) slot else "$slot ad"

    private fun adType(slot: String, rcParam: String): String {
        val hay = "$slot $rcParam".lowercase()
        return when {
            hay.contains("rewarded interstitial") -> "RewardedInterstitial"
            hay.contains("rewarded") -> "Rewarded"
            hay.contains("interstitial") -> "Interstitial"
            hay.contains("app open") || hay.contains("app_open") -> "AppOpen"
            hay.contains("native") -> "Native"
            hay.contains("mrec") || hay.contains("medium rectangle") || hay.contains("300x250") -> "MREC"
            hay.contains("banner") || hay.contains("adaptive") -> "Banner"
            else -> "Ad"
        }
    }

    private fun resolveScreen(slot: String, rcParam: String): String {
        val hay = "$slot $rcParam".lowercase()
        return when {
            hay.contains("splash") -> "splash"
            hay.contains("language") -> "language"
            hay.contains("onboarding") -> "onboarding"
            hay.contains("settings") -> "settings"
            hay.contains("generating") || hay.contains("generate progress") -> "generating"
            hay.contains("generate result") || hay.contains("image result") ||
                hay.contains("video result") || hay.contains("music result") -> "result"
            hay.contains("image generator") || hay.contains("image_generator") -> "image_generator"
            hay.contains("video generator") || hay.contains("video_generator") -> "video_generator"
            hay.contains("music generator") || hay.contains("music_generator") -> "music_generator"
            hay.contains("ai edit") || hay.contains("ai_edit") ||
                (hay.contains("edit") && hay.contains("native")) -> "ai_edit"
            hay.contains("cover") -> "ai_cover"
            hay.contains("wallpaper") -> "wallpaper"
            hay.contains("library") || hay.contains("projects") -> "library"
            hay.contains("chat") -> "chat"
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

    private fun statusLabel(event: Event): String = when (event) {
        Event.REQUEST -> "REQUESTING"
        Event.LOADED -> "LOADED"
        Event.SHOW -> "SHOWN"
        Event.FAILED -> "FAILED"
        Event.SHOW_FAILED -> "SHOW_FAILED"
        Event.CLOSED -> "CLOSED"
        Event.TRACE -> "INFO"
        Event.REMOUNT -> "REMOUNT"
    }

    private fun line(
        screen: String,
        event: Event,
        slot: String,
        rcParam: String,
        unitId: String,
        detail: String = "",
    ): String = buildString {
        append(statusLabel(event).padEnd(12))
        append(" | ")
        append(adType(slot, rcParam).padEnd(22))
        append(" | ")
        append(adSlotLabel(slot))
        append(" | screen=")
        append(screen)
        if (rcParam.isNotBlank()) {
            append(" | rc=")
            append(rcParam)
        }
        if (unitId.isNotBlank()) {
            append(" | unit=")
            append(unitId)
        }
        if (detail.isNotBlank()) {
            append(" | ")
            append(detail)
        }
    }

    private fun emit(event: Event, message: String, banner: Boolean) {
        if (!enabled()) return
        when (event) {
            Event.FAILED, Event.SHOW_FAILED -> {
                Log.e(TAG, message)
                if (banner) Log.e(BANNER_TAG, message)
            }
            Event.REMOUNT -> {
                Log.w(TAG, message)
                if (banner) Log.w(BANNER_TAG, message)
            }
            else -> {
                Log.i(TAG, message)
                if (banner) Log.i(BANNER_TAG, message)
            }
        }
    }

    private fun logLine(
        slot: String,
        rcParam: String,
        unitId: String,
        event: Event,
        detail: String = "",
        banner: Boolean = false,
    ) {
        emit(
            event,
            line(resolveScreen(slot, rcParam), event, slot, rcParam, unitId, detail),
            banner,
        )
    }

    fun bannerRequesting(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.REQUEST, banner = true)
    }

    fun bannerLoaded(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.LOADED, banner = true)
    }

    fun bannerShown(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.SHOW, banner = true)
    }

    fun bannerFailed(slot: String, rcParam: String, unitId: String, error: LoadAdError) {
        logLine(
            slot,
            rcParam,
            unitId,
            Event.FAILED,
            detail = "code=${error.code} message=${error.message}",
            banner = true,
        )
    }

    /** Pre-request failures (init timeout, shouldShowAds=false, blank unit). */
    fun bannerSkipped(slot: String, rcParam: String, unitId: String, reason: String) {
        logLine(slot, rcParam, unitId, Event.FAILED, detail = reason, banner = true)
    }

    fun bannerTrace(slot: String, rcParam: String, unitId: String, detail: String) {
        logLine(slot, rcParam, unitId, Event.TRACE, detail = detail, banner = true)
    }

    fun loaded(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.LOADED)
    }

    fun requesting(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.REQUEST)
    }

    /**
     * First time this native slot sends an AdMob request in this app process.
     */
    fun nativeInitialMount(
        slot: String,
        rcParam: String,
        unitId: String,
    ) {
        logLine(
            slot,
            rcParam,
            unitId,
            Event.TRACE,
            detail = "MOUNT mountCount=1 adsConfigRev=${AdsConfigRevision.state.value} -> native AdMob request",
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
        logLine(
            slot,
            rcParam,
            unitId,
            Event.REMOUNT,
            detail =
                "mountCount=$mountCount adsConfigRev=${AdsConfigRevision.state.value} " +
                    "-> new native AdMob request (remount)",
        )
    }

    fun trace(slot: String, message: String) {
        logLine(slot, "", "", Event.TRACE, detail = message)
    }

    fun shown(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.SHOW)
    }

    fun failed(slot: String, rcParam: String, unitId: String, error: LoadAdError) {
        logLine(
            slot,
            rcParam,
            unitId,
            Event.FAILED,
            detail = "code=${error.code} message=${error.message}",
        )
    }

    fun showedAndClosed(slot: String, rcParam: String, unitId: String) {
        logLine(slot, rcParam, unitId, Event.CLOSED)
    }

    fun showFailed(slot: String, rcParam: String, unitId: String, error: AdError) {
        logLine(
            slot,
            rcParam,
            unitId,
            Event.SHOW_FAILED,
            detail = "code=${error.code} message=${error.message}",
        )
    }

    fun skipped(slot: String, rcParam: String, unitId: String, reason: String) {
        logLine(slot, rcParam, unitId, Event.FAILED, detail = reason)
    }
}
