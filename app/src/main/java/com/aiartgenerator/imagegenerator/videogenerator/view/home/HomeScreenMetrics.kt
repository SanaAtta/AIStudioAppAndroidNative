package com.aiartgenerator.imagegenerator.videogenerator.view.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberAppLayoutMetrics

internal data class HomeScreenMetrics(
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp?,
    val sectionSpacing: Dp,
    val cardRowSpacing: Dp,
    val imageCardTextWidthFraction: Float,
    val smallCardHeight: Dp,
    val titleScale: Float,
    val bodyScale: Float,
)

@Composable
internal fun rememberHomeScreenMetrics(): HomeScreenMetrics {
    val layout = rememberAppLayoutMetrics()
    return remember(layout) {
        HomeScreenMetrics(
            horizontalPadding = layout.horizontalPadding,
            contentMaxWidth = layout.contentMaxWidth,
            sectionSpacing = layout.sectionSpacing,
            cardRowSpacing = if (layout.isCompactWidth) 8.dp else 12.dp,
            imageCardTextWidthFraction = if (layout.isCompactWidth) 0.64f else 0.58f,
            smallCardHeight = (layout.screenWidthDp * 0.36f).coerceIn(112f, 142f).dp,
            titleScale = layout.titleScale,
            bodyScale = layout.bodyScale,
        )
    }
}

internal fun scaledSp(base: Float, scale: Float): Float = base * scale

/** Background art is 1024 x 470 — keeps card + Explore CTA aligned on all widths. */
internal const val HOME_IMAGE_GENERATOR_ASPECT_RATIO = 1024f / 470f
