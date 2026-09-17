package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryItem
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryMediaType
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.chipButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MusicPlaceholderStart = Color(0xFF0D2818)
private val MusicPlaceholderEnd = Color(0xFF051A10)
private val MusicGlow = Color(0xFF39FF14)

@Composable
fun LibraryTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            LibraryTabChip(
                label = tab,
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun LibraryTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
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
            .clickable(onClick = trackedClick(itemId = "library_tab", onClick = onClick))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) white else HomeOnBackground,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LibraryGridCard(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = trackedClick(itemId = "library_item", onClick = onClick)),
    ) {
        LibraryItemPreview(item = item)

        LibraryMediaTypeBadge(
            type = item.type,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
    }
}

@Composable
fun LibraryItemPreview(
    item: LibraryItem,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (item.type) {
            LibraryMediaType.Image -> {
                if (item.previewUri != null) {
                    AsyncImage(
                        model = item.previewUri,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    LibraryMediaPlaceholder(
                        type = item.type,
                        accentColor = item.accentColor,
                    )
                }
            }
            LibraryMediaType.Video -> {
                if (item.previewUri != null) {
                    LibraryVideoThumbnail(
                        uri = item.previewUri,
                        accentColor = item.accentColor,
                    )
                } else {
                    LibraryMediaPlaceholder(
                        type = item.type,
                        accentColor = item.accentColor,
                    )
                }
            }
            LibraryMediaType.Music -> {
                LibraryMusicPlaceholderCard()
            }
            LibraryMediaType.Cover -> {
                LibraryCoverPlaceholderCard()
            }
        }
    }
}

@Composable
fun LibraryVideoThumbnail(
    uri: Uri,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var frame by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        frame = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(0)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (frame != null) {
            Image(
                bitmap = frame!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            LibraryMediaPlaceholder(
                type = LibraryMediaType.Video,
                accentColor = accentColor,
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = white,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun LibraryMediaPlaceholder(
    type: LibraryMediaType,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.55f),
                        accentColor.copy(alpha = 0.25f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.badge,
            color = white.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun LibraryMusicPlaceholderCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MusicPlaceholderStart, MusicPlaceholderEnd),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MusicGlow.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        radius = 280f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MusicGlow.copy(alpha = 0.18f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MusicGlow,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
fun LibraryCoverPlaceholderCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A1F4D), Color(0xFF151028)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ProGradientStart.copy(alpha = 0.22f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = ProGradientStart,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
fun LibraryMediaTypeBadge(
    type: LibraryMediaType,
    modifier: Modifier = Modifier,
) {
    val background = when (type) {
        LibraryMediaType.Image -> ProGradientStart
        LibraryMediaType.Video -> Color(0xFFFF9500)
        LibraryMediaType.Music -> Color(0xFF26C6DA)
        LibraryMediaType.Cover -> Color(0xFF7C4DFF)
    }
    Text(
        text = type.badge,
        color = white,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
