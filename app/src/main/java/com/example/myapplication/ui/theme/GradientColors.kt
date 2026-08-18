package com.example.myapplication.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Brand anchors for gradients (hex from design).
 */
object BrandPalette {
    val CoralRed = Color(0xFFE93B3B)
    val FlameOrange = Color(0xFFEF5A1C)
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
 * Default app brand gradient: #E93B3B → #EF5A1C.
 */
val BrandGradient = GradientColorSet(
    start = BrandPalette.CoralRed,
    end = BrandPalette.FlameOrange,
)
