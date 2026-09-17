package com.aiartgenerator.imagegenerator.videogenerator.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.aiartgenerator.imagegenerator.videogenerator.model.CreationActions
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoversRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusicsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryItem
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibrarySource
import com.aiartgenerator.imagegenerator.videogenerator.view.common.HomeExploreButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LibraryItemPreview
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LibraryMediaTypeBadge
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ChatStyleFloatingFab
import com.aiartgenerator.imagegenerator.videogenerator.view.common.fabAboveBottomNav
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.HomeScreenAdSlot
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppBarProSettingsActions
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeSeeAll
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MusicCardBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.VideoCardBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppGradients
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import java.util.Calendar

@Composable
fun HomeScreen(
    onImageGeneratorClick: () -> Unit = {},
    onVideoGeneratorClick: () -> Unit = {},
    onMusicGeneratorClick: () -> Unit = {},
    onChatbotClick: () -> Unit = {},
    onSeeAllCreationsClick: () -> Unit = {},
    onItemClick: (LibraryItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val creations by GeneratedCreationsRepository.observeItems()
    val videos by GeneratedVideosRepository.observeItems()
    val musics by GeneratedMusicsRepository.observeItems()
    val covers by GeneratedCoversRepository.observeItems()
    val scope = rememberCoroutineScope()
    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val recentAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.homeRecentSectionAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val showRecentAd = adsReady && AdsControl.homeShowRecentSectionAdSlot()
    val homeNativeUnit = remember(adsConfigRev) { AdUnitIds.homeNative() }
    val homeBannerUnit = remember(adsConfigRev) { AdUnitIds.homeAdaptiveBanner() }
    val metrics = rememberHomeScreenMetrics()

    val recentItems = remember(creations, videos, musics, covers) {
        LibraryRepository.buildItems(context).take(4)
    }

    LaunchedEffect(Unit) {
        GeneratedCreationsRepository.load(context)
        GeneratedVideosRepository.load(context)
        GeneratedMusicsRepository.load(context)
        GeneratedCoversRepository.load(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(metrics = metrics)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, fill = true),
            ) {
                val contentModifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (metrics.contentMaxWidth != null) {
                            Modifier.widthIn(max = metrics.contentMaxWidth)
                        } else {
                            Modifier
                        },
                    )
                    .align(Alignment.TopCenter)
                Column(
                    modifier = contentModifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
                ) {
                    ImageGeneratorCard(
                        onClick = onImageGeneratorClick,
                        metrics = metrics,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.cardRowSpacing),
                    ) {
                        VideoGeneratorCard(
                            onClick = onVideoGeneratorClick,
                            metrics = metrics,
                            modifier = Modifier.weight(1f),
                        )
                        MusicGeneratorCard(
                            onClick = onMusicGeneratorClick,
                            metrics = metrics,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (showRecentAd) {
                        HomeScreenAdSlot(
                            slotEnabled = true,
                            adaptiveBannerEnabled = recentAd.adaptiveBanner,
                            bannerUnitId = homeBannerUnit,
                            nativeTemplate = recentAd.nativeTemplate,
                            nativeUnitId = homeNativeUnit,
                            nativeSlotLabel = "home recent section native ad",
                            nativeRcParam = AdUnitIds.Rc.HOME_NATIVE,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    RecentCreationsSection(
                        items = recentItems,
                        onSeeAllClick = onSeeAllCreationsClick,
                        onItemClick = onItemClick,
                        metrics = metrics,
                        onFavoriteClick = { item ->
                            CreationActions.toggleFavorite(context, item)
                        },
                        onDownloadClick = { item ->
                            scope.launch {
                                val saved = CreationActions.downloadToGallery(context, item)
                                android.widget.Toast.makeText(
                                    context,
                                    if (saved) "Saved to gallery" else "Download failed",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        onShareClick = { item ->
                            CreationActions.share(context, item)
                        },
                        onGenerateFirstClick = onImageGeneratorClick,
                    )
                }
            }
        }

        ChatStyleFloatingFab(
            onClick = onChatbotClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fabAboveBottomNav(),
        )
    }
}

@Composable
private fun HomeTopBar(
    metrics: HomeScreenMetrics,
    modifier: Modifier = Modifier,
) {
    val greeting = rememberHomeGreeting()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = metrics.horizontalPadding + 2.dp)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = greeting,
                color = HomeOnBackground,
                fontSize = scaledSp(14f, metrics.bodyScale).sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_title),
                color = HomeOnBackground,
                fontSize = scaledSp(26f, metrics.titleScale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppBarProSettingsActions()
        }
    }
}

@Composable
private fun rememberHomeGreeting(): String {
    val greetingResId = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }
    return stringResource(greetingResId)
}

private val ImageGeneratorSubtitleColor = Color(0xFFCBD5F5)

