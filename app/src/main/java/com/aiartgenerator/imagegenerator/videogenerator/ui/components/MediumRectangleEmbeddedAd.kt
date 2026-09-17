package com.aiartgenerator.imagegenerator.videogenerator.ui.components

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsLoadLog
import com.aiartgenerator.imagegenerator.videogenerator.ads.MobileAdsInitializer
import com.aiartgenerator.imagegenerator.videogenerator.ads.rememberAdsAllowed
import com.aiartgenerator.imagegenerator.videogenerator.ads.adLoadGenerationCurrent
import com.aiartgenerator.imagegenerator.videogenerator.ads.rememberAdLoadGeneration
import com.aiartgenerator.imagegenerator.videogenerator.ui.theme.BrandPalette
import kotlinx.coroutines.delay

/** Standard AdMob medium rectangle height (300×250 dp). */
val MediumRectangleSlotHeight = 250.dp

private val MediumRectangleSlotShape = RoundedCornerShape(10.dp)
private val MediumRectangleSlotBorderColor = BrandPalette.ProStart.copy(alpha = 0.35f)

/**
 * Fixed-size medium rectangle banner (MREC) for social platform paste screens.
 * Shows [MediumRectangleAdLoadingShimmer] until the ad loads.
 */
@Composable
fun MediumRectangleEmbeddedAd(
    unitId: String,
    modifier: Modifier = Modifier,
    slotLabel: String = "medium rectangle banner",
    rcParam: String = "",
    onLoaded: () -> Unit = {},
    onFailed: () -> Unit = {},
) {
    if (unitId.isBlank()) {
        LaunchedEffect(Unit) { onFailed() }
        return
    }

    var readyToLoad by remember(unitId) { mutableStateOf(false) }
    val slotVisible = remember(unitId) { mutableStateOf(true) }
    val showShimmer = remember(unitId) { mutableStateOf(true) }

    LaunchedEffect(unitId) {
        if (!MobileAdsInitializer.awaitReady()) {
            AdsLoadLog.bannerSkipped(slotLabel, rcParam, unitId, "MobileAds init timeout")
            slotVisible.value = false
            onFailed()
            return@LaunchedEffect
        }
        repeat(50) {
            if (AdsControl.shouldShowAds()) {
                readyToLoad = true
                AdsLoadLog.bannerTrace(slotLabel, rcParam, unitId, "ready to load")
                return@LaunchedEffect
            }
            delay(200)
        }
        if (!AdsControl.shouldShowAds()) {
            AdsLoadLog.bannerSkipped(slotLabel, rcParam, unitId, "shouldShowAds=false")
            slotVisible.value = false
            onFailed()
        }
    }

    if (!slotVisible.value) return

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MediumRectangleSlotHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(MediumRectangleSlotWidth)
                    .height(MediumRectangleSlotHeight)
                    .clip(MediumRectangleSlotShape)
                    .border(1.dp, MediumRectangleSlotBorderColor, MediumRectangleSlotShape),
            contentAlignment = Alignment.Center,
        ) {
            if (showShimmer.value || !readyToLoad) {
                MediumRectangleAdLoadingShimmer(
                    modifier = Modifier.fillMaxWidth().height(MediumRectangleSlotHeight),
                )
            }

            if (readyToLoad) {
                key(unitId) {
                    val loadGeneration = rememberAdLoadGeneration()
                    val reported = remember { mutableStateOf(false) }

                    AndroidView(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(MediumRectangleSlotHeight),
                    factory = { context ->
                        val generationAtCreate = loadGeneration.intValue
                        AdView(context).apply {
                            visibility = View.INVISIBLE
                            setAdSize(AdSize.MEDIUM_RECTANGLE)
                            adUnitId = unitId
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                )
                            adListener =
                                object : AdListener() {
                                    override fun onAdLoaded() {
                                        if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return
                                        if (reported.value) return
                                        reported.value = true
                                        showShimmer.value = false
                                        visibility = View.VISIBLE
                                        AdsLoadLog.bannerLoaded(slotLabel, rcParam, unitId)
                                        AdsLoadLog.bannerShown(slotLabel, rcParam, unitId)
                                        onLoaded()
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return
                                        if (reported.value) return
                                        reported.value = true
                                        showShimmer.value = false
                                        slotVisible.value = false
                                        AdsLoadLog.bannerFailed(slotLabel, rcParam, unitId, error)
                                        onFailed()
                                    }
                                }
                            AdsLoadLog.bannerRequesting(slotLabel, rcParam, unitId)
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    onRelease = { adView -> adView.destroy() },
                )
            }
        }
        }
    }
}
