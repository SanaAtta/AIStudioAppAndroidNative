package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

object NativeAdViewBinder {
    fun bind(
        nativeAd: NativeAd,
        adView: NativeAdView,
        colorConfig: NativeAdColorConfig,
    ) {
        val style = StyleCache.from(colorConfig)
        applyContainerStyle(adView, style)
        bindHeadline(adView, nativeAd, style)
        bindBody(adView, nativeAd, style)
        bindCallToAction(adView, nativeAd, style)
        bindIcon(adView, nativeAd)
        bindMediaView(adView)
        bindAdvertiser(adView, nativeAd, style)
        bindAdChoices(adView)
        adView.setNativeAd(nativeAd)
    }

    private fun applyContainerStyle(adView: NativeAdView, style: StyleCache) {
        adView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = style.cornerRadiusPx
            setStroke(style.strokeWidthPx, style.strokeColor)
            setColor(style.backgroundColor)
        }
    }

    private fun bindHeadline(adView: NativeAdView, nativeAd: NativeAd, style: StyleCache) {
        adView.headlineView = adView.findViewById<TextView>(R.id.ad_headline)?.apply {
            text = nativeAd.headline
            setTextColor(style.headlineColor)
            visibility = if (nativeAd.headline.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun bindBody(adView: NativeAdView, nativeAd: NativeAd, style: StyleCache) {
        adView.bodyView = adView.findViewById<TextView>(R.id.ad_body)?.apply {
            text = nativeAd.body
            setTextColor(style.bodyTextColor)
            visibility = if (nativeAd.body.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun bindCallToAction(adView: NativeAdView, nativeAd: NativeAd, style: StyleCache) {
        val ctaView = adView.findViewById<TextView>(R.id.ad_call_to_action)
            ?: adView.findViewById<Button>(R.id.ad_call_to_action)

        adView.callToActionView = ctaView?.apply {
            text = nativeAd.callToAction
            setTextColor(style.ctaTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, style.ctaTextSizeSp)
            background = style.ctaBackground
            visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun bindIcon(adView: NativeAdView, nativeAd: NativeAd) {
        adView.iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)?.apply {
            nativeAd.icon?.let { icon ->
                setImageDrawable(icon.drawable)
                visibility = View.VISIBLE
            } ?: run {
                visibility = View.GONE
            }
        }
    }

    private fun bindMediaView(adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
    }

    private fun bindAdvertiser(adView: NativeAdView, nativeAd: NativeAd, style: StyleCache) {
        adView.advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)?.apply {
            text = nativeAd.advertiser
            setTextColor(style.bodyTextColor)
            visibility = if (nativeAd.advertiser.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun bindAdChoices(adView: NativeAdView) {
        adView.adChoicesView = adView.findViewById(R.id.ad_choices_view)
    }

    private data class StyleCache(
        val backgroundColor: Int,
        val strokeColor: Int,
        val cornerRadiusPx: Float,
        val strokeWidthPx: Int,
        val headlineColor: Int,
        val bodyTextColor: Int,
        val ctaTextColor: Int,
        val ctaTextSizeSp: Float,
        val ctaBackground: Drawable,
    ) {
        companion object {
            fun from(config: NativeAdColorConfig): StyleCache {
                return StyleCache(
                    backgroundColor = config.backgroundColorHex.parseColorOrDefault(Color.WHITE),
                    strokeColor = config.strokeColorHex.parseColorOrDefault(Color.LTGRAY),
                    cornerRadiusPx = (config.cornerRadiusDp ?: 0).dpToPx().toFloat(),
                    strokeWidthPx = (config.strokeWidthDp ?: 0).dpToPx(),
                    headlineColor = config.headlineColorHex.parseColorOrDefault(Color.BLACK),
                    bodyTextColor = config.bodyTextColorHex.parseColorOrDefault(Color.DKGRAY),
                    ctaTextColor = config.ctaTextColorHex.parseColorOrDefault(Color.WHITE),
                    ctaTextSizeSp = (config.ctaTextSizeSp ?: 14).toFloat(),
                    ctaBackground = createCtaBackground(
                        config.ctaBackgroundColorHex,
                        (config.ctaCornerRadiusDp ?: 8).dpToPx().toFloat(),
                    ),
                )
            }

            private fun createCtaBackground(colorString: String?, cornerRadius: Float): Drawable {
                val colors = colorString
                    ?.split(",")
                    ?.mapNotNull { runCatching { it.trim().toColorInt() }.getOrNull() }
                    .orEmpty()

                return when {
                    colors.size >= 2 -> GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        colors.toIntArray(),
                    ).apply { this.cornerRadius = cornerRadius }

                    colors.size == 1 -> GradientDrawable().apply {
                        setColor(colors[0])
                        this.cornerRadius = cornerRadius
                    }

                    else -> GradientDrawable().apply {
                        setColor(Color.BLACK)
                        this.cornerRadius = cornerRadius
                    }
                }
            }
        }
    }
}

private fun String?.parseColorOrDefault(default: Int): Int {
    return this?.let { runCatching { it.toColorInt() }.getOrDefault(default) } ?: default
}

private fun Int.dpToPx(): Int {
    return (this * Resources.getSystem().displayMetrics.density).toInt()
}
