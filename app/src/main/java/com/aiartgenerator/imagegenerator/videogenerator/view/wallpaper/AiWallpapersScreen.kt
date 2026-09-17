package com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.LanguageScreenAdSlot
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiWallpaperController
import com.aiartgenerator.imagegenerator.videogenerator.view.common.MainTabTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.controller.WallpaperDetailController
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperFilterChip
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperItem
import com.aiartgenerator.imagegenerator.videogenerator.view.common.NetworkImage
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChatInputHint
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.chipButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.generateButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PlusJakartaSansFamily
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch

private val BannerIconBg = Color(0xFF9A75F9)

@Composable
fun AiWallpapersScreen(
    onGenerateClick: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiWallpaperController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val bottomAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.wallpaperBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val showBottomAd = adsReady && AdsControl.wallpaperShowBottomAdSlot()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.wallpaperNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.wallpaperAdaptiveBanner() }

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                MainTabTopBar(
                    title = stringResource(R.string.wallpapers_title),
                    horizontalPadding = metrics.horizontalPadding,
                )

                /*
                WallpaperSearchBar(
                    query = controller.searchQuery,
                    onQueryChange = controller::onSearchChange,
                    modifier = Modifier.padding(horizontal = metrics.horizontalPadding),
                )

                Spacer(modifier = Modifier.height(14.dp))
                */

                CategoryChipRow(
                    chips = controller.filterChips,
                    selectedId = controller.selectedCategoryId,
                    onSelected = controller::onCategorySelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(bottom = 12.dp),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (metrics.screenWidthDp >= 600) 3 else 2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    /*
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AiWallpaperBanner(onClick = onGenerateClick)
                    }
                    */
                    if (controller.displayedWallpapers.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "No wallpapers found",
                                color = HomeMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(controller.displayedWallpapers, key = { it.id }) { wallpaper ->
                            WallpaperGridTile(
                                wallpaper = wallpaper,
                                onClick = { onWallpaperClick(wallpaper.id) },
                                onDownload = {
                                    scope.launch {
                                        val detail = WallpaperDetailController(wallpaper.id)
                                        detail.download(context)
                                        detail.statusMessage?.let { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onShare = {
                                    scope.launch {
                                        val detail = WallpaperDetailController(wallpaper.id)
                                        detail.share(context)
                                        detail.statusMessage?.let { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                if (showBottomAd) {
                    LanguageScreenAdSlot(
                        slotEnabled = true,
                        adaptiveBannerEnabled = bottomAd.adaptiveBanner,
                        bannerUnitId = bannerUnit,
                        nativeTemplate = bottomAd.nativeTemplate,
                        nativeUnitId = nativeUnit,
                        nativeSlotLabel = "wallpaper native bottom ad",
                        nativeRcParam = AdUnitIds.Rc.WALLPAPER_NATIVE,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.horizontalPadding)
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(GeneratorInputBackground)
            .border(1.dp, GeneratorInputBorder, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = ChatInputHint,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = PlusJakartaSansFamily,
                
                color = HomeOnBackground,
                fontSize = 15.sp,
            ),
            cursorBrush = SolidColor(ProGradientStart),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search wallpapers...",
                            color = ChatInputHint,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun CategoryChipRow(
    chips: List<WallpaperFilterChip>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            CategoryFilterChip(
                label = chip.label,
                selected = chip.id == selectedId,
                onClick = { onSelected(chip.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
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
            .clickable(onClick = onClick)
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
private fun AiWallpaperBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .generateButtonShadow(shape)
            .clip(shape)
            .background(ProGradientBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BannerIconBg.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_aria_spark),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "AI Wallpapers",
                color = white,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Generate your own unique wallpaper",
                color = white.copy(alpha = 0.78f),
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Generate",
            tint = white,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun WallpaperGridTile(
    wallpaper: WallpaperItem,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        NetworkImage(
            url = wallpaper.imageUrl(),
            contentDescription = wallpaper.title,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WallpaperOverlayButton(
                icon = Icons.Filled.Download,
                contentDescription = "Download",
                onClick = onDownload,
            )
            WallpaperOverlayButton(
                icon = Icons.Filled.Share,
                contentDescription = "Share",
                onClick = onShare,
            )
        }
    }
}

@Composable
private fun WallpaperOverlayButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = white,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228)
@Composable
private fun AiWallpapersScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiWallpapersScreen(
            onGenerateClick = {},
            onWallpaperClick = {},
        )
    }
}
