package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiartgenerator.imagegenerator.videogenerator.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    data object Home : BottomNavItem(AppRoutes.Home, R.string.nav_home, R.drawable.ic_nav_home)
    data object AiCover : BottomNavItem(AppRoutes.AiCover, R.string.nav_ai_cover, R.drawable.ic_nav_cover)
    data object AiEdit : BottomNavItem(AppRoutes.AiEdit, R.string.nav_ai_edit, R.drawable.ic_nav_edit)
    data object AiWallpapers : BottomNavItem(AppRoutes.AiWallpapers, R.string.nav_ai_wallpaper, R.drawable.ic_nav_wallpaper)
    data object Library : BottomNavItem(AppRoutes.Projects, R.string.nav_library, R.drawable.ic_nav_library)
}

val bottomNavStartItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.AiCover,
)

val bottomNavEndItems = listOf(
    BottomNavItem.AiWallpapers,
    BottomNavItem.Library,
)

val bottomNavItems = bottomNavStartItems + bottomNavEndItems

fun BottomNavItem.isSelected(currentRoute: String?): Boolean = currentRoute == route
