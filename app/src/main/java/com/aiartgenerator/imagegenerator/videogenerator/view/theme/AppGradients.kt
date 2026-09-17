package com.aiartgenerator.imagegenerator.videogenerator.view.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * App-wide gradient tokens. Reuse these — do not define one-off brushes in screens.
 *
 * - [Brand] Purple PRO/CTA gradient (#7C4DFF → #5C2DF5)
 * - [Action] Blue-purple action gradient (#2F80ED → #8A2BE2)
 * - [ProBadge] Orange-yellow Pro pill gradient (#FF9500 → #FFD60A)
 * - [Subscribe] Orange-gold subscribe CTA gradient (#FF9500 → #FFBA24)
 * - [VideoCard] Video generator card (orange)
 * - [MusicCard] Music generator card (teal)
 * - [Screen] App-wide screen background (#0D1540 → #090909)
 */
object AppGradients {
    val Brand get() = ProGradientBrush
    val Action get() = OnboardingButtonBrush
    val ProBadge get() = ProBadgeGradientBrush
    val Subscribe get() = SubscribeGradientBrush
    val VideoCard: Brush
        @Composable @ReadOnlyComposable get() = VideoCardGradientBrush
    val MusicCard: Brush
        @Composable @ReadOnlyComposable get() = MusicCardGradientBrush
    val Screen: Brush
        @Composable @ReadOnlyComposable
        get() = AllScreenBgGradientBrush
}

fun Modifier.brandGradientBackground(shape: Shape = RectangleShape): Modifier =
    clip(shape).background(AppGradients.Brand)

fun Modifier.actionGradientBackground(shape: Shape = RectangleShape): Modifier =
    clip(shape).background(AppGradients.Action)

fun Modifier.proBadgeGradientBackground(shape: Shape = RectangleShape): Modifier =
    clip(shape).background(AppGradients.ProBadge)

fun Modifier.subscribeGradientBackground(shape: Shape = RectangleShape): Modifier =
    clip(shape).background(AppGradients.Subscribe)
