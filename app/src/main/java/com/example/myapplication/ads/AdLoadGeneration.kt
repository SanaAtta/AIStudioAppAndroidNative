package com.example.myapplication.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Per-slot load generation — incremented when the composable leaves composition so async
 * AdMob callbacks from a disposed [androidx.compose.ui.viewinterop.AndroidView] are ignored.
 */
@Composable
fun rememberAdLoadGeneration(): MutableIntState {
    val generation = remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        onDispose { generation.intValue++ }
    }
    return generation
}

fun adLoadGenerationCurrent(
    generationAtCreate: Int,
    generation: MutableIntState,
): Boolean = generationAtCreate == generation.intValue

/** Survives Compose leave/re-enter — used to detect remount-driven duplicate native requests. */
private val nativeSlotMountCounts = ConcurrentHashMap<String, AtomicInteger>()

private fun nativeSlotMountKey(slotLabel: String, unitId: String): String = "$slotLabel|$unitId"

/**
 * Call immediately before each native [AdLoader.loadAd].
 * Logs REMOUNT when this slot has already sent a request earlier in the app process.
 */
fun logNativeMountRequest(
    slotLabel: String,
    rcParam: String,
    unitId: String,
) {
    val mountCount =
        nativeSlotMountCounts
            .getOrPut(nativeSlotMountKey(slotLabel, unitId)) { AtomicInteger(0) }
            .incrementAndGet()
    if (mountCount > 1) {
        AdsLoadLog.nativeRemount(slotLabel, rcParam, unitId, mountCount)
    } else {
        AdsLoadLog.nativeInitialMount(slotLabel, rcParam, unitId)
    }
    AdsLoadLog.requesting(slotLabel, rcParam, unitId)
}
