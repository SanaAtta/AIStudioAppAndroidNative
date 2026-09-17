package com.aiartgenerator.imagegenerator.videogenerator.view.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.aiartgenerator.imagegenerator.videogenerator.controller.LibraryController
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoversRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusicsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryFilter
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibrarySource
import com.aiartgenerator.imagegenerator.videogenerator.view.common.MainTabTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LibraryGridCard
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LibraryTabRow
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme

@Composable
fun ProjectsScreen(
    onImageClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onMusicClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val controller = remember { LibraryController() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        GeneratedCreationsRepository.load(context)
        GeneratedVideosRepository.load(context)
        GeneratedMusicsRepository.load(context)
        GeneratedCoversRepository.load(context)
    }

    val creations by GeneratedCreationsRepository.observeItems()
    val videos by GeneratedVideosRepository.observeItems()
    val musics by GeneratedMusicsRepository.observeItems()
    val covers by GeneratedCoversRepository.observeItems()

    val allItems = remember(creations, videos, musics, covers) {
        LibraryRepository.buildItems(context)
    }
    val displayedItems = remember(allItems, controller.selectedFilter) {
        LibraryRepository.filterItems(allItems, controller.selectedFilter)
    }

    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val bottomAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.libraryBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val showBottomAd = adsReady && AdsControl.libraryShowBottomAdSlot() && displayedItems.isNotEmpty()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.libraryNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.libraryAdaptiveBanner() }

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            MainTabTopBar(
                title = stringResource(R.string.library_title),
                horizontalPadding = metrics.horizontalPadding,
            )

            val tabLabels = controller.filters.map { stringResource(it.labelRes) }
            val selectedTabIndex = controller.filters.indexOf(controller.selectedFilter).coerceAtLeast(0)
            LibraryTabRow(
                tabs = tabLabels,
                selectedIndex = selectedTabIndex,
                onTabSelected = { index ->
                    controller.filters.getOrNull(index)?.let { controller.onFilterSelected(it) }
                },
                modifier = Modifier.padding(horizontal = metrics.horizontalPadding),
            )

            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(metrics.horizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (controller.selectedFilter == LibraryFilter.Saved) {
                            stringResource(R.string.library_empty_saved)
                        } else {
                            stringResource(R.string.library_empty)
                        },
                        color = HomeMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(metrics.gridColumnCount()),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            ) {
                items(displayedItems, key = { "${it.source.name}_${it.id}" }) { item ->
                    LibraryGridCard(
                        item = item,
                        onClick = {
                            if (LibraryRepository.openItem(context, item)) {
                                when (item.source) {
                                    LibrarySource.Creation -> onImageClick()
                                    LibrarySource.Video -> onVideoClick()
                                    LibrarySource.Music -> onMusicClick()
                                    LibrarySource.Cover -> onCoverClick()
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
                    nativeSlotLabel = "library native bottom ad",
                    nativeRcParam = AdUnitIds.Rc.LIBRARY_NATIVE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228)
@Composable
private fun ProjectsScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        ProjectsScreen()
    }
}
