package com.aiartgenerator.imagegenerator.videogenerator.view.cover

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiCoverController
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverVoiceStyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCover
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoversRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GenerationSession
import com.aiartgenerator.imagegenerator.videogenerator.model.SingerStyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.VoicePreviewPlayer
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AudioProgressSlider
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GenerateActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberFreeGenerationGate
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GeneratorSelectionChip
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.common.MainTabTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.chipButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Hint = Color(0xFF8E8E93)

@Composable
fun AiCoverScreen(
    onGenerating: () -> Unit,
    onResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiCoverController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generateGate = rememberFreeGenerationGate(FreeUsageKind.Cover)
    val lifecycleOwner = LocalLifecycleOwner.current
    val coversState = GeneratedCoversRepository.observeItems()
    val covers by coversState

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) controller.onSongUploaded(context, uri)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) controller.startRecording(context)
        else Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }

    fun requestRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) controller.startRecording(context)
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        GeneratedCoversRepository.load(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                GeneratedCoversRepository.load(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
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
            controller.tickRecordingSecond(context)
        }
    }

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            CoverTopBar(horizontalPadding = metrics.horizontalPadding)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = metrics.horizontalPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
            UploadVoiceSection(
                hasSong = controller.hasSong,
                isRecording = controller.isRecording,
                songUri = controller.songUri,
                songLocalPath = controller.songLocalPath,
                songDisplayName = controller.songDisplayName,
                songDurationSeconds = controller.songDurationSeconds,
                recordingSeconds = controller.recordingSeconds,
                onUploadClick = { audioPicker.launch("audio/*") },
                onMicClick = ::requestRecording,
                onStopRecording = { controller.stopRecording(context) },
            )
            SingerStyleSection(
                styles = controller.singerStyles,
                selectedId = controller.selectedSingerStyleId,
                onSelected = controller::onSingerStyleSelected,
            )
            CoverVoiceStyleSection(
                styles = controller.coverVoiceStyles,
                selectedId = controller.selectedCoverVoiceStyleId,
                onSelected = controller::onCoverVoiceStyleSelected,
            )
            GenerateActionButton(
                label = stringResource(R.string.generate_cover),
                icon = Icons.Filled.Mic,
                enabled = controller.canGenerate || generateGate.locked,
                isLoading = false,
                loadingLabel = stringResource(R.string.generating_cover),
                locked = generateGate.locked,
                onLockedClick = generateGate.onLockedClick,
                onClick = {
                    val ctrl = controller
                    val ctx = context
                    GenerationSession.start(GenerationSession.Kind.Cover) {
                        var ok = false
                        ctrl.onGenerate(ctx, onSuccess = { ok = true })
                        if (!ok) {
                            error(ctrl.statusMessage ?: "Cover generation failed")
                        }
                    }
                    onGenerating()
                },
                shape = RoundedCornerShape(13.dp),
            )
            MyCoversSection(
                covers = covers,
                onCoverClick = { cover ->
                    GeneratedCoversRepository.loadIntoStore(context, cover)
                    onResult()
                },
            )
        }
        }
    }
}

@Composable
private fun CoverTopBar(
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    MainTabTopBar(
        title = stringResource(R.string.ai_cover_title),
        modifier = modifier,
        horizontalPadding = horizontalPadding,
    )
}

