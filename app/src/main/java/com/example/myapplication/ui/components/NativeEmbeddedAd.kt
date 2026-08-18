package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ads.MobileAdsInitializer
import com.example.myapplication.ads.NativeAdTemplate
import kotlinx.coroutines.delay

/** Reported while a native placement is loading, shown, or hidden after failure. */
enum class NativeAdLoadState {
    Loading,
    Loaded,
    Failed,
}

/** Reserved heights to avoid layout jump while native ads load or fail. */
object NativeAdSlotHeights {
    val WithoutMedia = 120.dp
    val WithMedia = 158.dp
}

/**
 * Single entry for embedded native ads — picks layout from [template] (Remote Config).
 */
@Composable
fun NativeEmbeddedAd(
    unitId: String,
    template: NativeAdTemplate,
    slotLabel: String,
    rcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 5.dp,
    onLoadStateChanged: ((NativeAdLoadState) -> Unit)? = null,
) {
    when (template) {
        NativeAdTemplate.DISABLED -> Unit
        NativeAdTemplate.WITHOUT_MEDIA ->
            NativeSmallEmbeddedAd(
                unitId = unitId,
                slotLabel = slotLabel,
                rcParam = rcParam,
                modifier = modifier,
                horizontalContentPadding = horizontalContentPadding,
                onLoadStateChanged = onLoadStateChanged,
            )
        NativeAdTemplate.WITH_MEDIA ->
            NativeWithMediaEmbeddedAd(
                unitId = unitId,
                slotLabel = slotLabel,
                rcParam = rcParam,
                modifier = modifier,
                horizontalContentPadding = horizontalContentPadding,
                onLoadStateChanged = onLoadStateChanged,
            )
    }
}

/**
 * Wraps [NativeEmbeddedAd] with a stable minimum height when the slot is enabled.
 */
@Composable
fun NativeEmbeddedAdSlot(
    enabled: Boolean,
    template: NativeAdTemplate,
    unitId: String,
    slotLabel: String,
    rcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 5.dp,
    /** Stagger loads when multiple slots are on screen (e.g. splash top + bottom). */
    loadDelayMillis: Long = 0L,
) {
    if (!enabled || template == NativeAdTemplate.DISABLED || unitId.isBlank()) return

    val minHeight =
        when (template) {
            NativeAdTemplate.WITHOUT_MEDIA -> NativeAdSlotHeights.WithoutMedia
            NativeAdTemplate.WITH_MEDIA -> NativeAdSlotHeights.WithMedia
            NativeAdTemplate.DISABLED -> 0.dp
        }

    var mayLoad by remember(enabled, unitId, loadDelayMillis) { mutableStateOf(false) }
    LaunchedEffect(enabled, unitId, loadDelayMillis) {
        if (!enabled || unitId.isBlank()) {
            mayLoad = false
            return@LaunchedEffect
        }
        MobileAdsInitializer.awaitReady()
        if (loadDelayMillis > 0L) delay(loadDelayMillis)
        mayLoad = true
    }

    Box(modifier = modifier.heightIn(min = minHeight)) {
        if (mayLoad) {
            NativeEmbeddedAd(
                unitId = unitId,
                template = template,
                slotLabel = slotLabel,
                rcParam = rcParam,
                horizontalContentPadding = horizontalContentPadding,
            )
        }
    }
}
