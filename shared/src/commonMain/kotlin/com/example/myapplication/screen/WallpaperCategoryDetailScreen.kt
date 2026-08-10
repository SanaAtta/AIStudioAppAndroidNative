package com.example.myapplication.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.WallpaperCatalog
import com.example.myapplication.theme.HomeBackground
import com.example.myapplication.theme.HomeMuted
import com.example.myapplication.theme.HomeOnBackground
import com.example.myapplication.theme.toComposeColor
import com.example.myapplication.ui.BackTopBar

@Composable
fun WallpaperCategoryDetailScreen(
    categoryId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = WallpaperCatalog.categoryById(categoryId)
    val wallpapers = WallpaperCatalog.wallpapersByCategory(categoryId).take(4)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        BackTopBar(
            title = category?.title ?: "Category",
            onBack = onBack,
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        if (wallpapers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "No wallpapers found", color = HomeMuted, fontSize = 15.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(wallpapers, key = { it.id }) { wallpaper ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.55f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(wallpaper.colorArgb.toComposeColor())
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(
                            text = wallpaper.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
