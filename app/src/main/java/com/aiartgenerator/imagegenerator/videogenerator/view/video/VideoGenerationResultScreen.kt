package com.aiartgenerator.imagegenerator.videogenerator.view.video

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideo
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideoStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.PlaybackQueueHelper
import androidx.compose.foundation.clickable
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GeneratedVideoCard
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.GeneratorResultAdKind
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.GeneratorResultBottomAd
import com.aiartgenerator.imagegenerator.videogenerator.view.common.MediaPlaybackControls
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionStyle
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ContentMoreMenu
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ReportContentModal
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import androidx.compose.runtime.rememberCoroutineScope
import com.aiartgenerator.imagegenerator.videogenerator.model.CreationActions
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class VideoResultAction {
    Download,
    Edit,
    Share,
}

private fun videoDisplayAspectRatio(aspectLabel: String): Float = when (aspectLabel) {
    "9:16" -> 9f / 16f
    "1:1" -> 1f
    "4:3" -> 4f / 3f
    "16:9" -> 16f / 9f
    else -> 16f / 9f
}

/** Letterbox/pillarbox to fit — never crop or stretch the generated video. */
private fun scaleVideoViewToFit(view: VideoView, videoWidth: Int, videoHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0) return
    view.post {
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return@post
        val scale = minOf(
            viewWidth / videoWidth.toFloat(),
            viewHeight / videoHeight.toFloat(),
        )
        view.pivotX = viewWidth / 2f
        view.pivotY = viewHeight / 2f
        view.scaleX = scale * videoWidth / viewWidth
        view.scaleY = scale * videoHeight / viewHeight
    }
}