@Composable
private fun UploadVoiceSection(
    hasSong: Boolean,
    isRecording: Boolean,
    songUri: Uri?,
    songLocalPath: String?,
    songDisplayName: String?,
    songDurationSeconds: Int,
    recordingSeconds: Int,
    onUploadClick: () -> Unit,
    onMicClick: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GeneratorCardBackground)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                drawRoundRect(
                    color = ProGradientStart.copy(alpha = 0.45f),
                    size = size,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = dashEffect),
                )
            }
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(white),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = ProGradientStart,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = when {
                isRecording -> stringResource(
                    R.string.cover_recording_progress,
                    CoverCatalog.formatDuration(recordingSeconds),
                )
                hasSong -> songDisplayName ?: stringResource(R.string.cover_voice_ready)
                else -> stringResource(R.string.upload_your_voice)
            },
            color = HomeOnBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = when {
                isRecording -> stringResource(R.string.cover_recording_hint)
                hasSong -> stringResource(R.string.cover_voice_ready_hint)
                else -> stringResource(R.string.upload_voice_hint)
            },
            color = Hint,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (isRecording) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onStopRecording)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.stop_recording),
                    color = white,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else if (hasSong && songUri != null) {
            VoiceSamplePreview(
                audioUri = songUri,
                localPath = songLocalPath,
                title = songDisplayName ?: stringResource(R.string.cover_voice_ready),
                durationSeconds = songDurationSeconds,
            )
            Text(
                text = stringResource(R.string.cover_replace_audio),
                color = HomeMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactVoiceActionButton(
                    label = stringResource(R.string.upload_audio),
                    icon = Icons.Filled.Add,
                    filled = true,
                    onClick = onUploadClick,
                    modifier = Modifier.weight(1f),
                )
                CompactVoiceActionButton(
                    label = stringResource(R.string.record_audio),
                    icon = Icons.Filled.Mic,
                    filled = false,
                    onClick = onMicClick,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CompactVoiceActionButton(
                    label = stringResource(R.string.upload_audio),
                    icon = Icons.Filled.Add,
                    filled = true,
                    onClick = onUploadClick,
                    modifier = Modifier.weight(1f),
                )
                CompactVoiceActionButton(
                    label = stringResource(R.string.record_audio),
                    icon = Icons.Filled.Mic,
                    filled = false,
                    onClick = onMicClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VoiceSamplePreview(
    audioUri: Uri,
    localPath: String?,
    title: String,
    durationSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var playing by remember(audioUri, localPath) { mutableStateOf(false) }
    var progressMs by remember(audioUri, localPath) { mutableIntStateOf(0) }
    var handle by remember(audioUri, localPath) { mutableStateOf<VoicePreviewPlayer.Handle?>(null) }

    DisposableEffect(audioUri, localPath) {
        val preview = runCatching {
            VoicePreviewPlayer.open(
                context = context,
                uri = audioUri,
                localPath = localPath,
                onComplete = {
                    playing = false
                    progressMs = 0
                },
            )
        }.getOrNull()
        handle = preview
        onDispose {
            preview?.release()
            handle = null
            playing = false
        }
    }

    LaunchedEffect(playing, audioUri, localPath) {
        while (playing) {
            progressMs = handle?.player?.currentPosition ?: 0
            if (handle?.player?.isPlaying != true) {
                playing = false
                break
            }
            delay(250)
        }
    }

    val durationMs = remember(audioUri, localPath, durationSeconds, handle) {
        handle?.player?.duration?.takeIf { it > 0 } ?: (durationSeconds * 1000).coerceAtLeast(1)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ChipUnselected)
            .border(1.dp, GeneratorInputBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconButton(
                onClick = {
                    val mediaPlayer = handle?.player ?: return@IconButton
                    if (playing) {
                        mediaPlayer.pause()
                        playing = false
                    } else {
                        mediaPlayer.start()
                        playing = true
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ProGradientBrush),
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) {
                        stringResource(R.string.cover_playing)
                    } else {
                        stringResource(R.string.cover_tap_to_listen)
                    },
                    tint = white,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = HomeOnBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (playing) {
                        stringResource(R.string.cover_playing)
                    } else {
                        stringResource(R.string.cover_tap_to_listen)
                    },
                    color = HomeMuted,
                    fontSize = 13.sp,
                )
            }
            Text(
                text = CoverCatalog.formatDuration(durationSeconds),
                color = HomeMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        AudioProgressSlider(
            progressMs = progressMs,
            durationMs = durationMs,
            onSeek = { seekTo ->
                handle?.player?.seekTo(seekTo)
                progressMs = seekTo
            },
        )
    }
}

@Composable
private fun CompactVoiceActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .then(if (filled) Modifier.brandButtonShadow(RoundedCornerShape(24.dp)) else Modifier)
            .then(
                if (filled) {
                    Modifier.background(ProGradientBrush)
                } else {
                    Modifier
                        .background(ChipUnselected)
                        .border(1.5.dp, ProGradientStart, RoundedCornerShape(24.dp))
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) white else HomeOnBackground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            color = if (filled) white else HomeOnBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SingerStyleSection(
    styles: List<SingerStyleOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.choose_singer_style),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            styles.forEach { style ->
                SingerStyleCard(
                    style = style,
                    selected = style.id == selectedId,
                    onClick = { onSelected(style.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SingerStyleCard(
    style: SingerStyleOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .then(if (selected) Modifier.chipButtonShadow(shape) else Modifier)
            .then(
                if (selected) {
                    Modifier.background(ProGradientBrush)
                } else {
                    Modifier
                        .background(ChipUnselected)
                        .border(1.dp, GeneratorInputBorder, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(id = style.iconRes),
            contentDescription = style.label,
            modifier = Modifier.size(36.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = style.label,
            color = if (selected) white else HomeMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverVoiceStyleSection(
    styles: List<CoverVoiceStyleOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_style),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            styles.forEach { style ->
                GeneratorSelectionChip(
                    label = style.label,
                    selected = style.id == selectedId,
                    onClick = { onSelected(style.id) },
                )
            }
        }
    }
}

@Composable
private fun MyCoversSection(
    covers: List<GeneratedCover>,
    onCoverClick: (GeneratedCover) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (covers.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.my_covers),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        covers.take(5).forEach { cover ->
            CoverRow(
                cover = cover,
                onClick = { onCoverClick(cover) },
            )
        }
    }
}

@Composable
private fun CoverRow(
    cover: GeneratedCover,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GeneratorCardBackground)
            .border(1.dp, GeneratorInputBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProGradientBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = white,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cover.title,
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${cover.singerStyleLabel} · ${cover.voiceStyleLabel} · ${CoverCatalog.formatDuration(cover.durationSeconds)}",
                color = HomeMuted,
                fontSize = 13.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.5.dp, ProGradientStart), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = ProGradientStart,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiCoverScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiCoverScreen(onGenerating = {}, onResult = {})
    }
}
