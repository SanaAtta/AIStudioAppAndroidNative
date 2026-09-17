package com.aiartgenerator.imagegenerator.videogenerator.view.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConsentManager
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsLoadLog
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ads.AppOpenAdManager
import com.aiartgenerator.imagegenerator.videogenerator.ads.MobileAdsInitializer
import com.aiartgenerator.imagegenerator.videogenerator.ads.SplashInterstitial
import com.aiartgenerator.imagegenerator.videogenerator.ads.SplashNativeAdManager
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.SplashScreenAdSlot
import com.aiartgenerator.imagegenerator.videogenerator.view.common.SplashScreenBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppThemeProvider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashProgressTrack
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.actionGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

private const val SplashTotalDurationMs = 12_000L
private const val SplashConsentBudgetMs = 5_000L
private const val SplashRcTimeoutMs = 8_000L
private const val SplashAdsTag = "SplashAds"

private fun splashAdsLog(message: String) {
    if (BuildConfig.DEBUG || AdsControl.adsDebugLogEnabled()) {
        Log.i(SplashAdsTag, message)
        AdsLoadLog.trace("splash diag", message)
    }
}

private fun logSplashAdSnapshot(stage: String) {
    val top = AdsControl.splashTopAdSlotFormat()
    val bottom = AdsControl.splashBottomAdSlotFormat()
    splashAdsLog(
        "$stage | " +
            "adsConfigReady=${AdsRemoteConfig.isAdsConfigReady()} " +
            "shouldShowAds=${AdsControl.shouldShowAds()} " +
            "canRequestAds=${AdsConsentManager.canRequestAds()} " +
            "premium=${PremiumAccess.isPremiumUser()} " +
            "allAdsOn=${AdsControl.allAdsOn()} " +
            "top(visible=${top.visible},banner=${top.adaptiveBanner},native=${top.nativeTemplate}) " +
            "bottom(visible=${bottom.visible},banner=${bottom.adaptiveBanner},native=${bottom.nativeTemplate}) " +
            "showTop=${AdsControl.splashShowTopAdSlot()} " +
            "showBottom=${AdsControl.splashShowBottomAdSlot()} " +
            "appOpen=${AdsControl.splashAppOpenEnabled()} " +
            "interstitial=${AdsControl.splashInterstitialEnabled()} " +
            "bannerUnit=${AdUnitIds.splashAdaptiveBanner()} " +
            "nativeUnit=${AdUnitIds.splashNative()} " +
            "appOpenUnit=${AdUnitIds.splashAppOpen()} " +
            "mobileAdsReady=${MobileAdsInitializer.isReady}",
    )
}

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val adsConfigRev by AdsConfigRevision.state
    var splashAdsReady by remember { mutableStateOf(false) }
    var showStartButton by remember { mutableStateOf(false) }
    var animateProgress by remember { mutableStateOf(false) }
    var splashProgressDurationMs by remember { mutableIntStateOf(SplashTotalDurationMs.toInt()) }
    val hasNetwork = remember { context.hasNetwork() }
    var consentPhaseActive by remember { mutableStateOf(hasNetwork) }
    val splashFlowMutex = remember { Mutex() }
    val onContinueState by rememberUpdatedState(onFinished)
    val loadingProgress = remember { Animatable(0f) }

    val splashTopAd =
        remember(splashAdsReady, adsConfigRev) {
            if (splashAdsReady) AdsControl.splashTopAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
        }
    val splashBottomAd =
        remember(splashAdsReady, adsConfigRev) {
            if (splashAdsReady) AdsControl.splashBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
        }
    val showSplashTopSlot =
        remember(splashAdsReady, adsConfigRev) {
            splashAdsReady && AdsControl.splashShowTopAdSlot()
        }
    val showSplashBottomSlot =
        remember(splashAdsReady, adsConfigRev) {
            splashAdsReady && AdsControl.splashShowBottomAdSlot()
        }
    val splashNativeUnitId =
        remember(splashAdsReady) { if (splashAdsReady) AdUnitIds.splashNative() else "" }
    val splashBannerUnitId =
        remember(splashAdsReady) { if (splashAdsReady) AdUnitIds.splashAdaptiveBanner() else "" }

    DisposableEffect(Unit) {
        onDispose { SplashNativeAdManager.releaseSession() }
    }

    val linearProgress by animateFloatAsState(
        targetValue = if (animateProgress && hasNetwork && !consentPhaseActive) 1f else 0f,
        animationSpec = tween(durationMillis = splashProgressDurationMs, easing = LinearEasing),
        label = "splashLinearProgress",
    )

    LaunchedEffect(animateProgress, splashProgressDurationMs, hasNetwork, consentPhaseActive) {
        if (animateProgress && hasNetwork && !consentPhaseActive) {
            loadingProgress.snapTo(0f)
            loadingProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = splashProgressDurationMs,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    LaunchedEffect(hasNetwork) {
        splashAdsLog("splash start hasNetwork=$hasNetwork")
        if (!hasNetwork) {
            splashAdsLog("NO NETWORK — skipping ads, continue after delay")
            showStartButton = false
            splashAdsReady = false
            animateProgress = false
            consentPhaseActive = false
            delay(1_200)
            onContinueState()
            return@LaunchedEffect
        }

        splashFlowMutex.withLock {
            val flowStartMs = SystemClock.elapsedRealtime()
            splashAdsReady = false
            showStartButton = false
            animateProgress = false
            consentPhaseActive = true
            val act = context as? Activity
            splashAdsLog(
                "consent phase begin activity=${act != null} " +
                    "totalTimerMs=$SplashTotalDurationMs",
            )

            AdsRemoteConfig.ensureLocalDefaultsApplied()
            logSplashAdSnapshot("after_local_defaults")

            coroutineScope {
                // Fetch ads RC as early as possible (parallel with UMP consent).
                val adsRcJob =
                    async {
                        AdsRemoteConfig.refreshAdsControlForSplash()
                    }
                val apiRcJob = async { ApiRemoteConfig.fetchAndActivate() }

                if (act != null) {
                    runCatching {
                        withTimeoutOrNull(SplashConsentBudgetMs) {
                            AdsConsentManager.gatherConsent(act)
                        }.also { timedOut ->
                            if (timedOut == null) {
                                splashAdsLog("consent TIMEOUT after ${SplashConsentBudgetMs}ms")
                            } else {
                                splashAdsLog(
                                    "consent finished canRequestAds=${AdsConsentManager.canRequestAds()}",
                                )
                            }
                        }
                    }.onFailure {
                        splashAdsLog("consent ERROR ${it.message}")
                    }
                } else {
                    splashAdsLog("consent SKIPPED — context is not Activity")
                }
                consentPhaseActive = false

                val elapsedMs = SystemClock.elapsedRealtime() - flowStartMs
                val progressMs =
                    (SplashTotalDurationMs - elapsedMs)
                        .coerceAtLeast(1_000L)
                        .toInt()
                splashProgressDurationMs = progressMs
                animateProgress = true
                splashAdsLog(
                    "progress window ${progressMs}ms " +
                        "(elapsed=${elapsedMs}ms / total=${SplashTotalDurationMs}ms)",
                )

                MobileAdsInitializer.start(appContext)
                splashAdsLog("MobileAdsInitializer.start() called")

                val progressMsLong = progressMs.toLong()
                val progressComplete = async { delay(progressMsLong) }

                // Prefer awaiting RC before binding splash ad slots (within remaining budget).
                val rcWaitMs =
                    maxOf(SplashRcTimeoutMs, progressMsLong).coerceAtMost(SplashTotalDurationMs)
                val rcResult =
                    runCatching {
                        withTimeoutOrNull(rcWaitMs) {
                            adsRcJob.await()
                            true
                        }
                    }
                splashAdsLog(
                    "RC refresh done success=${rcResult.isSuccess} " +
                        "timedOut=${rcResult.getOrNull() == null && rcResult.isSuccess} " +
                        "adsConfigReady=${AdsRemoteConfig.isAdsConfigReady()} " +
                        "error=${rcResult.exceptionOrNull()?.message}",
                )
                // If still running past timeout, keep job in background but use whatever is applied.
                if (rcResult.getOrNull() == null) {
                    splashAdsLog("RC await timed out — using applied/default ads_control")
                }
                // apiRcJob already running in parallel from splash start.
                logSplashAdSnapshot("after_rc_refresh")

                val showTop = AdsControl.splashShowTopAdSlot()
                val showBottom = AdsControl.splashShowBottomAdSlot()
                if (showTop || showBottom) {
                    splashAdsReady = true
                    splashAdsLog("splashAdsReady=true showTop=$showTop showBottom=$showBottom")
                } else {
                    splashAdsReady = false
                    splashAdsLog(
                        "splashAdsReady=false — NO SLOT VISIBLE " +
                            "(check shouldShowAds / RC splash.top|bottom flags)",
                    )
                }

                val splashInterstitialActive = AdsControl.splashInterstitialEnabled()
                val splashAppOpenActive =
                    act != null &&
                        AdsControl.splashAppOpenEnabled() &&
                        !splashInterstitialActive
                splashAdsLog(
                    "fullscreen interstitial=$splashInterstitialActive " +
                        "appOpen=$splashAppOpenActive",
                )

                if (splashInterstitialActive) {
                    val unitId = AdUnitIds.splashInterstitial()
                    splashAdsLog("preload interstitial unitId=$unitId")
                    if (unitId.isNotBlank()) {
                        SplashInterstitial.preloadIfNeeded(
                            appContext,
                            unitId,
                            "splash interstitial",
                            AdUnitIds.Rc.SPLASH_INTERSTITIAL,
                        )
                    }
                }
                if (splashAppOpenActive) {
                    splashAdsLog("startSplashLoad appOpen unit=${AdUnitIds.splashAppOpen()}")
                    AppOpenAdManager.startSplashLoad(appContext)
                }

                suspend fun showSplashAppOpenAndWait() {
                    splashAdsLog("SHOW app-open now")
                    val done = CompletableDeferred<Unit>()
                    AppOpenAdManager.showSplashAppOpenIfPreloaded(
                        act!!,
                        Runnable { if (!done.isCompleted) done.complete(Unit) },
                    )
                    done.await()
                    splashAdsLog("app-open closed/finished")
                }

                when {
                    splashInterstitialActive -> {
                        progressComplete.await()
                        splashAdsLog("progress done — show Let's Start for interstitial")
                        showStartButton = true
                    }
                    splashAppOpenActive -> {
                        val loaded =
                            AppOpenAdManager.awaitSplashAppOpenReady(appContext, progressMsLong)
                        splashAdsLog("awaitSplashAppOpenReady loaded=$loaded")
                        if (loaded) {
                            showSplashAppOpenAndWait()
                        } else {
                            progressComplete.await()
                            if (AppOpenAdManager.isSplashAppOpenReady()) {
                                showSplashAppOpenAndWait()
                            } else {
                                splashAdsLog("app-open NOT ready after progress — skip show")
                            }
                        }
                        onContinueState()
                    }
                    else -> {
                        progressComplete.await()
                        splashAdsLog("no fullscreen ad — navigate after progress")
                        onContinueState()
                    }
                }
            }
        }
    }

    LaunchedEffect(showSplashTopSlot, showSplashBottomSlot, splashBannerUnitId, splashNativeUnitId) {
        splashAdsLog(
            "compose slots showTop=$showSplashTopSlot showBottom=$showSplashBottomSlot " +
                "bannerUnit=$splashBannerUnitId nativeUnit=$splashNativeUnitId " +
                "topFmt=$splashTopAd bottomFmt=$splashBottomAd",
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Logo + title + tagline as one tight brand group (ads must not shift it).
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_img),
                contentDescription = stringResource(R.string.splash_title),
                modifier = Modifier.size(148.dp),
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = stringResource(R.string.splash_title),
                color = SplashOnBackground,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                color = SplashMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.1.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (showSplashTopSlot) {
            SplashScreenAdSlot(
                slotEnabled = true,
                adaptiveBannerEnabled = splashTopAd.adaptiveBanner,
                bannerUnitId = splashBannerUnitId,
                nativeTemplate = splashTopAd.nativeTemplate,
                nativeUnitId = splashNativeUnitId,
                nativeSlotLabel = "splash native top ad",
                nativeRcParam = AdUnitIds.Rc.SPLASH_NATIVE,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showSplashBottomSlot) {
                SplashScreenAdSlot(
                    slotEnabled = true,
                    adaptiveBannerEnabled = splashBottomAd.adaptiveBanner,
                    bannerUnitId = splashBannerUnitId,
                    nativeTemplate = splashBottomAd.nativeTemplate,
                    nativeUnitId = splashNativeUnitId,
                    nativeSlotLabel = "splash native bottom ad",
                    nativeRcParam = AdUnitIds.Rc.SPLASH_NATIVE,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showStartButton) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .actionGradientBackground(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val act = context as? Activity
                                        val unitId = AdUnitIds.splashInterstitial()
                                        if (act != null && unitId.isNotBlank()) {
                                            SplashInterstitial.showThen(
                                                activity = act,
                                                unitId = unitId,
                                                slotLabel = "splash interstitial",
                                                rcParam = AdUnitIds.Rc.SPLASH_INTERSTITIAL,
                                                onFinished = { onContinueState() },
                                            )
                                        } else {
                                            onContinueState()
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Let's Start",
                                color = white,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else if (consentPhaseActive) {
                        CircularProgressIndicator(color = SplashOnBackground)
                    } else {
                        val progressValue =
                            if (hasNetwork) {
                                maxOf(loadingProgress.value, linearProgress)
                            } else {
                                loadingProgress.value
                            }
                        Box(
                            modifier =
                                Modifier
                                    .width(280.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SplashProgressTrack),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressValue)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ProGradientBrush),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Context.hasNetwork(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        AppThemeProvider(isDarkTheme = false) {
            SplashScreenBackground(modifier = Modifier.fillMaxSize()) {
                SplashScreen(onFinished = {})
            }
        }
    }
}
