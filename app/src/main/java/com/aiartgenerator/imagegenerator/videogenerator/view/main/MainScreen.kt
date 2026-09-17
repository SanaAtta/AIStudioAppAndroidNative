package com.aiartgenerator.imagegenerator.videogenerator.view.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import com.aiartgenerator.imagegenerator.videogenerator.analytics.AppAnalytics
import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.FeatureInterstitial
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.model.AppRoutes
import com.aiartgenerator.imagegenerator.videogenerator.model.BottomNavItem
import com.aiartgenerator.imagegenerator.videogenerator.model.EditResultStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCreationsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedImageStore
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguagePreferences
import com.aiartgenerator.imagegenerator.videogenerator.model.LibraryRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.LibrarySource
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LocalOnProClick
import com.aiartgenerator.imagegenerator.videogenerator.view.common.LocalOnSettingsClick
import com.aiartgenerator.imagegenerator.videogenerator.view.chat.AiChatbotScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.cover.AiCoverScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.cover.CoverNowPlayingScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.edit.AiEditResultScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.edit.AiEditScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.exit.ExitConfirmationScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.favorites.FavouritesScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.generator.AiImageGeneratorScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.generator.ImageGenerationResultScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GeneratingScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.home.HomeScreen
import com.aiartgenerator.imagegenerator.videogenerator.model.GenerationSession
import com.aiartgenerator.imagegenerator.videogenerator.view.language.ChooseLanguageScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.music.AiMusicGeneratorScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.music.MusicNowPlayingScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.premium.PremiumScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.projects.ProjectsScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.settings.SettingsScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.video.AiVideoGeneratorScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.video.VideoGenerationResultScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper.AiWallpapersScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper.WallpaperCategoriesScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper.WallpaperCategoryDetailScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.wallpaper.WallpaperDetailScreen

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in AppRoutes.bottomNavRoutes
    val context = LocalContext.current
    val activity = context as? Activity
    var showExitScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = !showExitScreen) {
        if (navController.popBackStack()) return@BackHandler
        showExitScreen = true
    }

    fun navigateHomeClear() {
        val arrived = navController.popBackStack(BottomNavItem.Home.route, inclusive = false)
        if (!arrived) {
            navController.navigate(BottomNavItem.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    fun openExternalUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPlayStoreForRate() {
        val packageName = context.packageName
        val marketUri = Uri.parse("market://details?id=$packageName")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, marketUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.recoverCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.onFailure {
            Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareApp() {
        val packageName = context.packageName
        val playUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val appName = context.getString(R.string.app_name)
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, appName)
                putExtra(Intent.EXTRA_TEXT, "$appName\n$playUrl")
            }
        runCatching {
            context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.settings_share_app)),
            )
        }.onFailure {
            Toast.makeText(context, "Unable to share app", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateWithInterstitial(
        enabled: Boolean,
        slotLabel: String,
        navigate: () -> Unit,
    ) {
        AppAnalytics.click(itemId = slotLabel)
        FeatureInterstitial.showThen(
            activity = activity,
            enabled = enabled,
            unitId = AdUnitIds.genericInterstitial(),
            slotLabel = slotLabel,
            rcParam = AdUnitIds.Rc.INTERSTITIAL,
            onContinue = navigate,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalOnProClick provides trackedClick("pro_badge") {
                if (!PremiumAccess.isPremiumUser()) {
                    navController.navigate(AppRoutes.Premium)
                }
            },
            LocalOnSettingsClick provides {
                navigateWithInterstitial(
                    enabled = AdsControl.settingsInterstitialEnabled(),
                    slotLabel = "settings interstitial",
                ) {
                    navController.navigate(AppRoutes.Settings)
                }
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBottomBar) BottomNavSpec.totalHeight() else 0.dp),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
            composable(BottomNavItem.Home.route) {
                val homeContext = LocalContext.current
                HomeScreen(
                    onImageGeneratorClick = {
                        navigateWithInterstitial(
                            enabled = AdsControl.imageGeneratorInterstitialEnabled(),
                            slotLabel = "image generator card interstitial",
                        ) {
                            navController.navigate(AppRoutes.AiImageGenerator)
                        }
                    },
                    onVideoGeneratorClick = {
                        navigateWithInterstitial(
                            enabled = AdsControl.videoGeneratorInterstitialEnabled(),
                            slotLabel = "video generator card interstitial",
                        ) {
                            navController.navigate(AppRoutes.AiVideoGenerator)
                        }
                    },
                    onMusicGeneratorClick = {
                        navigateWithInterstitial(
                            enabled = AdsControl.musicGeneratorInterstitialEnabled(),
                            slotLabel = "music generator card interstitial",
                        ) {
                            navController.navigate(AppRoutes.AiMusicGenerator)
                        }
                    },
                    onChatbotClick = trackedClick("home_chatbot") {
                        navController.navigate(AppRoutes.AiChatbot)
                    },
                    onSeeAllCreationsClick = trackedClick("home_see_all") {
                        navController.navigate(BottomNavItem.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onItemClick = { item ->
                        AppAnalytics.click(itemId = "home_recent_item", extra = item.source.name)
                        if (LibraryRepository.openItem(homeContext, item)) {
                            when (item.source) {
                                LibrarySource.Creation ->
                                    navController.navigate(AppRoutes.ImageGenerationResult)
                                LibrarySource.Video ->
                                    navController.navigate(AppRoutes.VideoGenerationResult)
                                LibrarySource.Music ->
                                    navController.navigate(AppRoutes.MusicNowPlaying)
                                LibrarySource.Cover ->
                                    navController.navigate(AppRoutes.CoverNowPlaying)
                            }
                        }
                    },
                )
            }
            composable(BottomNavItem.AiWallpapers.route) {
                AiWallpapersScreen(
                    onGenerateClick = {
                        navigateWithInterstitial(
                            enabled = AdsControl.imageGeneratorInterstitialEnabled(),
                            slotLabel = "image generator card interstitial",
                        ) {
                            navController.navigate(AppRoutes.AiImageGenerator)
                        }
                    },
                    onWallpaperClick = { wallpaperId ->
                        navController.navigate(AppRoutes.wallpaperDetail(wallpaperId))
                    },
                )
            }
            composable(BottomNavItem.AiCover.route) {
                AiCoverScreen(
                    onGenerating = { navController.navigate(AppRoutes.generating("cover")) },
                    onResult = { navController.navigate(AppRoutes.CoverNowPlaying) },
                )
            }
            composable(BottomNavItem.AiEdit.route) {
                AiEditScreen(
                    onBack = {
                        if (!navController.popBackStack()) {
                            navigateHomeClear()
                        }
                    },
                    onResult = { navController.navigate(AppRoutes.AiEditResult) },
                )
            }
            composable(AppRoutes.CoverNowPlaying) {
                CoverNowPlayingScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.AiEditResult) {
                AiEditResultScreen(
                    onBack = {
                        EditResultStore.requestApplyEditedImageOnResume()
                        navController.popBackStack()
                    },
                )
            }
            composable(BottomNavItem.Library.route) {
                ProjectsScreen(
                    onImageClick = { navController.navigate(AppRoutes.ImageGenerationResult) },
                    onVideoClick = { navController.navigate(AppRoutes.VideoGenerationResult) },
                    onMusicClick = { navController.navigate(AppRoutes.MusicNowPlaying) },
                    onCoverClick = { navController.navigate(AppRoutes.CoverNowPlaying) },
                )
            }
            composable(AppRoutes.AiImageGenerator) {
                AiImageGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onGenerating = { navController.navigate(AppRoutes.generating("image")) },
                    onResult = { navController.navigate(AppRoutes.ImageGenerationResult) },
                )
            }
            composable(
                route = AppRoutes.Generating,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) {
                GeneratingScreen(
                    onSuccessNavigate = { kind ->
                        val resultRoute =
                            when (kind) {
                                GenerationSession.Kind.Image -> AppRoutes.ImageGenerationResult
                                GenerationSession.Kind.Video -> AppRoutes.VideoGenerationResult
                                GenerationSession.Kind.Music -> AppRoutes.MusicNowPlaying
                                GenerationSession.Kind.Cover -> AppRoutes.CoverNowPlaying
                            }
                        val generatorRoute =
                            when (kind) {
                                GenerationSession.Kind.Image -> AppRoutes.AiImageGenerator
                                GenerationSession.Kind.Video -> AppRoutes.AiVideoGenerator
                                GenerationSession.Kind.Music -> AppRoutes.AiMusicGenerator
                                GenerationSession.Kind.Cover -> AppRoutes.AiCover
                            }
                        navController.navigate(resultRoute) {
                            popUpTo(generatorRoute) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onErrorBack = { navController.popBackStack() },
                )
            }

            composable(AppRoutes.ImageGenerationResult) {
                ImageGenerationResultScreen(
                    onBack = { navController.popBackStack() },
                    onHome = {
                        navigateWithInterstitial(
                            enabled = AdsControl.generateResultHomeInterstitialEnabled(),
                            slotLabel = "generate result home interstitial",
                        ) {
                            navigateHomeClear()
                        }
                    },
                    onAiEdit = {
                        EditResultStore.prepareFromGeneratedImage(GeneratedImageStore.current?.uri)
                        navController.navigate(AppRoutes.AiEdit) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppRoutes.AiVideoGenerator) {
                AiVideoGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onGenerating = { navController.navigate(AppRoutes.generating("video")) },
                    onResult = { navController.navigate(AppRoutes.VideoGenerationResult) },
                )
            }
            composable(AppRoutes.VideoGenerationResult) {
                VideoGenerationResultScreen(
                    onBack = { navController.popBackStack() },
                    onHome = {
                        navigateWithInterstitial(
                            enabled = AdsControl.generateResultHomeInterstitialEnabled(),
                            slotLabel = "generate result home interstitial",
                        ) {
                            navigateHomeClear()
                        }
                    },
                    onEdit = {
                        navController.popBackStack(AppRoutes.AiVideoGenerator, inclusive = true)
                        navController.navigate(AppRoutes.AiVideoGenerator)
                    },
                )
            }
            composable(AppRoutes.AiMusicGenerator) {
                AiMusicGeneratorScreen(
                    onBack = { navController.popBackStack() },
                    onGenerating = { navController.navigate(AppRoutes.generating("music")) },
                    onResult = { navController.navigate(AppRoutes.MusicNowPlaying) },
                )
            }
            composable(AppRoutes.MusicNowPlaying) {
                MusicNowPlayingScreen(
                    onBack = { navController.popBackStack() },
                    onHome = {
                        navigateWithInterstitial(
                            enabled = AdsControl.generateResultHomeInterstitialEnabled(),
                            slotLabel = "generate result home interstitial",
                        ) {
                            navigateHomeClear()
                        }
                    },
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
                    onSubscribe = { navController.popBackStack() },
                )
            }
            composable(AppRoutes.Settings) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeClick = {
                        if (!PremiumAccess.isPremiumUser()) {
                            navController.navigate(AppRoutes.Premium)
                        }
                    },
                    onLanguageClick = { navController.navigate(AppRoutes.SettingsLanguage) },
                    onFavouritesClick = { navController.navigate(AppRoutes.Favourites) },
                    isDarkMode = isDarkTheme,
                    onDarkModeChange = onDarkModeChange,
                    onItemClick = { itemId ->
                        when (itemId) {
                            "privacy" -> openExternalUrl(AdsControl.privacyPolicyUrl())
                            "terms" -> openExternalUrl(AdsControl.termsUrl())
                            "rate" -> openPlayStoreForRate()
                            "share" -> shareApp()
                            else -> Unit
                        }
                    },
                )
            }
            composable(AppRoutes.Favourites) {
                FavouritesScreen(
                    onBack = { navController.popBackStack() },
                    onImageClick = { navController.navigate(AppRoutes.ImageGenerationResult) },
                    onVideoClick = { navController.navigate(AppRoutes.VideoGenerationResult) },
                    onMusicClick = { navController.navigate(AppRoutes.MusicNowPlaying) },
                    onCoverClick = { navController.navigate(AppRoutes.CoverNowPlaying) },
                )
            }
            composable(AppRoutes.SettingsLanguage) {
                val context = LocalContext.current
                ChooseLanguageScreen(
                    initialSelectedId = LanguagePreferences.getSelectedLanguageId(context),
                    onDone = { languageId ->
                        LanguagePreferences.setSelectedLanguageId(context, languageId)
                        (context as? Activity)?.recreate()
                        navController.popBackStack()
                    },
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

        if (showBottomBar) {
            AppBottomBar(
                currentRoute = currentRoute,
                onItemClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCreateClick = {
                    EditResultStore.prepareDirectEntry()
                    navController.navigate(AppRoutes.AiEdit) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        }

        if (showExitScreen) {
            ExitConfirmationScreen(
                onStay = { showExitScreen = false },
                onExit = {
                    showExitScreen = false
                    activity?.finish()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
