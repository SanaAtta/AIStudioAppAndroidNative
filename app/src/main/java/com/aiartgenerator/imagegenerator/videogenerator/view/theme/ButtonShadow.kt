package com.aiartgenerator.imagegenerator.videogenerator.view.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.brandButtonShadow(
    shape: Shape = RoundedCornerShape(14.dp),
    elevation: Dp = 18.dp,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ProGradientStart.copy(alpha = 0.42f),
    spotColor = ProGradientEnd.copy(alpha = 0.55f),
)

fun Modifier.generateButtonShadow(
    shape: Shape = RoundedCornerShape(13.dp),
    elevation: Dp = 24.dp,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ProGradientStart.copy(alpha = 0.52f),
    spotColor = ProGradientEnd.copy(alpha = 0.68f),
)

fun Modifier.chipButtonShadow(
    shape: Shape = RoundedCornerShape(50.dp),
    elevation: Dp = 8.dp,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ProGradientStart.copy(alpha = 0.22f),
    spotColor = ProGradientEnd.copy(alpha = 0.28f),
)

fun Modifier.subscribeButtonShadow(
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 20.dp,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = SubscribeGradientStart.copy(alpha = 0.45f),
    spotColor = SubscribeGradientEnd.copy(alpha = 0.55f),
)
