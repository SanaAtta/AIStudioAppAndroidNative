package com.aiartgenerator.imagegenerator.videogenerator.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Brand anchors for ad shimmers / CTA — matches app [ProGradientStart]/[ProGradientEnd].
 */
object BrandPalette {
    val ProStart = Color(0xFF7C4DFF)
    val ProEnd = Color(0xFF5C2DF5)

    /** @deprecated Prefer [ProStart]; kept so ad shimmer call-sites stay stable. */
    val CoralRed = ProStart
    /** @deprecated Prefer [ProEnd]. */
    val FlameOrange = ProEnd
}

/**
 * Reusable pair of gradient stops. Add more instances (e.g. `DarkOverlay`) as the app grows.
 */
data class GradientColorSet(
    val start: Color,
    val end: Color,
) {
    fun verticalBrush(): Brush = Brush.verticalGradient(colors = listOf(start, end))

    /** For backgrounds, borders, and buttons — not for [androidx.compose.material3.Text] (use [BrandGradientTitleText]). */
    fun horizontalBrush(): Brush = Brush.horizontalGradient(colors = listOf(start, end))

    fun linearBrush(
        startOffset: Offset = Offset.Zero,
        endOffset: Offset = Offset.Infinite,
    ): Brush = Brush.linearGradient(
        colors = listOf(start, end),
        start = startOffset,
        end = endOffset,
    )

    /**
     * Top-leading → bottom-trailing diagonal (common for hero / CTA backgrounds).
     */
    fun diagonalBrush(): Brush = linearBrush(
        startOffset = Offset.Zero,
        endOffset = Offset.Infinite,
    )
}

/**
 * Default app brand gradient: #7C4DFF → #5C2DF5.
 */
val BrandGradient = GradientColorSet(
    start = BrandPalette.ProStart,
    end = BrandPalette.ProEnd,
)
