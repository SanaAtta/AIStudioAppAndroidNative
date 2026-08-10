package com.example.myapplication

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.myapplication.screen.ChatbotScreen
import com.example.myapplication.screen.CoverScreen
import com.example.myapplication.screen.EditScreen
import com.example.myapplication.screen.HomeScreen
import com.example.myapplication.screen.ImageGeneratorScreen
import com.example.myapplication.screen.MusicGeneratorScreen
import com.example.myapplication.screen.ProjectsScreen
import com.example.myapplication.screen.VideoGeneratorScreen
import com.example.myapplication.screen.WallpaperCategoryDetailScreen
import com.example.myapplication.screen.WallpapersScreen
import com.example.myapplication.theme.HomeBackground
import com.example.myapplication.theme.HomeNavBar
import com.example.myapplication.theme.HomeNavSelected
import com.example.myapplication.theme.HomeNavUnselected
import kotlinx.serialization.Serializable

@Serializable
data class WallpaperCategoryRoute(val categoryId: String)

@Composable
fun MainShell(modifier: Modifier = Modifier) {
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
            startDestination = AppRoutes.Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoutes.Home) {
                HomeScreen(
                    onImageGeneratorClick = { navController.navigate(AppRoutes.AiImageGenerator) },
                    onVideoGeneratorClick = { navController.navigate(AppRoutes.AiVideoGenerator) },
                    onMusicGeneratorClick = { navController.navigate(AppRoutes.AiMusicGenerator) },
                    onChatbotClick = { navController.navigate(AppRoutes.AiChatbot) },
                )
            }
            composable(AppRoutes.AiWallpapers) {
                WallpapersScreen(
                    onCategoryClick = { categoryId ->
                        navController.navigate(WallpaperCategoryRoute(categoryId))
                    },
                )
            }
            composable(AppRoutes.AiEdit) { EditScreen() }
            composable(AppRoutes.AiCover) { CoverScreen() }
            composable(AppRoutes.Projects) { ProjectsScreen() }
            composable(AppRoutes.AiImageGenerator) {
                ImageGeneratorScreen(onBack = { navController.popBackStack() })
            }
            composable(AppRoutes.AiVideoGenerator) {
                VideoGeneratorScreen(onBack = { navController.popBackStack() })
            }
            composable(AppRoutes.AiMusicGenerator) {
                MusicGeneratorScreen(onBack = { navController.popBackStack() })
            }
            composable(AppRoutes.AiChatbot) {
                ChatbotScreen(onBack = { navController.popBackStack() })
            }
            composable<WallpaperCategoryRoute> { entry ->
                val route = entry.toRoute<WallpaperCategoryRoute>()
                WallpaperCategoryDetailScreen(
                    categoryId = route.categoryId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