@Composable
fun VideoGenerationResultScreen(
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val payload = GeneratedVideoStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (payload == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "No video to display", color = SplashMuted)
        }
        return
    }

    var activePayload by remember { mutableStateOf(payload) }
    var sessionId by remember { mutableIntStateOf(0) }
    val isPreview = activePayload.uri == null
    val previewDurationMs = activePayload.durationSeconds * 1000

    LaunchedEffect(Unit) {
        GeneratedVideosRepository.load(context)
    }

    val similarVideos = remember(activePayload.creationId, sessionId) {
        GeneratedVideosRepository.items
            .filter { it.id != activePayload.creationId }
            .take(3)
    }

    var playing by remember { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var duration by remember {
        mutableIntStateOf(if (isPreview) previewDurationMs else 0)
    }
    var shuffleOn by remember { mutableStateOf(false) }
    var repeatOn by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<VideoResultAction?>(null) }
    var showReportModal by remember { mutableStateOf(false) }

    fun loadTrack(video: GeneratedVideo) {
        GeneratedVideosRepository.loadIntoStore(context, video)
        val nextPayload = GeneratedVideoStore.current ?: return
        videoView?.stopPlayback()
        videoView = null
        mediaPlayer = null
        activePayload = nextPayload
        sessionId += 1
        playing = false
        progress = 0
        duration = if (nextPayload.uri == null) nextPayload.durationSeconds * 1000 else 0
    }

    fun seekTo(positionMs: Int) {
        val maxDuration = if (duration > 0) duration else previewDurationMs
        val target = positionMs.coerceIn(0, maxDuration)
        progress = target
        if (!isPreview) videoView?.seekTo(target)
    }

    fun togglePlayback() {
        if (isPreview) {
            playing = !playing
            return
        }
        videoView?.let { view ->
            if (playing) {
                view.pause()
                playing = false
            } else {
                view.start()
                playing = true
            }
        }
    }

    fun handlePrevious() {
        if (progress > PlaybackQueueHelper.RESTART_THRESHOLD_MS) {
            seekTo(0)
            if (!isPreview) {
                videoView?.start()
                playing = true
            }
            return
        }
        PlaybackQueueHelper.previousVideo(activePayload.creationId)?.let { loadTrack(it) }
    }

    fun handleNext() {
        val next = PlaybackQueueHelper.nextVideo(activePayload.creationId, shuffleOn)
        if (next != null) {
            loadTrack(next)
        } else {
            val maxDuration = if (duration > 0) duration else previewDurationMs
            seekTo(maxDuration)
            playing = false
        }
    }

    val currentPayload by rememberUpdatedState(activePayload)
    val shuffleState by rememberUpdatedState(shuffleOn)
    val repeatState by rememberUpdatedState(repeatOn)

    LaunchedEffect(repeatOn, mediaPlayer) {
        mediaPlayer?.isLooping = repeatOn
    }

    LaunchedEffect(playing, videoView, isPreview, previewDurationMs, sessionId, repeatOn) {
        if (isPreview) {
            if (!playing) return@LaunchedEffect
            while (playing && progress < previewDurationMs) {
                progress = (progress + 200).coerceAtMost(previewDurationMs)
                delay(200L)
            }
            if (progress >= previewDurationMs) {
                if (repeatOn) {
                    progress = 0
                } else {
                    playing = false
                    handleNext()
                }
            }
        } else {
            while (playing && videoView != null) {
                progress = videoView?.currentPosition ?: 0
                delay(200L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val aspectRatio = videoDisplayAspectRatio(activePayload.aspectLabel)
            val videoHeight = (maxWidth / aspectRatio).coerceAtMost(320.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(videoHeight)
                    .clip(RoundedCornerShape(0.dp)),
            ) {
                if (!isPreview && activePayload.uri != null) {
                    key(sessionId) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).also { view ->
                                    videoView = view
                                    view.setVideoURI(activePayload.uri)
                                    view.setOnPreparedListener { mp ->
                                        mediaPlayer = mp
                                        duration = mp.duration
                                        mp.isLooping = repeatState
                                        scaleVideoViewToFit(view, mp.videoWidth, mp.videoHeight)
                                    }
                                    view.setOnCompletionListener {
                                        if (repeatState) {
                                            view.seekTo(0)
                                            view.start()
                                            playing = true
                                        } else {
                                            playing = false
                                            PlaybackQueueHelper.nextVideo(
                                                currentPayload.creationId,
                                                shuffleState,
                                            )?.let { next ->
                                                loadTrack(next)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2A1045),
                                        Color(0xFF1A1035),
                                        Color(0xFF0D1228),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = white.copy(alpha = 0.35f),
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                            ),
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(2f)
                        .padding(12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverlayCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverlayCircleButton(
                            icon = Icons.Filled.Share,
                            contentDescription = "Share",
                            onClick = {
                                activePayload.uri?.let { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                                }
                            },
                        )
                        OverlayCircleButton(
                            icon = Icons.Filled.Home,
                            contentDescription = "Home",
                            onClick = onHome,
                        )
                        ContentMoreMenu(
                            isOverlay = true,
                            onReportClick = { showReportModal = true },
                        )
                    }
                }

                IconButton(
                    onClick = { togglePlayback() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = white,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (duration > 0 || isPreview) {
                        val totalDuration = if (duration > 0) duration else previewDurationMs
                        LinearProgressIndicator(
                            progress = {
                                if (totalDuration > 0) progress.toFloat() / totalDuration else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = ProGradientStart,
                            trackColor = Color.White.copy(alpha = 0.25f),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatTime(progress),
                                color = white,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = formatTime(totalDuration),
                                color = white,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = activePayload.prompt.take(48).ifBlank { "Generated Video" },
                color = SplashOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${activePayload.durationSeconds}s · ${activePayload.styleTitle} · ${activePayload.aspectLabel}",
                color = SplashMuted,
                fontSize = 14.sp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GeneratorCardBackground)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaPlaybackControls(
                    playing = playing,
                    shuffleEnabled = shuffleOn,
                    repeatEnabled = repeatOn,
                    onShuffleToggle = { shuffleOn = !shuffleOn },
                    onPrevious = { handlePrevious() },
                    onPlayPause = { togglePlayback() },
                    onNext = { handleNext() },
                    onRepeatToggle = { repeatOn = !repeatOn },
                    playButtonSize = 44.dp,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ResultActionButton(
                    label = "Download",
                    icon = Icons.Filled.Download,
                    style = ResultActionStyle.Secondary,
                    selected = selectedAction == VideoResultAction.Download,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedAction = VideoResultAction.Download
                        scope.launch {
                            val saved = CreationActions.downloadVideoFromPayload(context, activePayload)
                            Toast.makeText(
                                context,
                                if (saved) "Saved to gallery" else "Download failed",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
                ResultActionButton(
                    label = "Edit",
                    icon = Icons.Filled.Edit,
                    style = ResultActionStyle.Secondary,
                    selected = selectedAction == VideoResultAction.Edit,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedAction = VideoResultAction.Edit
                        GeneratedVideoStore.editDraft = GeneratedVideoStore.editDraftFrom(activePayload)
                        onEdit()
                    },
                )
                ResultActionButton(
                    label = "Share",
                    icon = Icons.Filled.Share,
                    style = ResultActionStyle.Secondary,
                    selected = selectedAction == VideoResultAction.Share,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedAction = VideoResultAction.Share
                        activePayload.uri?.let { uri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share video"))
                        }
                    },
                )
            }

            if (similarVideos.isNotEmpty()) {
                Text(
                    text = "Similar Videos",
                    color = SplashOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(similarVideos, key = { it.id }) { video ->
                        val uri = GeneratedVideosRepository.getUri(context, video)
                        if (uri != null) {
                            GeneratedVideoCard(
                                videoUri = uri,
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { loadTrack(video) },
                            )
                        }
                    }
                }
            }
        }
        }

        GeneratorResultBottomAd(kind = GeneratorResultAdKind.Video)
    }

    ReportContentModal(
        show = showReportModal,
        contentId = activePayload.creationId ?: activePayload.uri?.toString().orEmpty(),
        contentType = ReportContentType.Video,
        prompt = activePayload.prompt,
        mediaUri = activePayload.uri?.toString(),
        onDismiss = { showReportModal = false },
    )
}

@Composable
private fun OverlayCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = white,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
