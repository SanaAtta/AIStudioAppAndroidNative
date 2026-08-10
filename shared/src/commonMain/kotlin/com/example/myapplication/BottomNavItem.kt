package com.example.myapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(AppRoutes.Home, "Home", Icons.Filled.Home),
    BottomNavItem(AppRoutes.AiWallpapers, "AI Wallpaper", Icons.Filled.Wallpaper),
    BottomNavItem(AppRoutes.AiEdit, "AI Edit", Icons.Filled.Edit),
    BottomNavItem(AppRoutes.AiCover, "AI Cover", Icons.Filled.AutoAwesome),
    BottomNavItem(AppRoutes.Projects, "Project", Icons.Filled.Folder),
)
