package com.aiartgenerator.imagegenerator.videogenerator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.ui.theme.BrandPalette

/**
 * Mirrors [R.layout.native_small] row (icon ~55dp, lines, pill CTA) while the splash native ad loads.
 */
@Composable
fun NativeSmallAdLoadingShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash_native_small_shimmer")
    val shiftPx by transition.animateFloat(
        initialValue = -240f,
        targetValue = 480f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "native_small_shimmer_shift",
    )
    // Primary-tinted sweep (matches [NativeMediumAdLoadingShimmer], no cool grey)
    val primary = BrandPalette.ProStart
    val edge = primary.copy(alpha = 0.22f)
    val mid = primary.copy(alpha = 0.07f)
    val shimmerBrush =
        Brush.linearGradient(
            colors = listOf(edge, mid, edge),
            start = Offset(shiftPx, 0f),
            end = Offset(shiftPx + 180f, 400f),
        )

    val bannerFill = primary.copy(alpha = 0.10f)
    val bannerBorder = primary.copy(alpha = 0.28f)
    val bannerShape = RoundedCornerShape(8.dp)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bannerFill, bannerShape)
                .border(width = 1.dp, color = bannerBorder, shape = bannerShape),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.92f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(shimmerBrush),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.75f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(shimmerBrush),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier =
                Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(shimmerBrush),
        )
    }
}
