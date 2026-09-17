package com.aiartgenerator.imagegenerator.videogenerator.view.generator

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aiartgenerator.imagegenerator.videogenerator.model.CreationActions
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedImageStore
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.GeneratorResultAdKind
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.GeneratorResultBottomAd
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionStyle
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ContentMoreMenu
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ReportContentModal
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch

@Composable
fun ImageGenerationResultScreen(
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onAiEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = GeneratedImageStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReportModal by remember { mutableStateOf(false) }
    var isFavorite by remember(payload?.creationId) {
        mutableStateOf(
            payload?.creationId?.let { id ->
                GeneratedCreationsRepository.findById(id)?.isFavorite
            } ?: false,
        )
    }

    if (payload == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "No image to display", color = SplashMuted)
        }
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .navigationBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val imageAspectRatio =
                    payload.width.toFloat().coerceAtLeast(1f) /
                        payload.height.toFloat().coerceAtLeast(1f)
                val imageHeight = (maxWidth / imageAspectRatio).coerceIn(260.dp, 380.dp)

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(imageHeight),
                ) {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(context)
                                .data(payload.uri ?: payload.bytes)
                                .memoryCacheKey(payload.creationId ?: payload.uri?.toString())
                                .diskCacheKey(payload.creationId ?: payload.uri?.toString())
                                .crossfade(true)
                                .build(),
                        contentDescription = "Generated image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.Transparent,
                                            ),
                                    ),
                                ),
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .zIndex(2f)
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OverlayIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OverlayIconButton(
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
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Box(
                                    modifier =
                                        Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(ProGradientStart)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "${payload.width}×${payload.height} • ${payload.styleTitle} Style",
                                        color = white,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Text(
                                    text = resolutionLabel(payload.width, payload.height),
                                    color = white,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OverlayIconButton(
                                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    onClick = {
                                        payload.creationId?.let { id ->
                                            GeneratedCreationsRepository.findById(id)?.let { creation ->
                                                isFavorite = CreationActions.toggleFavorite(context, creation)
                                            }
                                        }
                                    },
                                )
                                OverlayIconButton(
                                    icon = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    onClick = {
                                        payload.uri?.let { uri ->
                                            val shareIntent =
                                                Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/jpeg"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                            context.startActivity(
                                                Intent.createChooser(shareIntent, "Share image"),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ResultActionButton(
                        label = "Download",
                        icon = Icons.Filled.Download,
                        style = ResultActionStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val saved = CreationActions.downloadImageFromPayload(context, payload)
                                Toast
                                    .makeText(
                                        context,
                                        if (saved) "Saved to gallery" else "Download failed",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        },
                    )
                    ResultActionButton(
                        label = "AI Edit",
                        icon = Icons.Filled.Edit,
                        style = ResultActionStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onAiEdit,
                    )
                    ResultActionButton(
                        label = "Save",
                        icon = Icons.Filled.BookmarkBorder,
                        style = ResultActionStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Toast.makeText(context, "Already saved to recents", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }

        GeneratorResultBottomAd(kind = GeneratorResultAdKind.Image)
    }

    ReportContentModal(
        show = showReportModal,
        contentId = payload.creationId ?: payload.uri?.toString().orEmpty(),
        contentType = ReportContentType.Image,
        prompt = payload.prompt,
        mediaUri = payload.uri?.toString(),
        onDismiss = { showReportModal = false },
    )
}

private fun resolutionLabel(width: Int, height: Int): String =
    "${width.coerceAtLeast(1)}×${height.coerceAtLeast(1)}"

@Composable
private fun OverlayIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
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

@Preview(showBackground = true)
@Composable
private fun ImageGenerationResultScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        ImageGenerationResultScreen(onBack = {}, onHome = {}, onAiEdit = {})
    }
}
