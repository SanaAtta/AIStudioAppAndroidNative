package com.aiartgenerator.imagegenerator.videogenerator.view.common

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ads.FeatureInterstitial
import com.aiartgenerator.imagegenerator.videogenerator.ads.GenerateProgressRewardedAds
import com.aiartgenerator.imagegenerator.videogenerator.model.GenerationSession
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.LanguageScreenAdSlot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashProgressTrack
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val ProgressStroke = 14.dp
@Composable
fun GeneratingScreen(
    onSuccessNavigate: (GenerationSession.Kind) -> Unit,
    onErrorBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val kind = GenerationSession.kind
    val phase = GenerationSession.phase
    val progress = GenerationSession.progress
    var fullscreenAdStarted by remember { mutableStateOf(false) }
    var errorHandled by remember { mutableStateOf(false) }

    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val topAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.generatingTopAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val bottomAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.generatingBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val showTop = adsReady && AdsControl.generatingShowTopAdSlot()
    val showBottom = adsReady && AdsControl.generatingShowBottomAdSlot()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.generatingNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.generatingAdaptiveBanner() }

    LaunchedEffect(Unit) {
        when (kind) {
            GenerationSession.Kind.Image -> {
                // Image progress: rewarded only (never regular interstitial).
                if (!AdsControl.generateProgressRewardedEnabled()) return@LaunchedEffect
                GenerateProgressRewardedAds.preloadRewarded(
                    context = context,
                    unitId = AdUnitIds.generatingRewarded(),
                    slotLabel = "image generate progress",
                    rcParam = AdUnitIds.Rc.GENERATING_REWARDED,
                )
            }
            GenerationSession.Kind.Video,
            GenerationSession.Kind.Music,
            -> {
                val enabled =
                    when (kind) {
                        GenerationSession.Kind.Video -> AdsControl.videoGeneratorInterstitialEnabled()
                        GenerationSession.Kind.Music -> AdsControl.musicGeneratorInterstitialEnabled()
                        else -> false
                    }
                if (!enabled) return@LaunchedEffect
                FeatureInterstitial.preload(
                    context = context,
                    unitId = AdUnitIds.genericInterstitial(),
                    slotLabel = "${kind.name.lowercase()} post-generate interstitial",
                    rcParam = AdUnitIds.Rc.INTERSTITIAL,
                )
            }
            GenerationSession.Kind.Cover -> Unit
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "genProgress",
    )
    val percent = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)

    val statusText =
        when (kind) {
            GenerationSession.Kind.Image -> stringResource(R.string.generating_image_status)
            GenerationSession.Kind.Video -> stringResource(R.string.generating_video_status)
            GenerationSession.Kind.Music -> stringResource(R.string.generating_music_status)
            GenerationSession.Kind.Cover -> stringResource(R.string.generating_cover_status)
        }

    // Ads only after generation succeeds AND the UI has reached 100%.
    LaunchedEffect(phase, animatedProgress) {
        when (phase) {
            GenerationSession.Phase.Success -> {
                if (fullscreenAdStarted) return@LaunchedEffect
                if (animatedProgress < 0.995f) return@LaunchedEffect
                // Brief hold at 100% so the user sees completion before the ad.
                delay(280)
                if (fullscreenAdStarted) return@LaunchedEffect
                if (GenerationSession.phase != GenerationSession.Phase.Success) return@LaunchedEffect
                fullscreenAdStarted = true
                val k = kind
                fun continueToResult() {
                    GenerationSession.reset()
                    onSuccessNavigate(k)
                }
                when (kind) {
                    GenerationSession.Kind.Image -> {
                        GenerateProgressRewardedAds.showThen(
                            activity = activity,
                            rewardedEnabled = AdsControl.generateProgressRewardedEnabled(),
                            rewardedUnitId = AdUnitIds.generatingRewarded(),
                            rewardedRcParam = AdUnitIds.Rc.GENERATING_REWARDED,
                            rewardedInterstitialEnabled =
                                AdsControl.generateProgressRewardedInterstitialEnabled(),
                            rewardedInterstitialUnitId = AdUnitIds.generatingRewardedInterstitial(),
                            rewardedInterstitialRcParam =
                                AdUnitIds.Rc.GENERATING_REWARDED_INTERSTITIAL,
                            slotLabel = "image generate progress",
                            onContinue = { continueToResult() },
                        )
                    }
                    GenerationSession.Kind.Video,
                    GenerationSession.Kind.Music,
                    -> {
                        val enabled =
                            when (kind) {
                                GenerationSession.Kind.Video ->
                                    AdsControl.videoGeneratorInterstitialEnabled()
                                GenerationSession.Kind.Music ->
                                    AdsControl.musicGeneratorInterstitialEnabled()
                                else -> false
                            }
                        FeatureInterstitial.showThen(
                            activity = activity,
                            enabled = enabled,
                            unitId = AdUnitIds.genericInterstitial(),
                            slotLabel = "${kind.name.lowercase()} post-generate interstitial",
                            rcParam = AdUnitIds.Rc.INTERSTITIAL,
                            withLoadingDialog = false,
                            onContinue = { continueToResult() },
                        )
                    }
                    GenerationSession.Kind.Cover -> continueToResult()
                }
            }
            GenerationSession.Phase.Error -> {
                if (errorHandled) return@LaunchedEffect
                errorHandled = true
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.generation_error_toast),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                GenerationSession.reset()
                onErrorBack()
            }
            else -> Unit
        }
    }

    val pulse = rememberInfiniteTransition(label = "genPulse")
    val glowScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glowScale",
    )
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glowAlpha",
    )
    val dotsPhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "dots",
    )
    val dots =
        when (dotsPhase.toInt().coerceIn(0, 2)) {
            0 -> "."
            1 -> ".."
            else -> "..."
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(HomeBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        if (showTop) {
            LanguageScreenAdSlot(
                slotEnabled = true,
                adaptiveBannerEnabled = topAd.adaptiveBanner,
                bannerUnitId = bannerUnit,
                nativeTemplate = topAd.nativeTemplate,
                nativeUnitId = nativeUnit,
                nativeSlotLabel = "generating native top ad",
                nativeRcParam = AdUnitIds.Rc.GENERATING_NATIVE,
                bannerSlotLabel = "generating large banner 320x100 top",
                bannerRcParam = AdUnitIds.Rc.GENERATING_ADAPTIVE_BANNER,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        ProGradientStart.copy(alpha = 0.12f),
                                        Color.Transparent,
                                    ),
                            ),
                        ),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier
                                .size(168.dp)
                                .scale(glowScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                ProGradientStart.copy(alpha = glowAlpha),
                                                Color.Transparent,
                                            ),
                                    ),
                                ),
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(132.dp),
                        color = ProGradientStart,
                        trackColor = SplashProgressTrack,
                        strokeWidth = ProgressStroke,
                        strokeCap = StrokeCap.Round,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent",
                            color = HomeOnBackground,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "%",
                            color = ProGradientStart,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = statusText + dots,
                    color = HomeOnBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.generating_please_wait),
                    color = HomeMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        if (showBottom) {
            LanguageScreenAdSlot(
                slotEnabled = true,
                adaptiveBannerEnabled = bottomAd.adaptiveBanner,
                bannerUnitId = bannerUnit,
                nativeTemplate = bottomAd.nativeTemplate,
                nativeUnitId = nativeUnit,
                nativeSlotLabel = "generating native bottom ad",
                nativeRcParam = AdUnitIds.Rc.GENERATING_NATIVE,
                bannerSlotLabel = "generating large banner 320x100 bottom",
                bannerRcParam = AdUnitIds.Rc.GENERATING_ADAPTIVE_BANNER,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
            )
        }
    }
}
