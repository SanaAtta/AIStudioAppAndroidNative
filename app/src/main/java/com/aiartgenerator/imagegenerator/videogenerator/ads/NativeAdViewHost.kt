package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess

/**
 * XML-based native ad host (VideoDownloader style).
 * Ready to use on any screen later — not wired anywhere yet.
 */
@Composable
fun NativeAdViewHost(
    adUnitId: String,
    adSize: NativeAdSize,
    modifier: Modifier = Modifier,
    colorConfig: NativeAdColorConfig = NativeAdColorConfig.default(),
    source: String = "default",
    onAdLoaded: () -> Unit = {},
    onAdFailed: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()

    if (activity == null || adUnitId.isBlank() || isPremium) {
        return
    }

    val adManager = remember(activity) { AdmobNativeManager(activity.applicationContext) }
    val adViewContainer = remember { FrameLayout(context) }
    val shimmerContainer = remember { FrameLayout(context) }

    DisposableEffect(adUnitId, adSize, source) {
        val shown = adManager.showNativeAdIfAvailable(
            adUnitId = adUnitId,
            adContainer = adViewContainer,
            adSize = adSize,
            shimmerContainer = shimmerContainer,
            colorConfig = colorConfig,
            onShown = { onAdLoaded() },
        )

        if (!shown) {
            adManager.loadNativeAd(
                adContainer = adViewContainer,
                adUnitId = adUnitId,
                adSize = adSize,
                shimmerContainer = shimmerContainer,
                colorConfig = colorConfig,
                source = source,
                onLoaded = { onAdLoaded() },
                onFailed = { onAdFailed() },
            )
        }

        onDispose {
            adManager.destroyAd(adUnitId)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                addView(shimmerContainer)
                addView(adViewContainer)
            }
        },
    )
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
