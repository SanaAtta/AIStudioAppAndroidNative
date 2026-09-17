package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

@Composable
fun MediaPlaybackControls(
    playing: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonSize: Dp = 56.dp,
    playButtonShape: Shape = RoundedCornerShape(16.dp),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackControlIcon(
            icon = Icons.Filled.Shuffle,
            contentDescription = "Shuffle",
            selected = shuffleEnabled,
            onClick = trackedClick("playback_shuffle", onClick = onShuffleToggle),
        )
        PlaybackControlIcon(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Previous",
            onClick = trackedClick("playback_previous", onClick = onPrevious),
        )
        Box(
            modifier = Modifier
                .size(playButtonSize)
                .brandButtonShadow(playButtonShape)
                .clip(playButtonShape)
                .background(ProGradientBrush)
                .clickable(onClick = trackedClick("playback_play_pause", onClick = onPlayPause)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = white,
                modifier = Modifier.size(playButtonSize * 0.5f),
            )
        }
        PlaybackControlIcon(
            icon = Icons.Filled.SkipNext,
            contentDescription = "Next",
            onClick = trackedClick("playback_next", onClick = onNext),
        )
        PlaybackControlIcon(
            icon = Icons.Filled.Repeat,
            contentDescription = "Repeat",
            selected = repeatEnabled,
            onClick = trackedClick("playback_repeat", onClick = onRepeatToggle),
        )
    }
}

@Composable
fun PlaybackControlIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(ProGradientStart.copy(alpha = 0.18f))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) ProGradientStart else HomeMuted,
            modifier = Modifier.size(22.dp),
        )
    }
}
