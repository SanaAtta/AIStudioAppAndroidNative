package com.example.myapplication.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.example.myapplication.R
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsLoadLog
import com.example.myapplication.ads.MobileAdsInitializer
import com.example.myapplication.ads.NativeAdCtaStyles
import com.example.myapplication.ads.adLoadGenerationCurrent
import com.example.myapplication.ads.applyNativeAdCta
import com.example.myapplication.ads.logNativeMountRequest
import com.example.myapplication.ads.rememberAdLoadGeneration
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Splash top banner: loads a native advanced ad into [R.layout.native_small] (icon + headline + body + CTA,
 * no main media placement).
 *
 * @param unitId AdMob native unit id (typically from [com.example.myapplication.ads.AdUnitIds]).
 */
@Composable
fun NativeSmallEmbeddedAd(
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
        val holder = remember { NativeAdSingleton() }
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
                        .heightIn(min = NativeAdSlotHeights.WithoutMedia),
            ) {
                if (showLoadingShimmer.value) {
                    NativeSmallAdLoadingShimmer(modifier = Modifier.fillMaxWidth())
                }

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        val root =
                            LayoutInflater.from(ctx).inflate(R.layout.native_small, null, false) as NativeAdView
                        root.visibility = View.INVISIBLE
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
                                    bindAndAttach(holder, root, ad)
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

private fun bindAndAttach(holder: NativeAdSingleton, nativeAdView: NativeAdView, nativeAd: NativeAd) {
    nativeAdView.apply {
        headlineView = findViewById(R.id.ad_headline)
        bodyView = findViewById(R.id.ad_body)
        callToActionView = findViewById(R.id.ad_call_to_action)
        iconView = findViewById(R.id.ad_app_icon)
    }
    holder.attach(nativeAdView, nativeAd)
}


private fun applyNativeSmallAppearance(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    val ctx = nativeAdView.context

    fun color(resId: Int) = ContextCompat.getColor(ctx, resId)

    val headline =
        nativeAdView.headlineView as? TextView ?: nativeAdView.findViewById(R.id.ad_headline)
    headline?.apply {
        nativeAd.headline?.takeIf { it.isNotBlank() }?.let { text = it }
        setTextColor(color(R.color.native_ad_primary_text))
    }

    val body =
        nativeAdView.bodyView as? TextView ?: nativeAdView.findViewById(R.id.ad_body)
    body?.apply {
        nativeAd.body?.takeIf { it.isNotBlank() }?.let { text = it }
        setTextColor(color(R.color.native_ad_secondary_text))
    }

    val cta =
        nativeAdView.callToActionView as? TextView
            ?: nativeAdView.findViewById<View>(R.id.ad_call_to_action) as? TextView
    cta?.apply {
        nativeAd.callToAction?.takeIf { it.isNotBlank() }?.let { text = it }
        applyNativeAdCta(NativeAdCtaStyles.Variant.WITHOUT_MEDIA)
    }

    bindNativeSmallIcon(nativeAdView, nativeAd)
}


private class NativeAdSingleton {
    private var ad: NativeAd? = null

    fun attach(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        ad?.destroy()
        ad = nativeAd
        nativeAdView.setNativeAd(nativeAd)
        // Ads SDK + Compose/context can strip button fill or text; reinforce assets we show.
        applyNativeSmallAppearance(nativeAdView, nativeAd)
    }

    fun destroy() {
        ad?.destroy()
        ad = null
    }
}
