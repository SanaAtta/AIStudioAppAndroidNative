package com.example.myapplication.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : BottomNavItem(AppRoutes.Home, "Home", Icons.Filled.Home)
    data object AiWallpapers : BottomNavItem(AppRoutes.AiWallpapers, "AI Wallpaper", Icons.Filled.Wallpaper)
    data object AiEdit : BottomNavItem(AppRoutes.AiEdit, "AI Edit", Icons.Filled.Edit)
    data object AiCover : BottomNavItem(AppRoutes.AiCover, "AI Cover", Icons.Filled.AutoAwesome)
    data object Projects : BottomNavItem(AppRoutes.Projects, "Project", Icons.Filled.Folder)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.AiWallpapers,
    BottomNavItem.AiEdit,
    BottomNavItem.AiCover,
    BottomNavItem.Projects,
)
