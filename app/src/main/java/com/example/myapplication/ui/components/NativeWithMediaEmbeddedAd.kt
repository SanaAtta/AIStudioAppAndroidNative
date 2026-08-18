package com.example.myapplication.ui.components

import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.example.myapplication.R
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsLoadLog
import com.example.myapplication.ads.MobileAdsInitializer
import com.example.myapplication.ads.NativeAdCtaStyles
import com.example.myapplication.ads.adLoadGenerationCurrent
import com.example.myapplication.ads.applyNativeAdCta
import com.example.myapplication.ads.logNativeMountRequest
import com.example.myapplication.ads.rememberAdLoadGeneration

/**
 * Native template [R.layout.native_ad_with_media] — horizontal media + headline/body/CTA.
 */
@Composable
fun NativeWithMediaEmbeddedAd(
    unitId: String,
    slotLabel: String,
    rcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 5.dp,
    onLoadStateChanged: ((NativeAdLoadState) -> Unit)? = null,
) {
    if (!AdsControl.shouldShowAds() || unitId.isBlank()) {
        onLoadStateChanged?.invoke(NativeAdLoadState.Failed)
        return
    }

    key(unitId) {
        val holder = remember { NativeWithMediaAdHolder() }
        val showLoadingShimmer = remember { mutableStateOf(true) }
        val adSlotVisible = remember { mutableStateOf(true) }
        val loadGeneration = rememberAdLoadGeneration()

        DisposableEffect(Unit) {
            onDispose { holder.destroy() }
        }

        DisposableEffect(Unit) {
            onLoadStateChanged?.invoke(NativeAdLoadState.Loading)
            onDispose { }
        }

        if (adSlotVisible.value) {
            Box(
                modifier =
                    modifier
                        .padding(horizontal = horizontalContentPadding)
                        .heightIn(min = NativeAdSlotHeights.WithMedia),
            ) {
                if (showLoadingShimmer.value) {
                    NativeWithMediaAdLoadingShimmer(modifier = Modifier.fillMaxWidth())
                }

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        val root =
                            LayoutInflater.from(ctx)
                                .inflate(R.layout.native_ad_with_media, null, false) as NativeAdView
                        root.visibility = View.INVISIBLE
                        bindNativeWithMediaSlots(root)
                        val generationAtCreate = loadGeneration.intValue

                        MobileAdsInitializer.runWhenReady {
                            if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return@runWhenReady
                            logNativeMountRequest(slotLabel, rcParam, unitId)
                            AdLoader.Builder(ctx, unitId)
                                .forNativeAd { ad ->
                                    if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) {
                                        ad.destroy()
                                        return@forNativeAd
                                    }
                                    AdsLoadLog.loaded(slotLabel, rcParam, unitId)
                                    holder.attach(root, ad)
                                    showLoadingShimmer.value = false
                                    root.visibility = View.VISIBLE
                                    AdsLoadLog.shown(slotLabel, rcParam, unitId)
                                    onLoadStateChanged?.invoke(NativeAdLoadState.Loaded)
                                }
                                .withAdListener(
                                    object : AdListener() {
                                        override fun onAdFailedToLoad(error: LoadAdError) {
                                            if (!adLoadGenerationCurrent(generationAtCreate, loadGeneration)) return
                                            AdsLoadLog.failed(slotLabel, rcParam, unitId, error)
                                            root.visibility = View.GONE
                                            showLoadingShimmer.value = false
                                            adSlotVisible.value = false
                                            onLoadStateChanged?.invoke(NativeAdLoadState.Failed)
                                        }
                                    },
                                )
                                .withNativeAdOptions(NativeAdOptions.Builder().build())
                                .build()
                                .loadAd(AdRequest.Builder().build())
                        }

                        root
                    },
                )
            }
        }
    }
}

private fun bindNativeWithMediaSlots(nativeAdView: NativeAdView) {
    nativeAdView.mediaView = nativeAdView.findViewById(R.id.ad_media)
    nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
    nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
    nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
    nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
    clipNativeWithMediaViews(nativeAdView)
}

/** Keeps video/image inside the rounded media frame (prevents bleed past card border). */
private fun clipNativeWithMediaViews(nativeAdView: NativeAdView) {
    val density = nativeAdView.resources.displayMetrics.density
    val mediaRadiusPx = 10f * density

    nativeAdView.findViewById<FrameLayout>(R.id.ad_media_container)?.apply {
        clipToOutline = true
        clipChildren = true
        outlineProvider = ViewOutlineProvider.BACKGROUND
    }

    (nativeAdView.mediaView as? MediaView)?.let { mediaView ->
        fun applyClip() {
            if (mediaView.width <= 0 || mediaView.height <= 0) return
            mediaView.clipToOutline = true
            mediaView.outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(
                        view: View,
                        outline: Outline,
                    ) {
                        outline.setRoundRect(0, 0, view.width, view.height, mediaRadiusPx)
                    }
                }
        }
        applyClip()
        mediaView.post { applyClip() }
    }
}

private fun applyNativeWithMediaAppearance(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    val ctx = nativeAdView.context
    fun color(resId: Int) = ContextCompat.getColor(ctx, resId)

    nativeAd.mediaContent?.let { media ->
        (nativeAdView.mediaView as? MediaView)?.mediaContent = media
    }

    (nativeAdView.headlineView as? TextView)?.apply {
        nativeAd.headline?.takeIf { it.isNotBlank() }?.let { text = it }
        setTextColor(color(R.color.native_ad_primary_text))
    }

    (nativeAdView.bodyView as? TextView)?.apply {
        nativeAd.body?.takeIf { it.isNotBlank() }?.let { text = it }
        setTextColor(color(R.color.native_ad_secondary_text))
    }

    (nativeAdView.callToActionView as? TextView)?.apply {
        nativeAd.callToAction?.takeIf { it.isNotBlank() }?.let { text = it }
        applyNativeAdCta(NativeAdCtaStyles.Variant.WITH_MEDIA)
    }
}

private class NativeWithMediaAdHolder {
    private var ad: NativeAd? = null

    fun attach(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        ad?.destroy()
        ad = nativeAd
        nativeAdView.setNativeAd(nativeAd)
        applyNativeWithMediaAppearance(nativeAdView, nativeAd)
    }

    fun destroy() {
        ad?.destroy()
        ad = null
    }
}
