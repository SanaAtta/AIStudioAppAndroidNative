package com.example.myapplication.ui.components

import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.example.myapplication.R
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsLoadLog
import com.example.myapplication.ads.NativeAdTemplate
import com.example.myapplication.ads.SplashNativeAdManager

/**
 * Splash native — binds the single session ad from [SplashNativeAdManager] (no per-remount AdLoader).
 */
@Composable
fun SplashNativeEmbeddedAdSlot(
    unitId: String,
    template: NativeAdTemplate,
    slotLabel: String,
    rcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 5.dp,
    onVisibleChanged: ((Boolean) -> Unit)? = null,
) {
    if (!AdsControl.shouldShowAds() || unitId.isBlank() || template == NativeAdTemplate.DISABLED) {
        LaunchedEffect(Unit) { onVisibleChanged?.invoke(false) }
        return
    }

    val context = LocalContext.current
    var nativeAd by remember(unitId, template) { mutableStateOf<NativeAd?>(null) }
    var showShimmer by remember(unitId, template) { mutableStateOf(true) }
    var failed by remember(unitId, template) { mutableStateOf(false) }

    LaunchedEffect(unitId, template) {
        failed = false
        showShimmer = true
        nativeAd = null
        onVisibleChanged?.invoke(false)
        SplashNativeAdManager.awaitLoaded(context, unitId, template, slotLabel, rcParam) { success ->
            if (success) {
                nativeAd = SplashNativeAdManager.borrowAd(unitId, template)
                showShimmer = nativeAd == null
                failed = nativeAd == null
            } else {
                showShimmer = false
                failed = true
                onVisibleChanged?.invoke(false)
            }
        }
    }

    if (failed) return

    val minHeight =
        when (template) {
            NativeAdTemplate.WITH_MEDIA -> NativeAdSlotHeights.WithMedia
            NativeAdTemplate.WITHOUT_MEDIA -> NativeAdSlotHeights.WithoutMedia
            NativeAdTemplate.DISABLED -> 0.dp
        }

    Box(
        modifier =
            modifier
                .padding(horizontal = horizontalContentPadding)
                .heightIn(min = if (showShimmer || nativeAd != null) minHeight else 0.dp),
    ) {
        if (showShimmer) {
            when (template) {
                NativeAdTemplate.WITH_MEDIA ->
                    NativeWithMediaAdLoadingShimmer(modifier = Modifier.fillMaxWidth())
                NativeAdTemplate.WITHOUT_MEDIA ->
                    NativeSmallAdLoadingShimmer(modifier = Modifier.fillMaxWidth())
                NativeAdTemplate.DISABLED -> Unit
            }
        }

        val ad = nativeAd
        if (ad != null) {
            LaunchedEffect(ad) {
                AdsLoadLog.shown(slotLabel, rcParam, unitId)
                onVisibleChanged?.invoke(true)
                showShimmer = false
            }
            val layoutRes =
                when (template) {
                    NativeAdTemplate.WITH_MEDIA -> R.layout.native_ad_with_media
                    NativeAdTemplate.WITHOUT_MEDIA -> R.layout.native_small
                    NativeAdTemplate.DISABLED -> return
                }
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    val root =
                        LayoutInflater.from(ctx).inflate(layoutRes, null, false) as NativeAdView
                    when (template) {
                        NativeAdTemplate.WITH_MEDIA -> bindSplashNativeWithMedia(root, ad)
                        NativeAdTemplate.WITHOUT_MEDIA -> bindSplashNativeSmall(root, ad)
                        NativeAdTemplate.DISABLED -> Unit
                    }
                    root.visibility = View.VISIBLE
                    root
                },
                update = { root ->
                    when (template) {
                        NativeAdTemplate.WITH_MEDIA -> bindSplashNativeWithMedia(root, ad)
                        NativeAdTemplate.WITHOUT_MEDIA -> bindSplashNativeSmall(root, ad)
                        NativeAdTemplate.DISABLED -> Unit
                    }
                },
            )
        }
    }
}

private fun bindSplashNativeWithMedia(
    nativeAdView: NativeAdView,
    nativeAd: NativeAd,
) {
    nativeAdView.mediaView = nativeAdView.findViewById(R.id.ad_media)
    nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
    nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
    nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
    nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
    nativeAdView.setNativeAd(nativeAd)
}

private fun bindSplashNativeSmall(
    nativeAdView: NativeAdView,
    nativeAd: NativeAd,
) {
    nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
    nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
    nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
    nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
    nativeAdView.setNativeAd(nativeAd)
}
