package com.example.myapplication.view.wallpaper

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.controller.WallpaperDetailController
import com.example.myapplication.model.WallpaperApplyTarget
import com.example.myapplication.view.common.NetworkImage
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeMuted
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import kotlinx.coroutines.launch

private val OverlayButton = Color(0x99000000)
private val DialogSurface = Color(0xFF1C1C1E)
private val OptionIdle = Color(0xFF2C2C2E)

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

    LaunchedEffect(controller.statusMessage) {
        controller.statusMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            controller.clearStatus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
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
                        tint = HomeOnBackground,
                    )
                },
            )
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
                        tint = HomeOnBackground,
                    )
                },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionButton(
                text = "Download",
                icon = Icons.Filled.Download,
                loading = controller.isDownloading,
                onClick = {
                    scope.launch { controller.download(context) }
                },
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                text = "Apply",
                icon = Icons.Filled.TouchApp,
                loading = false,
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
            .background(OverlayButton)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeNavSelected,
            contentColor = Color.White,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = text,
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
                .background(DialogSurface)
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
                        .background(OptionIdle)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(1.5.dp, HomeNavSelected),
                                    RoundedCornerShape(14.dp),
                                )
                            } else {
                                Modifier
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
                                    if (selected) HomeNavSelected else HomeMuted,
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
                                    .background(HomeNavSelected),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onApply,
                enabled = !isApplying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeNavSelected,
                    contentColor = Color.White,
                ),
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = "Apply",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
