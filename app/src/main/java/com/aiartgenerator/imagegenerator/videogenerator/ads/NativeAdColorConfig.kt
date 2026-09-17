package com.aiartgenerator.imagegenerator.videogenerator.ads

import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppThemeColors

data class NativeAdColorConfig(
    val backgroundColorHex: String? = null,
    val cornerRadiusDp: Int? = 12,
    val strokeWidthDp: Int? = 1,
    val strokeColorHex: String? = null,
    val headlineColorHex: String? = null,
    val bodyTextColorHex: String? = null,
    val ctaBackgroundColorHex: String? = null,
    val ctaTextColorHex: String? = null,
    val ctaCornerRadiusDp: Int? = 10,
    val ctaTextSizeSp: Int? = null,
) {
    companion object {
        fun default(): NativeAdColorConfig = NativeAdColorConfig(
            backgroundColorHex = "#FFFFFFFF",
            strokeColorHex = "#FFE8E8E8",
            headlineColorHex = "#FF111111",
            bodyTextColorHex = "#FF666666",
            ctaBackgroundColorHex = "#7C4DFF,#5061E9",
            ctaTextColorHex = "#FFFFFFFF",
            ctaTextSizeSp = 14,
        )

        fun fromTheme(colors: AppThemeColors): NativeAdColorConfig {
            return NativeAdColorConfig(
                backgroundColorHex = colors.settingsCardBackground.toHex(),
                strokeColorHex = colors.generatorInputBorder.toHex(),
                headlineColorHex = colors.homeOnBackground.toHex(),
                bodyTextColorHex = colors.homeMuted.toHex(),
                ctaBackgroundColorHex = "#7C4DFF,#5061E9",
                ctaTextColorHex = "#FFFFFFFF",
            )
        }

        private fun androidx.compose.ui.graphics.Color.toHex(): String {
            val argb = ((alpha * 255).toInt() shl 24) or
                ((red * 255).toInt() shl 16) or
                ((green * 255).toInt() shl 8) or
                (blue * 255).toInt()
            return String.format("#%08X", argb)
        }
    }
}

object NativeAdLayouts {
    fun layoutRes(adSize: NativeAdSize): Int = when (adSize) {
        NativeAdSize.SMALL -> R.layout.native_small
        NativeAdSize.MEDIUM -> R.layout.native_medium
        NativeAdSize.ONLY_MEDIA -> R.layout.native_only_media
        NativeAdSize.LARGE -> R.layout.native_large
    }

    fun shimmerLayoutRes(adSize: NativeAdSize): Int = when (adSize) {
        NativeAdSize.SMALL -> R.layout.native_loading_small
        NativeAdSize.MEDIUM -> R.layout.native_loading_medium
        NativeAdSize.ONLY_MEDIA -> R.layout.native_loading_media
        NativeAdSize.LARGE -> R.layout.native_loading_large
    }
}
