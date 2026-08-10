package com.example.myapplication.view.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.controller.AiWallpaperController
import com.example.myapplication.model.WallpaperCategory
import com.example.myapplication.model.WallpaperItem
import com.example.myapplication.view.common.NetworkImage
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeOnBackground
import com.example.myapplication.view.theme.HomeSeeAll
import com.example.myapplication.view.theme.MyApplicationTheme

@Composable
fun AiWallpapersScreen(
    onSeeAllCategories: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onWallpaperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiWallpaperController() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Text(
            text = "AI Wallpaper",
            color = HomeOnBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoriesHeader(onSeeAllClick = onSeeAllCategories)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoriesRow(
                    categories = controller.visibleCategories,
                    onCategoryClick = onCategoryClick,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Trending Wallpapers",
                    color = HomeOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
            }
            items(controller.trending, key = { it.id }) { wallpaper ->
                TrendingWallpaperTile(
                    wallpaper = wallpaper,
                    onClick = { onWallpaperClick(wallpaper.id) },
                )
            }
        }
    }
}

@Composable
private fun CategoriesHeader(
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Explore Categories",
            color = HomeOnBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "See all",
            color = HomeSeeAll,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onSeeAllClick),
        )
    }
}

@Composable
private fun CategoriesRow(
    categories: List<WallpaperCategory>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        categories.forEach { category ->
            CategoryCard(
                category = category,
                onClick = { onCategoryClick(category.id) },
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: WallpaperCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(110.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            category.color.copy(alpha = 0.95f),
                            category.color.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.title,
            color = HomeOnBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TrendingWallpaperTile(
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiWallpapersScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiWallpapersScreen(
            onSeeAllCategories = {},
            onCategoryClick = {},
            onWallpaperClick = {},
        )
    }
}
