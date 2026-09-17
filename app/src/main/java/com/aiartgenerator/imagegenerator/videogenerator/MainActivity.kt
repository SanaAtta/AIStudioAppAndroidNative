package com.aiartgenerator.imagegenerator.videogenerator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.FeatureInterstitial
import com.aiartgenerator.imagegenerator.videogenerator.ads.ResumeAppOpenCoordinator
import com.aiartgenerator.imagegenerator.videogenerator.analytics.AppAnalytics
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.billing.SubscriptionSync
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguagePreferences
import com.aiartgenerator.imagegenerator.videogenerator.model.LocaleHelper
import com.aiartgenerator.imagegenerator.videogenerator.model.OnboardingPreferences
import com.aiartgenerator.imagegenerator.videogenerator.model.ThemePreferences
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppScreenBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResumeAdLoadingScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.common.SplashScreenBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.language.ChooseLanguageScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.main.MainScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.onboarding.OnboardingScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.premium.PremiumScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.splash.SplashScreen
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppThemeProvider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme

/**
 * App entry point.
 *
 * Launch flow:
 * - First time: Splash -> Language -> Onboarding -> Premium -> Home
 * - Returning: Splash -> Premium -> Home
 */
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SubscriptionSync.attachActivity(this)
        SubscriptionSync.refreshPurchases()
        ResumeAppOpenCoordinator.bindActivity(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var isDarkTheme by remember { mutableStateOf(ThemePreferences.isDarkMode(context)) }
            val showResumeAdLoading by ResumeAppOpenCoordinator.showLoading.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppThemeProvider(isDarkTheme = isDarkTheme) {
                    var languageCompleted by remember {
                        mutableStateOf(LanguagePreferences.isCompleted(context))
                    }
                    var onboardingCompleted by remember {
                        mutableStateOf(OnboardingPreferences.isCompleted(context))
                    }

                    var splashFinished by rememberSaveable { mutableStateOf(false) }
                    var premiumDismissed by remember { mutableStateOf(false) }
                    val adsConfigRev by AdsConfigRevision.state
                    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()
                    val showLaunchPremium =
                        remember(adsConfigRev, premiumDismissed, isPremium) {
                            !premiumDismissed && !isPremium && AdsControl.showPremiumAfterOnboarding()
                        }

                    val inMainApp =
                        splashFinished &&
                            languageCompleted &&
                            onboardingCompleted &&
                            !showLaunchPremium

                    LaunchedEffect(inMainApp) {
                        ResumeAppOpenCoordinator.setHostReadyForResumeAds(inMainApp)
                    }

                    fun completeOnboarding() {
                        AppAnalytics.click(itemId = "onboarding_done")
                        OnboardingPreferences.setCompleted(context)
                        onboardingCompleted = true
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            !splashFinished -> {
                                SplashScreenBackground(modifier = Modifier.fillMaxSize()) {
                                    SplashScreen(
                                        onFinished = { splashFinished = true },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            !languageCompleted -> {
                                AppScreenBackground(modifier = Modifier.fillMaxSize()) {
                                    ChooseLanguageScreen(
                                        initialSelectedId = LanguagePreferences.getSelectedLanguageId(context),
                                        onDone = { languageId ->
                                            AppAnalytics.click(itemId = "language_done", extra = languageId)
                                            LanguagePreferences.setSelectedLanguageId(context, languageId)
                                            LanguagePreferences.setCompleted(context)
                                            languageCompleted = true
                                            recreate()
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            !onboardingCompleted -> {
                                SplashScreenBackground(modifier = Modifier.fillMaxSize()) {
                                    OnboardingScreen(
                                        onFinished = {
                                            FeatureInterstitial.showThen(
                                                activity = this@MainActivity,
                                                enabled = AdsControl.onboardingLastPageInterstitialEnabled(),
                                                unitId = AdUnitIds.onboardingInterstitial(),
                                                slotLabel = "onboarding interstitial",
                                                rcParam = AdUnitIds.Rc.ONBOARDING_INTERSTITIAL,
                                                onContinue = { completeOnboarding() },
                                            )
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            showLaunchPremium -> {
                                AppScreenBackground(modifier = Modifier.fillMaxSize()) {
                                    PremiumScreen(
                                        onBack = {
                                            AppAnalytics.click(itemId = "premium_close")
                                            premiumDismissed = true
                                        },
                                        onSubscribe = {
                                            AppAnalytics.click(itemId = "premium_subscribe")
                                            premiumDismissed = true
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            else -> {
                                AppScreenBackground(modifier = Modifier.fillMaxSize()) {
                                    MainScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        isDarkTheme = isDarkTheme,
                                        onDarkModeChange = { enabled ->
                                            AppAnalytics.click(
                                                itemId = "settings_dark_mode",
                                                extra = if (enabled) "on" else "off",
                                            )
                                            ThemePreferences.setDarkMode(context, enabled)
                                            isDarkTheme = enabled
                                        },
                                    )
                                }
                            }
                        }

                        if (showResumeAdLoading) {
                            ResumeAdLoadingScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SubscriptionSync.attachActivity(this)
        SubscriptionSync.refreshPurchases()
        ResumeAppOpenCoordinator.bindActivity(this)
    }

    override fun onDestroy() {
        SubscriptionSync.detachActivity(this)
        super.onDestroy()
    }
}
