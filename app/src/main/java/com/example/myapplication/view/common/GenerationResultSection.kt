package com.example.myapplication.view.common

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.network.AiConfig
import com.example.myapplication.view.theme.HomeMuted
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import android.media.MediaPlayer as AndroidMediaPlayer

private val Surface = Color(0xFF1C1C1E)

@Composable
fun GenerationLoadingCard(
    label: String = "Generating…",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = HomeNavSelected)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = label, color = HomeMuted, fontSize = 14.sp)
        }
    }
}

@Composable
fun GeneratedImageCard(
    bytes: ByteArray?,
    fileUri: Uri?,
    modifier: Modifier = Modifier,
) {
    val model: Any? = bytes ?: fileUri
    if (model == null) return

    AsyncImage(
        model = model,
        contentDescription = "Generated image",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .addHeader("Authorization", "Bearer ${AiConfig.API_KEY}")
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

@Composable
fun GeneratedVideoCard(
    videoUri: Uri,
    modifier: Modifier = Modifier,
) {
    var playing by remember(videoUri) { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<VideoView?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface),
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).also { view ->
                    videoView = view
                    view.setVideoURI(videoUri)
                    view.setOnPreparedListener { mp ->
                        mp.isLooping = true
                    }
                    view.setOnCompletionListener {
                        playing = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (view.tag != videoUri) {
                    view.tag = videoUri
                    view.setVideoURI(videoUri)
                    playing = false
                }
            },
        )
        IconButton(
            onClick = {
                val view = videoView ?: return@IconButton
                if (playing) {
                    view.pause()
                    playing = false
                } else {
                    view.start()
                    playing = true
                }
            },
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)),
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
fun GeneratedAudioCard(
    audioUri: Uri,
    title: String = "Generated audio",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var playing by remember(audioUri) { mutableStateOf(false) }
    var player by remember(audioUri) { mutableStateOf<AndroidMediaPlayer?>(null) }

    DisposableEffect(audioUri) {
        val mediaPlayer = AndroidMediaPlayer().apply {
            setDataSource(context, audioUri)
            setOnCompletionListener { playing = false }
            prepare()
        }
        player = mediaPlayer
        onDispose {
            mediaPlayer.release()
            player = null
            playing = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = {
                val mediaPlayer = player ?: return@IconButton
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
                .background(HomeNavSelected, RoundedCornerShape(24.dp)),
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (playing) "Playing…" else "Tap play to listen",
                color = HomeMuted,
                fontSize = 13.sp,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}
