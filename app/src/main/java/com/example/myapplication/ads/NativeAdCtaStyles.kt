package com.example.myapplication.ads

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.gson.annotations.SerializedName

/**
 * Remote Config: `ads_control.native_ad_cta` — CTA colors per native template.
 *
 * ```json
 * "native_ad_cta": {
 *   "without_media": { "gradient_start": "#E93B3B", "gradient_end": "#EF5A1C", "text_color": "#FFFFFF" },
 *   "with_media": { "background_color": "#EF5A1C", "text_color": "#FFFFFF" },
 *   "fullscreen": { "gradient_start": "#E93B3B", "gradient_end": "#EF5A1C", "text_color": "#FFFFFF" }
 * }
 * ```
 *
 * Supports `#RRGGBB` / `#AARRGGBB`. Invalid values fall back to brand defaults.
 */
object NativeAdCtaStyles {
    private const val DEFAULT_GRADIENT_START = 0xFFE93B3B.toInt()
    private const val DEFAULT_GRADIENT_END = 0xFFEF5A1C.toInt()
    private const val DEFAULT_TEXT = Color.WHITE

    @Volatile
    private var root: NativeAdCtaRoot? = null

    enum class Variant {
        WITHOUT_MEDIA,
        WITH_MEDIA,
        FULLSCREEN,
    }

    data class Style(
        val background: GradientDrawable,
        val textColor: Int,
    )

    internal fun applyFromAdsControl(ctaRoot: NativeAdCtaRoot?) {
        root = ctaRoot
    }

    fun style(context: Context, variant: Variant): Style {
        val spec =
            when (variant) {
                Variant.WITHOUT_MEDIA -> root?.withoutMedia
                Variant.WITH_MEDIA -> root?.withMedia
                Variant.FULLSCREEN -> root?.fullscreen
            }
        return buildStyle(context, spec)
    }

    private fun buildStyle(context: Context, spec: NativeAdCtaColorSpec?): Style {
        val gradientStart = parseColorOrNull(spec?.gradientStart) ?: DEFAULT_GRADIENT_START
        val gradientEnd = parseColorOrNull(spec?.gradientEnd) ?: DEFAULT_GRADIENT_END
        val solid = parseColorOrNull(spec?.backgroundColor)

        val background =
            GradientDrawable().apply {
                cornerRadius = 999f * context.resources.displayMetrics.density
                if (solid != null) {
                    setColor(solid)
                } else {
                    colors = intArrayOf(gradientStart, gradientEnd)
                    orientation = GradientDrawable.Orientation.LEFT_RIGHT
                }
            }

        val bgForContrast =
            solid
                ?: ColorUtils.compositeColors(
                    gradientEnd,
                    gradientStart,
                )

        val textColor =
            parseColorOrNull(spec?.textColor)
                ?: contrastTextColorForBackground(bgForContrast)

        return Style(background = background, textColor = textColor)
    }

    internal fun parseColorOrNull(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        var hex = raw.trim()
        if (!hex.startsWith("#")) hex = "#$hex"
        if (hex.length != 7 && hex.length != 9) return null
        return runCatching { Color.parseColor(hex) }.getOrNull()
    }

    private fun contrastTextColorForBackground(background: Int): Int {
        val luminance = ColorUtils.calculateLuminance(background)
        return if (luminance > 0.55) Color.BLACK else Color.WHITE
    }
}

internal data class NativeAdCtaRoot(
    @SerializedName("without_media") val withoutMedia: NativeAdCtaColorSpec? = null,
    @SerializedName("with_media") val withMedia: NativeAdCtaColorSpec? = null,
    @SerializedName("fullscreen") val fullscreen: NativeAdCtaColorSpec? = null,
)

/** Applies pill background + text color from RC to a native ad CTA [TextView]. */
fun TextView.applyNativeAdCta(variant: NativeAdCtaStyles.Variant) {
    val s = NativeAdCtaStyles.style(context, variant)
    background = s.background
    setTextColor(s.textColor)
}

internal data class NativeAdCtaColorSpec(
    @SerializedName("background_color") val backgroundColor: String? = null,
    @SerializedName("gradient_start") val gradientStart: String? = null,
    @SerializedName("gradient_end") val gradientEnd: String? = null,
    @SerializedName("text_color") val textColor: String? = null,
)
