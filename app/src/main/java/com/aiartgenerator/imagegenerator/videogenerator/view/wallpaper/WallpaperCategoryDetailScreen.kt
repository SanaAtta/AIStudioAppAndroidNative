package com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.controller.WallpaperCategoryDetailController
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperItem
import com.aiartgenerator.imagegenerator.videogenerator.view.common.BackScreenTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.NetworkImage
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground

@Composable
fun WallpaperCategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit,
    onWallpaperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember(categoryId) { WallpaperCategoryDetailController(categoryId) }
    val title = controller.category?.title ?: "Category"

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        BackScreenTopBar(
            onBack = onBack,
            title = title,
            showProAndSettings = false,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        ) {
            items(controller.wallpapers, key = { it.id }) { wallpaper ->
                CategoryWallpaperTile(
                    wallpaper = wallpaper,
                    onClick = { onWallpaperClick(wallpaper.id) },
                )
            }
        }
    }
}

@Composable
private fun CategoryWallpaperTile(
    wallpaper: WallpaperItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.62f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        NetworkImage(
            url = wallpaper.imageUrl(),
            contentDescription = wallpaper.title,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
