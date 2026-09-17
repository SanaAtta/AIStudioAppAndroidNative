package com.aiartgenerator.imagegenerator.videogenerator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.ui.theme.BrandPalette

/** Standard AdMob medium rectangle width (300×250 dp). */
val MediumRectangleSlotWidth = 300.dp

/**
 * Loading placeholder for a 300×250 medium rectangle banner (social platform paste screens).
 */
@Composable
fun MediumRectangleAdLoadingShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mrec_shimmer")
    val shiftPx by transition.animateFloat(
        initialValue = -280f,
        targetValue = 520f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "mrec_shimmer_shift",
    )
    val primary = BrandPalette.ProStart
    val brush =
        Brush.linearGradient(
            colors =
                listOf(
                    primary.copy(alpha = 0.22f),
                    primary.copy(alpha = 0.07f),
                    primary.copy(alpha = 0.22f),
                ),
            start = Offset(shiftPx, 0f),
            end = Offset(shiftPx + 220f, 280f),
        )
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(primary.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(brush),
        )
    }
}
