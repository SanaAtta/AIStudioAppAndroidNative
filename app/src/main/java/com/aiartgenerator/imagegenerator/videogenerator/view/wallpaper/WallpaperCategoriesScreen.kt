package com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.controller.WallpaperCategoriesController
import com.aiartgenerator.imagegenerator.videogenerator.model.WallpaperCategory
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground

@Composable
fun WallpaperCategoriesScreen(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { WallpaperCategoriesController() }

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Categories",
                color = HomeOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 14.dp),
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (metrics.screenWidthDp >= 600) 3 else 2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
        ) {
            items(controller.categories, key = { it.id }) { category ->
                CategoryGridCard(
                    category = category,
                    onClick = { onCategoryClick(category.id) },
                )
            }
        }
        }
    }
}

@Composable
private fun CategoryGridCard(
    category: WallpaperCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            category.color.copy(alpha = 0.95f),
                            category.color.copy(alpha = 0.5f),
                        ),
                    ),
                ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.title,
            color = HomeOnBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
