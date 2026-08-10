package com.example.myapplication.view.cover

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.controller.AiCoverController
import com.example.myapplication.controller.SongSource
import com.example.myapplication.model.VoiceOption
import com.example.myapplication.view.common.GeneratedAudioCard
import com.example.myapplication.view.common.GenerationLoadingCard
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeMuted
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import com.example.myapplication.view.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CoverSurface = Color(0xFF1C1C1E)
private val CoverSurfaceAlt = Color(0xFF2C2C2E)
private val CoverDisabledButton = Color(0xFF3A3A3C)
private val CoverHint = Color(0xFF8E8E93)

@Composable
fun AiCoverScreen(modifier: Modifier = Modifier) {
    val controller = remember { AiCoverController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val songPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            controller.onSongUploaded(context, uri)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            controller.startRecording(context)
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestStartRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            controller.startRecording(context)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(controller.statusMessage) {
        controller.statusMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            controller.clearStatus()
        }
    }

    LaunchedEffect(controller.isRecording) {
        while (controller.isRecording) {
            delay(1000)
            controller.tickRecordingSecond()
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

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
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp),
        ) {
            ChooseSongSection(
                selectedMode = controller.selectedMode,
                hasSong = controller.hasSong,
                isRecording = controller.isRecording,
                songSource = controller.songSource,
                songDisplayName = controller.songDisplayName,
                recordingSeconds = controller.recordingSeconds,
                onSelectUpload = { controller.selectMode(SongSource.Upload) },
                onSelectRecord = { controller.selectMode(SongSource.Record) },
                onUploadAreaClick = { songPicker.launch("audio/*") },
                onStartRecording = ::requestStartRecording,
                onStopRecording = { controller.stopRecording(context) },
                onClearSong = controller::clearSong,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Choose a Voice",
                color = HomeOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(controller.voices, key = { it.id }) { option ->
                    VoiceGridItem(
                        option = option,
                        selected = option.id == controller.selectedVoiceId,
                        onClick = { controller.onVoiceSelected(option.id) },
                    )
                }
            }
            CreateCoverButton(
                enabled = controller.canCreate,
                isCreating = controller.isCreating,
                onClick = { scope.launch { controller.onCreateCover(context) } },
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (controller.isCreating) {
                GenerationLoadingCard(
                    label = "Creating cover…",
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            controller.resultUri?.let { uri ->
                GeneratedAudioCard(
                    audioUri = uri,
                    title = "AI Cover",
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ChooseSongSection(
    selectedMode: SongSource,
    hasSong: Boolean,
    isRecording: Boolean,
    songSource: SongSource?,
    songDisplayName: String?,
    recordingSeconds: Int,
    onSelectUpload: () -> Unit,
    onSelectRecord: () -> Unit,
    onUploadAreaClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearSong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Choose a Song",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeButton(
                text = "Upload",
                icon = Icons.Filled.CloudUpload,
                selected = selectedMode == SongSource.Upload && !isRecording,
                onClick = onSelectUpload,
                enabled = !isRecording,
                modifier = Modifier.weight(1f),
            )
            ModeButton(
                text = "Record",
                icon = Icons.Filled.Mic,
                selected = selectedMode == SongSource.Record || isRecording,
                onClick = onSelectRecord,
                enabled = !isRecording,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            isRecording -> RecordingInProgressCard(
                seconds = recordingSeconds,
                onStop = onStopRecording,
            )
            hasSong -> SongCompletedCard(
                isRecordingSource = songSource == SongSource.Record,
                fileName = songDisplayName ?: "Audio001",
                onClear = onClearSong,
            )
            selectedMode == SongSource.Upload -> UploadIdleCard(onClick = onUploadAreaClick)
            else -> RecordIdleCard(onStart = onStartRecording)
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) HomeNavSelected else CoverSurface,
            contentColor = Color.White,
            disabledContainerColor = CoverDisabledButton,
            disabledContentColor = CoverHint,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RecordIdleCard(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CoverSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircleActionButton(
                onClick = onStart,
                icon = Icons.Filled.Mic,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Tap to start recording",
                color = HomeOnBackground,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun RecordingInProgressCard(
    seconds: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CoverSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatHms(seconds),
                color = HomeOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircleActionButton(
                onClick = onStop,
                icon = Icons.Filled.Stop,
            )
        }
    }
}

@Composable
private fun UploadIdleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CoverSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircleActionButton(
                onClick = onClick,
                icon = Icons.Filled.CloudUpload,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Tap to Upload Your Song",
                color = HomeOnBackground,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun SongCompletedCard(
    isRecordingSource: Boolean,
    fileName: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeNavSelected)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isRecordingSource) {
                    "Recording Completed Successfully!"
                } else {
                    "Song Uploaded Successfully!"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CoverSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CoverSurfaceAlt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = HomeNavSelected,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = fileName,
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = HomeOnBackground,
                )
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(HomeNavSelected)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp),
        )
    }
}

private fun formatHms(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
private fun VoiceGridItem(
    option: VoiceOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(option.tileColor)
                .then(
                    if (selected) {
                        Modifier.border(
                            BorderStroke(2.dp, HomeNavSelected),
                            RoundedCornerShape(14.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.title,
            color = HomeOnBackground,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CreateCoverButton(
    enabled: Boolean,
    isCreating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeNavSelected,
            contentColor = Color.White,
            disabledContainerColor = CoverDisabledButton,
            disabledContentColor = HomeMuted,
        ),
    ) {
        Text(
            text = if (isCreating) "Creating…" else "Create AI Cover",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiCoverScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiCoverScreen()
    }
}
