package com.example.myapplication.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ads.AdsRemoteConfig

private object SetupRoutes {
    const val Splash = "setup_splash"
    const val Language = "setup_language"
    const val Onboarding = "setup_onboarding"
}

@Composable
fun SetupNavHost(onSetupFinished: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        AdsRemoteConfig.ensureLocalDefaultsApplied()
        AdsRemoteConfig.refreshAdsControlForSetupFlow()
    }

    NavHost(navController = navController, startDestination = SetupRoutes.Splash) {
        composable(SetupRoutes.Splash) {
            SetupSplashScreen(
                onContinue = {
                    when {
                        !SetupPrefs.isLanguageDone(context) ->
                            navController.navigate(SetupRoutes.Language) {
                                popUpTo(SetupRoutes.Splash) { inclusive = true }
                            }
                        !SetupPrefs.isOnboardingDone(context) ->
                            navController.navigate(SetupRoutes.Onboarding) {
                                popUpTo(SetupRoutes.Splash) { inclusive = true }
                            }
                        else -> onSetupFinished()
                    }
                },
            )
        }
        composable(SetupRoutes.Language) {
            SetupLanguageScreen(
                onDone = {
                    if (!SetupPrefs.isOnboardingDone(context)) {
                        navController.navigate(SetupRoutes.Onboarding) {
                            popUpTo(SetupRoutes.Language) { inclusive = true }
                        }
                    } else {
                        onSetupFinished()
                    }
                },
            )
        }
        composable(SetupRoutes.Onboarding) {
            SetupOnboardingScreen(onFinished = onSetupFinished)
        }
    }
}
