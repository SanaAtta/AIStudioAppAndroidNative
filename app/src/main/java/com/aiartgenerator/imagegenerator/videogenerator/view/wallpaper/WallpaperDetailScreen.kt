package com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aiartgenerator.imagegenerator.videogenerator.controller.WallpaperDetailController
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperApplyTarget
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ContentMoreMenu
import com.aiartgenerator.imagegenerator.videogenerator.view.common.NetworkImage
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ReportContentModal
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionStyle
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch

private val OverlayScrim = Color.Black.copy(alpha = 0.45f)

@Composable
fun WallpaperDetailScreen(
    wallpaperId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember(wallpaperId) { WallpaperDetailController(wallpaperId) }
    val wallpaper = controller.wallpaper
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReportModal by remember { mutableStateOf(false) }

    LaunchedEffect(controller.statusMessage) {
        controller.statusMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            controller.clearStatus()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        controller.previewUrl?.let { url ->
            NetworkImage(
                url = url,
                contentDescription = wallpaper?.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleIconButton(
                onClick = onBack,
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = white,
                    )
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleIconButton(
                    onClick = {
                        if (!controller.isSharing) {
                            scope.launch { controller.share(context) }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = white,
                        )
                    },
                )
                ContentMoreMenu(
                    isOverlay = true,
                    onReportClick = { showReportModal = true },
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WallpaperActionButton(
                text = "Download",
                icon = Icons.Filled.Download,
                loading = controller.isDownloading,
                style = ResultActionStyle.Secondary,
                onClick = {
                    scope.launch { controller.download(context) }
                },
                modifier = Modifier.weight(1f),
            )
            WallpaperActionButton(
                text = "Apply",
                icon = Icons.Filled.TouchApp,
                style = ResultActionStyle.Primary,
                onClick = controller::openApplyDialog,
                modifier = Modifier.weight(1f),
            )
        }

        if (controller.showApplyDialog) {
            SetWallpaperDialog(
                selectedTarget = controller.selectedTarget,
                isApplying = controller.isApplying,
                onSelect = controller::selectTarget,
                onDismiss = controller::dismissApplyDialog,
                onApply = {
                    scope.launch { controller.applyWallpaper(context) }
                },
            )
        }

        ReportContentModal(
            show = showReportModal,
            contentId = wallpaperId,
            contentType = ReportContentType.Image,
            prompt = wallpaper?.title,
            mediaUri = controller.previewUrl,
            onDismiss = { showReportModal = false },
        )
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(OverlayScrim)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun WallpaperActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ResultActionStyle = ResultActionStyle.Primary,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    val contentColor = when (style) {
        ResultActionStyle.Primary -> white
        ResultActionStyle.Secondary -> HomeOnBackground
    }
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .then(
                when (style) {
                    ResultActionStyle.Primary -> {
                        Modifier
                            .brandButtonShadow(shape)
                            .background(ProGradientBrush)
                    }
                    ResultActionStyle.Secondary -> {
                        Modifier
                            .background(GeneratorCardBackground.copy(alpha = 0.92f))
                            .border(1.dp, GeneratorInputBorder, shape)
                    }
                },
            )
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SetWallpaperDialog(
    selectedTarget: WallpaperApplyTarget,
    isApplying: Boolean,
    onSelect: (WallpaperApplyTarget) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(GeneratorCardBackground)
                .border(1.dp, GeneratorInputBorder, RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "Set Wallpaper",
                color = HomeOnBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose where you want to apply this wallpaper",
                color = HomeMuted,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))

            WallpaperApplyTarget.entries.forEach { target ->
                val selected = target == selectedTarget
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChipUnselected)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(1.5.dp, ProGradientStart),
                                    RoundedCornerShape(14.dp),
                                )
                            } else {
                                Modifier.border(
                                    1.dp,
                                    GeneratorInputBorder,
                                    RoundedCornerShape(14.dp),
                                )
                            },
                        )
                        .clickable { onSelect(target) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = target.label,
                        color = HomeOnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (selected) ProGradientStart else HomeMuted,
                                ),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ProGradientStart),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            WallpaperActionButton(
                text = "Apply",
                icon = Icons.Filled.TouchApp,
                loading = isApplying,
                style = ResultActionStyle.Primary,
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
