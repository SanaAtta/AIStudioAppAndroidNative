package com.example.myapplication.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.theme.DisabledButton
import com.example.myapplication.theme.HomeBackground
import com.example.myapplication.theme.HomeMuted
import com.example.myapplication.theme.HomeNavSelected
import com.example.myapplication.theme.HomeOnBackground
import com.example.myapplication.theme.ScreenSurface
import com.example.myapplication.theme.ScreenSurfaceAlt

private enum class SongSource { Upload, Record }

@Composable
fun CoverScreen(modifier: Modifier = Modifier) {
    var source by remember { mutableStateOf(SongSource.Upload) }
    var hasSong by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Text(
            text = "AI Cover",
            color = HomeOnBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SourceChip(
                    label = "Upload",
                    icon = Icons.Filled.CloudUpload,
                    selected = source == SongSource.Upload,
                    onClick = { source = SongSource.Upload },
                    modifier = Modifier.weight(1f),
                )
                SourceChip(
                    label = "Record",
                    icon = Icons.Filled.Mic,
                    selected = source == SongSource.Record,
                    onClick = { source = SongSource.Record },
                    modifier = Modifier.weight(1f),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScreenSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (hasSong) {
                    Text(
                        text = if (source == SongSource.Upload) "Song selected" else "Recording stub",
                        color = HomeOnBackground,
                        fontSize = 16.sp,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (source == SongSource.Upload) "Upload a song" else "Record a song",
                            color = HomeOnBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = if (source == SongSource.Record) "Recording not implemented yet" else "Tap below to select",
                            color = HomeMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Button(
                onClick = { hasSong = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScreenSurfaceAlt),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = if (source == SongSource.Upload) Icons.Filled.CloudUpload else Icons.Filled.Mic,
                    contentDescription = null,
                    tint = HomeNavSelected,
                )
                Text(
                    text = if (source == SongSource.Upload) "Select song" else "Start recording (stub)",
                    color = HomeOnBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Button(
                onClick = {},
                enabled = hasSong,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeNavSelected,
                    disabledContainerColor = DisabledButton,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = "Create Cover",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HomeNavSelected else ScreenSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else HomeMuted,
            )
            Text(
                text = label,
                color = if (selected) Color.White else HomeOnBackground,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
