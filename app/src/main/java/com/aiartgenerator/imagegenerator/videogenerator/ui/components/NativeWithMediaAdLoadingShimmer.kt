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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.ui.theme.BrandPalette

/** Placeholder matching [R.layout.native_ad_with_media] (~158dp). */
@Composable
fun NativeWithMediaAdLoadingShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "native_with_media_shimmer")
    val shiftPx by transition.animateFloat(
        initialValue = -240f,
        targetValue = 480f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "native_with_media_shimmer_shift",
    )
    val primary = BrandPalette.ProStart
    val edge = primary.copy(alpha = 0.22f)
    val mid = primary.copy(alpha = 0.07f)
    val shimmerBrush =
        Brush.linearGradient(
            colors = listOf(edge, mid, edge),
            start = Offset(shiftPx, 0f),
            end = Offset(shiftPx + 180f, 400f),
        )
    val cardShape = RoundedCornerShape(12.dp)
    val cardFill = primary.copy(alpha = 0.10f)
    val cardBorder = primary.copy(alpha = 0.28f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(NativeAdSlotHeights.WithMedia)
                .clip(cardShape)
                .background(cardFill)
                .border(1.dp, cardBorder, cardShape)
                .padding(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier
                        .width(164.dp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush),
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(0.85f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush),
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush),
                )
            }
        }
    }
}
