package com.aiartgenerator.imagegenerator.videogenerator.billing

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FreeUsageKind(
    val prefsKey: String,
    val freeLimit: Int,
) {
    Image("free_usage_image", 3),
    Video("free_usage_video", 1),
    Music("free_usage_music", 1),
    Cover("free_usage_cover", 1),
    Edit("free_usage_edit", 2),
}

data class FreeUsageSnapshot(
    val image: Int = 0,
    val video: Int = 0,
    val music: Int = 0,
    val cover: Int = 0,
    val edit: Int = 0,
) {
    fun used(kind: FreeUsageKind): Int = when (kind) {
        FreeUsageKind.Image -> image
        FreeUsageKind.Video -> video
        FreeUsageKind.Music -> music
        FreeUsageKind.Cover -> cover
        FreeUsageKind.Edit -> edit
    }

    fun withUsed(kind: FreeUsageKind, value: Int): FreeUsageSnapshot = when (kind) {
        FreeUsageKind.Image -> copy(image = value)
        FreeUsageKind.Video -> copy(video = value)
        FreeUsageKind.Music -> copy(music = value)
        FreeUsageKind.Cover -> copy(cover = value)
        FreeUsageKind.Edit -> copy(edit = value)
    }
}

/**
 * Per-feature free-use counters. Premium users skip the cap.
 * Counts persist in [PremiumStatus] prefs (`app_prefs`) and survive process death.
 */
object FreeUsageLimits {
    private const val PREFS_NAME = "app_prefs"

    private lateinit var appContext: Context
    private val lock = Any()
    private val _usage = MutableStateFlow(FreeUsageSnapshot())
    val usageFlow: StateFlow<FreeUsageSnapshot> = _usage.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        _usage.value = readSnapshot()
    }

    fun canUse(kind: FreeUsageKind): Boolean {
        if (PremiumAccess.isPremiumUser()) return true
        return used(kind) < kind.freeLimit
    }

    fun used(kind: FreeUsageKind): Int = _usage.value.used(kind)

    fun recordSuccessfulUse(kind: FreeUsageKind) {
        if (!::appContext.isInitialized) return
        if (PremiumAccess.isPremiumUser()) return
        synchronized(lock) {
            val current = readSnapshot()
            val nextUsed = (current.used(kind) + 1).coerceAtMost(kind.freeLimit)
            if (nextUsed == current.used(kind)) {
                _usage.value = current
                return
            }
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(kind.prefsKey, nextUsed)
                .commit()
            _usage.value = current.withUsed(kind, nextUsed)
        }
    }

    private fun readSnapshot(): FreeUsageSnapshot {
        if (!::appContext.isInitialized) return FreeUsageSnapshot()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FreeUsageSnapshot(
            image = prefs.getInt(FreeUsageKind.Image.prefsKey, 0).coerceAtLeast(0),
            video = prefs.getInt(FreeUsageKind.Video.prefsKey, 0).coerceAtLeast(0),
            music = prefs.getInt(FreeUsageKind.Music.prefsKey, 0).coerceAtLeast(0),
            cover = prefs.getInt(FreeUsageKind.Cover.prefsKey, 0).coerceAtLeast(0),
            edit = prefs.getInt(FreeUsageKind.Edit.prefsKey, 0).coerceAtLeast(0),
        )
    }
}