@Composable
private fun ImageGeneratorCard(
    onClick: () -> Unit,
    metrics: HomeScreenMetrics,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HOME_IMAGE_GENERATOR_ASPECT_RATIO)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = R.drawable.home_image_generator_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .fillMaxWidth(metrics.imageCardTextWidthFraction)
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ImageGeneratorGalleryIcon()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.home_image_generator_title),
                        color = white,
                        fontSize = scaledSp(22f, metrics.titleScale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = scaledSp(26f, metrics.titleScale).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.home_image_generator_subtitle),
                        color = ImageGeneratorSubtitleColor,
                        fontSize = scaledSp(13f, metrics.bodyScale).sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = scaledSp(17f, metrics.bodyScale).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            HomeExploreButton(
                text = stringResource(R.string.home_image_generator_explore),
                fontSize = scaledSp(13f, metrics.bodyScale).sp,
            )
        }
    }
}

@Composable
private fun ImageGeneratorGalleryIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(50.dp))
            .border(
                width = 1.dp,
                color = HomeSeeAll.copy(alpha = 0.35f),
                shape = RoundedCornerShape(50.dp),
            )
            .background(HomeSeeAll.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_home_image_gallery),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun VideoGeneratorCard(
    onClick: () -> Unit,
    metrics: HomeScreenMetrics,
    modifier: Modifier = Modifier,
) {
    HomeGeneratorCard(
        modifier = modifier.height(metrics.smallCardHeight),
        gradient = AppGradients.VideoCard,
        borderColor = VideoCardBorder.copy(alpha = 0.3f),
        onClick = onClick,
    ) {
        GeneratorCardIcon(
            icon = Icons.Filled.Videocam,
            tintBackground = HomeSeeAll,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.home_video_generator_title),
                color = white,
                fontSize = scaledSp(16f, metrics.titleScale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_video_generator_subtitle),
                color = white.copy(alpha = 0.88f),
                fontSize = scaledSp(12f, metrics.bodyScale).sp,
                lineHeight = scaledSp(16f, metrics.bodyScale).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.weight(1f))

    }
}

@Composable
private fun MusicGeneratorCard(
    onClick: () -> Unit,
    metrics: HomeScreenMetrics,
    modifier: Modifier = Modifier,
) {
    HomeGeneratorCard(
        modifier = modifier.height(metrics.smallCardHeight),
        gradient = AppGradients.MusicCard,
        borderColor = MusicCardBorder.copy(alpha = 0.3f),
        onClick = onClick,
    ) {
        GeneratorCardIcon(
            icon = Icons.Filled.MusicNote,
            tintBackground = ProGradientStart,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.home_music_generator_title),
                color = white,
                fontSize = scaledSp(16f, metrics.titleScale).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_music_generator_subtitle),
                color = white.copy(alpha = 0.88f),
                fontSize = scaledSp(12f, metrics.bodyScale).sp,
                lineHeight = scaledSp(16f, metrics.bodyScale).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.weight(1f))

    }
}

@Composable
private fun HomeGeneratorCard(
    gradient: Brush,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp),
            )
            .background(gradient)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            content = content,
        )
    }
}

@Composable
private fun GeneratorCardIcon(
    icon: ImageVector,
    tintBackground: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))

            .background(white.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = white,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RecentCreationsSection(
    items: List<LibraryItem>,
    onSeeAllClick: () -> Unit,
    onItemClick: (LibraryItem) -> Unit,
    onFavoriteClick: (LibraryItem) -> Unit,
    onDownloadClick: (LibraryItem) -> Unit,
    onShareClick: (LibraryItem) -> Unit,
    onGenerateFirstClick: () -> Unit,
    metrics: HomeScreenMetrics,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_recent_creations),
                color = HomeOnBackground,
                fontSize = scaledSp(18f, metrics.titleScale).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (items.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    color = HomeSeeAll,
                    fontSize = scaledSp(14f, metrics.bodyScale).sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onSeeAllClick),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            EmptyRecentState(onGenerateClick = onGenerateFirstClick)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(metrics.cardRowSpacing)) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.cardRowSpacing),
                    ) {
                        rowItems.forEach { item ->
                            RecentLibraryCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onFavoriteClick = { onFavoriteClick(item) },
                                onDownloadClick = { onDownloadClick(item) },
                                onShareClick = { onShareClick(item) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRecentState(
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onGenerateClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.library_empty),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.home_recent_empty_hint),
            color = HomeOnBackground.copy(alpha = 0.65f),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun RecentLibraryCard(
    item: LibraryItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        LibraryItemPreview(item = item)

        LibraryMediaTypeBadge(
            type = item.type,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecentOverlayButton(
                icon = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                tint = if (item.isFavorite) Color(0xFFFF2D55) else white,
                contentDescription = "Favorite",
                onClick = onFavoriteClick,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecentOverlayButton(
                    icon = Icons.Filled.Download,
                    contentDescription = "Download",
                    onClick = onDownloadClick,
                )
                RecentOverlayButton(
                    icon = Icons.Filled.Share,
                    contentDescription = "Share",
                    onClick = onShareClick,
                )
            }
        }
    }
}

@Composable
private fun RecentOverlayButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = white,
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
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen()
    }
}
