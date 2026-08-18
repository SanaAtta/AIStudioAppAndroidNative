package com.example.myapplication.setup

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.BuildConfig
import com.example.myapplication.ads.AdUnitIds
import com.example.myapplication.ads.AdsConfigRevision
import com.example.myapplication.ads.AdsConsentManager
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsLoadLog
import com.example.myapplication.ads.AdsRemoteConfig
import com.example.myapplication.ads.AppOpenAdManager
import com.example.myapplication.ads.MobileAdsInitializer
import com.example.myapplication.ads.SplashInterstitial
import com.example.myapplication.ads.SplashNativeAdManager
import com.example.myapplication.billing.PremiumAccess
import com.example.myapplication.ui.components.SplashScreenAdSlot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private const val SplashTotalDurationMs = 20_000L
private const val SplashConsentBudgetMs = 10_000L
private const val SplashRcTimeoutMs = 5_000L
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
        "$stage | networkReady=ok " +
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

private val SplashBg = Color(0xFF0B1220)
private val SplashAccent = Color(0xFF2DD4BF)

@Composable
fun SetupSplashScreen(onContinue: () -> Unit) {
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
    val onContinueState by rememberUpdatedState(onContinue)

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
            splashAdsLog("consent phase begin activity=${act != null}")

            if (act != null) {
                runCatching {
                    withTimeoutOrNull(SplashConsentBudgetMs) {
                        AdsConsentManager.gatherConsent(act)
                    }.also { timedOut ->
                        if (timedOut == null) {
                            splashAdsLog("consent TIMEOUT after ${SplashConsentBudgetMs}ms")
                        } else {
                            splashAdsLog("consent finished canRequestAds=${AdsConsentManager.canRequestAds()}")
                        }
                    }
                }.onFailure {
                    splashAdsLog("consent ERROR ${it.message}")
                }
            } else {
                splashAdsLog("consent SKIPPED — context is not Activity")
            }
            consentPhaseActive = false

            val progressMs =
                (SplashTotalDurationMs - (SystemClock.elapsedRealtime() - flowStartMs))
                    .coerceAtLeast(1_000L)
                    .toInt()
            splashProgressDurationMs = progressMs
            animateProgress = true

            MobileAdsInitializer.start(appContext)
            splashAdsLog("MobileAdsInitializer.start() called")
            AdsRemoteConfig.ensureLocalDefaultsApplied()
            logSplashAdSnapshot("after_local_defaults")

            coroutineScope {
                val progressMsLong = progressMs.toLong()
                val progressComplete = async { delay(progressMsLong) }

                val rcResult =
                    runCatching {
                        withTimeoutOrNull(SplashRcTimeoutMs) {
                            AdsRemoteConfig.refreshAdsControlForSplash()
                        }
                    }
                splashAdsLog(
                    "RC refresh done success=${rcResult.isSuccess} " +
                        "timedOut=${rcResult.getOrNull() == null && rcResult.isSuccess} " +
                        "error=${rcResult.exceptionOrNull()?.message}",
                )
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
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SplashBg, SplashBg.copy(alpha = 0.98f), SplashAccent.copy(alpha = 0.12f)),
                    ),
                ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showSplashTopSlot) {
                SplashScreenAdSlot(
                    slotEnabled = true,
                    adaptiveBannerEnabled = splashTopAd.adaptiveBanner,
                    bannerUnitId = splashBannerUnitId,
                    nativeTemplate = splashTopAd.nativeTemplate,
                    nativeUnitId = splashNativeUnitId,
                    nativeSlotLabel = "splash native top ad",
                    nativeRcParam = AdUnitIds.Rc.SPLASH_NATIVE,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Box(Modifier.weight(1f))
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("AI App", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Create with AI", color = Color(0xFF8B9BB4), fontSize = 15.sp)
                Spacer(modifier = Modifier.height(28.dp))
                if (consentPhaseActive) {
                    CircularProgressIndicator(color = SplashAccent)
                } else {
                    LinearProgressIndicator(
                        progress = linearProgress,
                        modifier =
                            Modifier
                                .width(180.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        color = SplashAccent,
                        trackColor = Color.White.copy(alpha = 0.15f),
                    )
                }
                if (showStartButton) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
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
                    ) {
                        Text("Let's Start")
                    }
                }
            }
            Box(modifier = Modifier.weight(1f))
            if (showSplashBottomSlot) {
                SplashScreenAdSlot(
                    slotEnabled = true,
                    adaptiveBannerEnabled = splashBottomAd.adaptiveBanner,
                    bannerUnitId = splashBannerUnitId,
                    nativeTemplate = splashBottomAd.nativeTemplate,
                    nativeUnitId = splashNativeUnitId,
                    nativeSlotLabel = "splash native bottom ad",
                    nativeRcParam = AdUnitIds.Rc.SPLASH_NATIVE,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
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
