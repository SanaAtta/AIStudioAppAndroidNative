package com.aiartgenerator.imagegenerator.videogenerator.view.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.aiartgenerator.imagegenerator.videogenerator.controller.FavouritesController
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoversRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusicsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import androidx.compose.ui.res.stringResource
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.FavouritesFilter
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibrarySource
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LibraryGridCard
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LibraryTabRow
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme

@Composable
fun FavouritesScreen(
    onBack: () -> Unit,
    onImageClick: () -> Unit = {},
    onVideoClick: () -> Unit = {},
    onMusicClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val controller = remember { FavouritesController() }
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
        LibraryRepository.filterFavourites(allItems, controller.selectedFilter)
    }

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(title = stringResource(R.string.favourites_title), onBack = onBack)

            val tabLabels = controller.filters.map { stringResource(it.labelRes) }
            val selectedTabIndex = controller.filters.indexOf(controller.selectedFilter).coerceAtLeast(0)
            LibraryTabRow(
                tabs = tabLabels,
                selectedIndex = selectedTabIndex,
                onTabSelected = { index ->
                    controller.filters.getOrNull(index)?.let { controller.onFilterSelected(it) }
                },
                modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 14.dp),
            )

            if (displayedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(metrics.horizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.favourites_empty),
                        color = HomeMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(metrics.gridColumnCount()),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = metrics.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
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
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228)
@Composable
private fun FavouritesScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        FavouritesScreen(onBack = {})
    }
}
