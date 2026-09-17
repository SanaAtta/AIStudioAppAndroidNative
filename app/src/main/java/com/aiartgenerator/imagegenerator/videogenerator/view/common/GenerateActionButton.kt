package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.DisabledButton
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.DisabledButtonText
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.generateButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.delay

@Composable
fun AnimatedLoadingDotsText(
    baseText: String,
    modifier: Modifier = Modifier,
    color: Color = white,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    var dotCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(baseText) {
        dotCount = 0
        while (true) {
            delay(400)
            dotCount = (dotCount + 1) % 4
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = baseText,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
        )
        Box(modifier = Modifier.width(18.dp)) {
            Text(
                text = ".".repeat(dotCount),
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
            )
        }
    }
}

@Composable
fun GenerateActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingLabel: String = "Generating",
    icon: ImageVector = Icons.Filled.AutoAwesome,
    shape: RoundedCornerShape = RoundedCornerShape(13.dp),
    analyticsName: String = "generate",
    locked: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
    showAdBadge: Boolean = false,
) {
    val handleClick: () -> Unit = {
        if (locked) {
            onLockedClick?.invoke()
        } else {
            onClick()
        }
        Unit
    }
    val canClick = if (locked) onLockedClick != null else enabled && !isLoading
    val trackedOnClick = trackedClick(itemId = analyticsName, onClick = handleClick)
    val showActiveStyle = enabled || isLoading || locked
    val leadingIcon = if (locked) Icons.Filled.Lock else icon
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(54.dp)
            .then(if (showActiveStyle) Modifier.generateButtonShadow(shape) else Modifier)
            .clip(shape)
            .then(
                if (showActiveStyle) {
                    Modifier.background(ProGradientBrush)
                } else {
                    Modifier.background(DisabledButton)
                },
            )
            .clickable(enabled = canClick, onClick = trackedOnClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!isLoading) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = white,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            if (isLoading) {
                AnimatedLoadingDotsText(
                    baseText = loadingLabel,
                    color = white,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = label,
                    color = if (showActiveStyle) white else DisabledButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (showAdBadge && !locked && !isLoading) {
            GenerateAdBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 7.dp),
            )
        }
    }
}

@Composable
private fun GenerateAdBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFB020))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "AD",
            color = Color(0xFF3A2200),
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
            lineHeight = 10.sp,
        )
    }
}
