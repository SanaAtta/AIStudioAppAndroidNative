package com.aiartgenerator.imagegenerator.videogenerator.view.cover

import android.content.Intent
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCover
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoverStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoversRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.PlaybackQueueHelper
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AudioProgressSlider
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AudioVolumeSlider
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AudioWaveform
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ContentMoreMenu
import com.aiartgenerator.imagegenerator.videogenerator.view.common.MediaPlaybackControls
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ReportContentModal
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionStyle
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.delay
import kotlin.random.Random

private val FavoritePink = Color(0xFFFF4081)

@Composable
fun CoverNowPlayingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = GeneratedCoverStore.current
    val context = LocalContext.current

    if (payload == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "No cover to play", color = SplashMuted)
        }
        return
    }

    var activePayload by remember { mutableStateOf(payload) }
    var sessionId by remember { mutableIntStateOf(0) }
    var showReportModal by remember { mutableStateOf(false) }
    val isPreview = activePayload.uri == null
    val previewDurationMs = activePayload.durationSeconds * 1000

    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var duration by remember {
        mutableIntStateOf(if (isPreview) previewDurationMs else 0)
    }
    var isFavorite by remember { mutableStateOf(activePayload.isFavorite) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var shuffleOn by remember { mutableStateOf(false) }
    var repeatOn by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    val waveformHeights = remember(sessionId) {
        List(48) { Random.nextFloat() * 0.6f + 0.25f }
    }

    fun loadTrack(cover: GeneratedCover) {
        GeneratedCoversRepository.loadIntoStore(context, cover)
        val nextPayload = GeneratedCoverStore.current ?: return
        activePayload = nextPayload
        isFavorite = nextPayload.isFavorite
        sessionId += 1
        playing = false
        progress = 0
        duration = if (nextPayload.uri == null) nextPayload.durationSeconds * 1000 else 0
    }

    fun seekTo(positionMs: Int) {
        val maxDuration = if (duration > 0) duration else previewDurationMs
        val target = positionMs.coerceIn(0, maxDuration)
        progress = target
        if (!isPreview) player?.seekTo(target)
    }

    fun togglePlayback() {
        if (isPreview) {
            playing = !playing
            return
        }
        player?.let { mp ->
            if (playing) {
                mp.pause()
                playing = false
            } else {
                mp.start()
                playing = true
            }
        }
    }

    fun handlePrevious() {
        if (progress > PlaybackQueueHelper.RESTART_THRESHOLD_MS) {
            seekTo(0)
            if (!isPreview) {
                player?.start()
                playing = true
            }
            return
        }
        PlaybackQueueHelper.previousCover(activePayload.creationId)?.let { loadTrack(it) }
    }

    fun handleNext() {
        val next = PlaybackQueueHelper.nextCover(activePayload.creationId, shuffleOn)
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

    key(sessionId) {
        if (!isPreview) {
            DisposableEffect(activePayload.uri) {
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, activePayload.uri!!)
                    setOnPreparedListener { mp ->
                        duration = mp.duration
                        mp.setVolume(volume, volume)
                        mp.isLooping = repeatState
                    }
                    setOnCompletionListener {
                        if (repeatState) {
                            seekTo(0)
                            start()
                        } else {
                            playing = false
                            PlaybackQueueHelper.nextCover(
                                currentPayload.creationId,
                                shuffleState,
                            )?.let { next ->
                                loadTrack(next)
                            }
                        }
                    }
                    prepare()
                }
                player = mediaPlayer
                onDispose {
                    mediaPlayer.release()
                    player = null
                    playing = false
                }
            }
        }
    }

    LaunchedEffect(repeatOn, player) {
        player?.isLooping = repeatOn
    }

    LaunchedEffect(volume, player) {
        player?.setVolume(volume, volume)
    }

    LaunchedEffect(playing, player, isPreview, previewDurationMs, sessionId) {
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
            while (playing && player != null) {
                progress = player?.currentPosition ?: 0
                delay(200L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SplashOnBackground,
                )
            }
            Text(
                text = "Now Playing",
                color = SplashOnBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ContentMoreMenu(
                isOverlay = false,
                tint = SplashOnBackground,
                onReportClick = { showReportModal = true },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
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
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = white.copy(alpha = 0.35f),
                    modifier = Modifier.size(72.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePayload.title,
                        color = SplashOnBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CoverCatalog.styleSubtitle(
                            activePayload.singerStyleLabel,
                            activePayload.voiceStyleLabel,
                        ),
                        color = SplashMuted,
                        fontSize = 14.sp,
                    )
                }
                IconButton(
                    onClick = {
                        activePayload.creationId?.let { id ->
                            GeneratedCoversRepository.findById(id)?.let { cover ->
                                isFavorite = GeneratedCoversRepository.toggleFavorite(context, cover)
                                val updated = activePayload.copy(isFavorite = isFavorite)
                                activePayload = updated
                                GeneratedCoverStore.current = updated
                            }
                        } ?: run { isFavorite = !isFavorite }
                    },
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) FavoritePink else SplashMuted,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AudioWaveform(
                    heights = waveformHeights,
                    progress = if (duration > 0) progress.toFloat() / duration else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                )
                AudioProgressSlider(
                    progressMs = progress,
                    durationMs = if (duration > 0) duration else previewDurationMs,
                    onSeek = { seekTo(it) },
                )
            }

            MediaPlaybackControls(
                playing = playing,
                shuffleEnabled = shuffleOn,
                repeatEnabled = repeatOn,
                onShuffleToggle = { shuffleOn = !shuffleOn },
                onPrevious = { handlePrevious() },
                onPlayPause = { togglePlayback() },
                onNext = { handleNext() },
                onRepeatToggle = { repeatOn = !repeatOn },
            )

            AudioVolumeSlider(
                volume = volume,
                onVolumeChange = { volume = it },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ResultActionButton(
                    label = "Save",
                    icon = Icons.Filled.Download,
                    style = ResultActionStyle.Primary,
                    selected = selectedAction == "save",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedAction = "save"
                        Toast.makeText(context, "Cover saved in app", Toast.LENGTH_SHORT).show()
                    },
                )
                ResultActionButton(
                    label = "Share",
                    icon = Icons.Filled.Share,
                    style = ResultActionStyle.Secondary,
                    selected = selectedAction == "share",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedAction = "share"
                        activePayload.uri?.let { uri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/mpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share cover"))
                        } ?: Toast.makeText(context, "Preview mode — no file to share", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    ReportContentModal(
        show = showReportModal,
        contentId = activePayload.creationId ?: activePayload.uri?.toString().orEmpty(),
        contentType = ReportContentType.Audio,
        prompt = activePayload.songName,
        mediaUri = activePayload.uri?.toString(),
        onDismiss = { showReportModal = false },
    )
}
