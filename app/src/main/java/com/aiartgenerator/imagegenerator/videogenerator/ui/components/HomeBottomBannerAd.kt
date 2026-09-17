package com.aiartgenerator.imagegenerator.videogenerator.ui.components

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdMobGoogleTestUnits
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsLoadLog
import com.aiartgenerator.imagegenerator.videogenerator.ads.MobileAdsInitializer
import com.aiartgenerator.imagegenerator.videogenerator.ads.rememberAdsAllowed
import com.aiartgenerator.imagegenerator.videogenerator.ads.adLoadGenerationCurrent
import com.aiartgenerator.imagegenerator.videogenerator.ads.rememberAdLoadGeneration
import kotlinx.coroutines.delay

/**
 * Home bottom fixed large banner — [com.google.android.gms.ads.AdSize.LARGE_BANNER] (320x100 dp).
 * Shows [LargeBannerAdLoadingShimmer] while loading; expands in place when the ad is ready.
 */
@Composable
fun HomeBottomBannerAd(
    unitId: String,
    modifier: Modifier = Modifier,
    slotLabel: String = "home large banner 320x100",
    rcParam: String = "",
    onLoaded: () -> Unit = {},
    onFailed: () -> Unit = {},
) {
    if (unitId.isBlank()) {
        LaunchedEffect(Unit) { onFailed() }
        return
    }

    var readyToLoad by remember(unitId) { mutableStateOf(false) }
    var adLoaded by remember(unitId) { mutableStateOf(false) }
    var adFailed by remember(unitId) { mutableStateOf(false) }

    LaunchedEffect(unitId) {
        AdsLoadLog.bannerTrace(slotLabel, rcParam, unitId, "wait start shouldShowAds=${AdsControl.shouldShowAds()}")
        if (!MobileAdsInitializer.awaitReady()) {
            AdsLoadLog.bannerSkipped(slotLabel, rcParam, unitId, "MobileAds init timeout")
            adFailed = true
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
            adFailed = true
            onFailed()
        }
    }

    if (adFailed) {
        return
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(LargeBannerSlotHeight),
    ) {
        if (!adLoaded) {
            LargeBannerAdLoadingShimmer(modifier = Modifier.fillMaxSize())
        }

        if (readyToLoad) {
            key(unitId) {
                val loadGeneration = rememberAdLoadGeneration()
                val reported = remember { mutableStateOf(false) }
                val loadUnitId = AdMobGoogleTestUnits.largeBannerLoadUnitId(unitId)

                AndroidView(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(LargeBannerSlotHeight)
                            .clip(LargeBannerSlotShape)
                            .then(
                                if (!adLoaded) {
                                    Modifier.offset(y = 10_000.dp)
                                } else {
                                    Modifier
                                },
                            ),
                    factory = { ctx ->
                        val generationAtCreate = loadGeneration.intValue
                        AdView(ctx).apply {
                            adUnitId = loadUnitId
                            setAdSize(AdMobGoogleTestUnits.resolvedLargeBannerAdSize())
                            layoutParams =
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM,
                                )
                            adListener =
                                object : AdListener() {
                                    override fun onAdLoaded() {
                                        if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return
                                        if (reported.value) return
                                        reported.value = true
                                        adLoaded = true
                                        AdsLoadLog.bannerLoaded(slotLabel, rcParam, loadUnitId)
                                        AdsLoadLog.bannerShown(slotLabel, rcParam, loadUnitId)
                                        onLoaded()
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return
                                        if (reported.value) return
                                        reported.value = true
                                        adFailed = true
                                        AdsLoadLog.bannerFailed(slotLabel, rcParam, loadUnitId, error)
                                        onFailed()
                                    }
                                }
                            AdsLoadLog.bannerRequesting(slotLabel, rcParam, loadUnitId)
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    onRelease = { adView -> adView.destroy() },
                )
            }
        }
    }
}
