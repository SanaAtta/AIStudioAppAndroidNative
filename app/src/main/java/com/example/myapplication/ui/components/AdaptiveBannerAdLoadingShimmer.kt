package com.example.myapplication.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.BrandPalette

/** Reserved height while an anchored adaptive banner loads. */
val AdaptiveBannerSlotMinHeight = 60.dp

/** Fixed large banner slot — [com.google.android.gms.ads.AdSize.LARGE_BANNER] (320x100 dp). */
val LargeBannerSlotHeight = 100.dp

/** Matches language picker card border (16dp radius, light stroke). */
val LargeBannerSlotShape = RoundedCornerShape(16.dp)

private val LargeBannerSlotBorderColor = Color(0xFF0F1A10).copy(alpha = 0.20f)

fun Modifier.largeBannerSlotFrame(): Modifier =
    clip(LargeBannerSlotShape)
        .border(width = 1.dp, color = LargeBannerSlotBorderColor, shape = LargeBannerSlotShape)

@Composable
fun AdaptiveBannerAdLoadingShimmer(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "adaptive_banner_shimmer")
    val shiftPx by transition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "adaptive_banner_shimmer_shift",
    )
    val primary = BrandPalette.FlameOrange
    val brush =
        Brush.linearGradient(
            colors =
                listOf(
                    primary.copy(alpha = 0.20f),
                    primary.copy(alpha = 0.06f),
                    primary.copy(alpha = 0.20f),
                ),
            start = Offset(shiftPx, 0f),
            end = Offset(shiftPx + 160f, 0f),
        )
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(AdaptiveBannerSlotMinHeight)
                .clip(shape)
                .background(primary.copy(alpha = 0.08f))
                .border(1.dp, primary.copy(alpha = 0.22f), shape)
                .background(brush),
    )
}

/**
 * Loading placeholder for the home large banner slot (320×100 dp, 16dp card radius).
 * Skeleton mirrors a typical large-banner row: visual block + headline lines + CTA pill.
 */
@Composable
fun LargeBannerAdLoadingShimmer(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "large_banner_shimmer")
    val shiftPx by transition.animateFloat(
        initialValue = -280f,
        targetValue = 520f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "large_banner_shimmer_shift",
    )
    val primary = BrandPalette.FlameOrange
    val shimmerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    primary.copy(alpha = 0.22f),
                    primary.copy(alpha = 0.07f),
                    primary.copy(alpha = 0.22f),
                ),
            start = Offset(shiftPx, 0f),
            end = Offset(shiftPx + 200f, 120f),
        )
    val blockShape = RoundedCornerShape(10.dp)
    val pillShape = RoundedCornerShape(6.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(LargeBannerSlotHeight)
                .clip(LargeBannerSlotShape)
                .background(primary.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(blockShape)
                        .background(shimmerBrush),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.88f)
                            .height(14.dp)
                            .clip(pillShape)
                            .background(shimmerBrush),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.62f)
                            .height(12.dp)
                            .clip(pillShape)
                            .background(shimmerBrush),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.36f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush),
                )
            }
        }
    }
}
