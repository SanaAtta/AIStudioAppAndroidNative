package com.example.myapplication.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.example.myapplication.ads.AdMobGoogleTestUnits
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsLoadLog
import com.example.myapplication.ads.MobileAdsInitializer
import com.example.myapplication.ads.adLoadGenerationCurrent
import com.example.myapplication.ads.rememberAdLoadGeneration
import kotlinx.coroutines.delay

/**
 * Anchored adaptive banner. Waits for Mobile Ads SDK + [AdsControl.shouldShowAds] before
 * [AdView.loadAd]. AdView always gets non-zero height while loading (required by AdMob).
 */
@Composable
fun AdaptiveBannerEmbeddedAd(
    unitId: String,
    modifier: Modifier = Modifier,
    slotLabel: String = "adaptive banner",
    rcParam: String = "",
    onLoaded: (bannerHeight: Dp) -> Unit = {},
    onFailed: () -> Unit = {},
) {
    if (unitId.isBlank()) {
        LaunchedEffect(Unit) { onFailed() }
        return
    }

    val minSlotHeight =
        if (AdMobGoogleTestUnits.isMediumRectangleUnit(unitId)) {
            MediumRectangleSlotHeight
        } else {
            AdaptiveBannerSlotMinHeight
        }

    var readyToLoad by remember(unitId) { mutableStateOf(false) }
    var adLoaded by remember(unitId) { mutableStateOf(false) }
    var loadedHeight by remember(unitId) { mutableStateOf(minSlotHeight) }

    LaunchedEffect(unitId) {
        AdsLoadLog.bannerTrace(slotLabel, rcParam, unitId, "wait start shouldShowAds=${AdsControl.shouldShowAds()}")
        if (!MobileAdsInitializer.awaitReady()) {
            AdsLoadLog.bannerSkipped(slotLabel, rcParam, unitId, "MobileAds init timeout")
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
            onFailed()
        }
    }

    if (!readyToLoad) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(minSlotHeight),
        ) {
            if (AdMobGoogleTestUnits.isMediumRectangleUnit(unitId)) {
                MediumRectangleAdLoadingShimmer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(minSlotHeight),
                )
            } else {
                AdaptiveBannerAdLoadingShimmer(modifier = Modifier.fillMaxWidth())
            }
        }
        return
    }

    val density = LocalDensity.current
    val slotHeight = if (adLoaded) loadedHeight else minSlotHeight

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(slotHeight),
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)

        key(unitId, widthDp) {
            val loadGeneration = rememberAdLoadGeneration()
            val reported = remember { mutableStateOf(false) }

            AndroidView(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(slotHeight),
                factory = { context ->
                    val generationAtCreate = loadGeneration.intValue
                    AdView(context).apply {
                        setAdSize(
                            AdMobGoogleTestUnits.resolvedBannerAdSize(
                                context,
                                unitId,
                                widthDp,
                            ),
                        )
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
                                    AdsLoadLog.bannerLoaded(slotLabel, rcParam, unitId)
                                    AdsLoadLog.bannerShown(slotLabel, rcParam, unitId)
                                    val hPx = adSize?.height ?: 0
                                    val hDp =
                                        if (hPx > 0) {
                                            with(density) { hPx.toDp() }
                                        } else {
                                            minSlotHeight
                                        }
                                    loadedHeight = hDp.coerceAtLeast(minSlotHeight)
                                    adLoaded = true
                                    onLoaded(loadedHeight)
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return
                                    if (reported.value) return
                                    reported.value = true
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
