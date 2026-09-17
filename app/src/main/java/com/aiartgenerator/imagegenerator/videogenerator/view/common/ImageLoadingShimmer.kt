package com.aiartgenerator.imagegenerator.videogenerator.view.common

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
import androidx.compose.ui.tooling.preview.Preview
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme

@Composable
fun ImageLoadingShimmer(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "imageShimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "imageShimmerTranslate",
    )

    Box(
        modifier = modifier.background(GeneratorInputBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            GeneratorInputBackground,
                            HomeMuted.copy(alpha = 0.28f),
                            GeneratorInputBackground,
                        ),
                        start = Offset(translate - 400f, translate - 400f),
                        end = Offset(translate, translate),
                    ),
                ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228)
@Composable
private fun ImageLoadingShimmerPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        ImageLoadingShimmer(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
