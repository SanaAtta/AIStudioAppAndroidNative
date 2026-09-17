package com.aiartgenerator.imagegenerator.videogenerator.view.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import kotlin.math.roundToInt

@Composable
fun AudioWaveform(
    heights: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = ProGradientStart,
    inactiveColor: Color = HomeMuted.copy(alpha = 0.45f),
) {
    Canvas(modifier = modifier) {
        val barCount = heights.size
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
        val progressIndex = (barCount * progress).coerceIn(0f, barCount.toFloat())

        heights.forEachIndexed { index, heightFactor ->
            val barHeight = size.height * heightFactor
            val x = index * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            val color = if (index < progressIndex) activeColor else inactiveColor
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
fun AudioProgressSlider(
    progressMs: Int,
    durationMs: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    timeColor: Color = SplashMuted,
) {
    val fraction = if (durationMs > 0) {
        (progressMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(durationMs) {
                    if (durationMs <= 0) return@pointerInput
                    detectTapGestures { offset ->
                        val seekFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((seekFraction * durationMs).roundToInt())
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs <= 0) return@pointerInput
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val seekFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((seekFraction * durationMs).roundToInt())
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackWidthPx = constraints.maxWidth.toFloat()
            val thumbOffsetPx = trackWidthPx * fraction
            val thumbOffsetDp = with(LocalDensity.current) { thumbOffsetPx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ChipUnselected),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.001f))
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProGradientStart),
            )
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp - 7.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(ProGradientStart)
                    .border(2.dp, ChipUnselected, CircleShape)
                    .align(Alignment.CenterStart),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatAudioTime(progressMs),
                color = timeColor,
                fontSize = 12.sp,
            )
            Text(
                text = formatAudioTime(durationMs),
                color = timeColor,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun AudioVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val level = volume.coerceIn(0f, 1f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = "Volume",
            tint = SplashMuted,
            modifier = Modifier.size(20.dp),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val value = (offset.x / size.width).coerceIn(0f, 1f)
                        onVolumeChange(value)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val value = (change.position.x / size.width).coerceIn(0f, 1f)
                        onVolumeChange(value)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ChipUnselected),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(level.coerceAtLeast(0.001f))
                    .height(3.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProGradientStart),
            )
        }
    }
}

fun formatAudioTime(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
