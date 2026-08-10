package com.example.myapplication.view.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.model.AppRoutes
import com.example.myapplication.model.BottomNavItem
import com.example.myapplication.model.bottomNavItems
import com.example.myapplication.view.chat.AiChatbotScreen
import com.example.myapplication.view.cover.AiCoverScreen
import com.example.myapplication.view.edit.AiEditScreen
import com.example.myapplication.view.generator.AiImageGeneratorScreen
import com.example.myapplication.view.home.HomeScreen
import com.example.myapplication.view.music.AiMusicGeneratorScreen
import com.example.myapplication.view.premium.PremiumScreen
import com.example.myapplication.view.projects.ProjectsScreen
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeNavBar
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeNavUnselected
import com.example.myapplication.view.video.AiVideoGeneratorScreen
import com.example.myapplication.view.wallpaper.AiWallpapersScreen
import com.example.myapplication.view.wallpaper.WallpaperCategoriesScreen
import com.example.myapplication.view.wallpaper.WallpaperCategoryDetailScreen
import com.example.myapplication.view.wallpaper.WallpaperDetailScreen

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in AppRoutes.bottomNavRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HomeBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = HomeNavBar,
                    contentColor = HomeNavUnselected,
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HomeNavSelected,
                                selectedTextColor = HomeNavSelected,
                                unselectedIconColor = HomeNavUnselected,
                                unselectedTextColor = HomeNavUnselected,
                                indicatorColor = HomeNavBar,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onImageGeneratorClick = {
                        navController.navigate(AppRoutes.AiImageGenerator)
                    },
                    onVideoGeneratorClick = {
                        navController.navigate(AppRoutes.AiVideoGenerator)
                    },
                    onMusicGeneratorClick = {
                        navController.navigate(AppRoutes.AiMusicGenerator)
                    },
                    onChatbotClick = {
                        navController.navigate(AppRoutes.AiChatbot)
                    },
                )
            }
            composable(BottomNavItem.AiWallpapers.route) {
                AiWallpapersScreen(
                    onSeeAllCategories = {
                        navController.navigate(AppRoutes.WallpaperCategories)
                    },
                    onCategoryClick = { categoryId ->
                        navController.navigate(AppRoutes.categoryDetail(categoryId))
                    },
                    onWallpaperClick = { wallpaperId ->
                        navController.navigate(AppRoutes.wallpaperDetail(wallpaperId))
                    },
                )
            }
            composable(BottomNavItem.AiEdit.route) { AiEditScreen() }
            composable(BottomNavItem.AiCover.route) { AiCoverScreen() }
            composable(BottomNavItem.Projects.route) { ProjectsScreen() }
            composable(AppRoutes.AiImageGenerator) {
                AiImageGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onProClick = { navController.navigate(AppRoutes.Premium) },
                )
            }
            composable(AppRoutes.AiVideoGenerator) {
                AiVideoGeneratorScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.AiMusicGenerator) {
                AiMusicGeneratorScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.AiChatbot) {
                AiChatbotScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.Premium) {
                PremiumScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.WallpaperCategories) {
                WallpaperCategoriesScreen(
                    onCategoryClick = { categoryId ->
                        navController.navigate(AppRoutes.categoryDetail(categoryId))
                    },
                )
            }
            composable(
                route = AppRoutes.WallpaperCategoryDetail,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType },
                ),
            ) { entry ->
                val categoryId = entry.arguments?.getString("categoryId").orEmpty()
                WallpaperCategoryDetailScreen(
                    categoryId = categoryId,
                    onBack = { navController.popBackStack() },
                    onWallpaperClick = { wallpaperId ->
                        navController.navigate(AppRoutes.wallpaperDetail(wallpaperId))
                    },
                )
            }
            composable(
                route = AppRoutes.WallpaperDetail,
                arguments = listOf(
                    navArgument("wallpaperId") { type = NavType.StringType },
                ),
            ) { entry ->
                val wallpaperId = entry.arguments?.getString("wallpaperId").orEmpty()
                WallpaperDetailScreen(
                    wallpaperId = wallpaperId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
