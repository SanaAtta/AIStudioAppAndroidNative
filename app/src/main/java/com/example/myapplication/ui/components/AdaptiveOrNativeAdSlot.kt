package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ads.AdUnitIds
import com.example.myapplication.ads.AdMobGoogleTestUnits
import com.example.myapplication.ads.AdsLoadLog
import com.example.myapplication.ads.NativeAdTemplate

enum class BannerPresentation {
    Adaptive,
    /** Home bottom — fixed [com.google.android.gms.ads.AdSize.LARGE_BANNER] (320x100). */
    HomeLarge,
}

/**
 * RC-driven ad slot — shows only the format enabled in Remote Config.
 *
 * - Banner RC on → banner only (banner wins when both are on).
 * - Native RC on, banner off → native only. No automatic cross-format fallback.
 */
@Composable
fun AdaptiveOrNativeAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
    bannerSlotLabel: String = "adaptive banner",
    bannerRcParam: String = "",
    nativeFailBannerFallback: Boolean = false,
    bannerPresentation: BannerPresentation = BannerPresentation.Adaptive,
    /** Keep slot height while loading and after load failure (onboarding). */
    reserveSlotSpace: Boolean = false,
    onVisibleChanged: ((Boolean) -> Unit)? = null,
) {
    if (!slotEnabled) {
        LaunchedEffect(Unit) { onVisibleChanged?.invoke(false) }
        return
    }

    val nativeAvailable =
        nativeTemplate != NativeAdTemplate.DISABLED && nativeUnitId.isNotBlank()
    val bannerRcOn = adaptiveBannerEnabled && bannerUnitId.isNotBlank()
    val nativeRcOn = !bannerRcOn && nativeAvailable

    var nativeLoadState by remember(nativeUnitId, nativeTemplate, nativeRcOn) {
        mutableStateOf(
            if (nativeRcOn) NativeAdLoadState.Loading else NativeAdLoadState.Failed,
        )
    }

    val nativeFailed = nativeLoadState == NativeAdLoadState.Failed
    val showBannerFallback =
        nativeFailBannerFallback && nativeRcOn && nativeFailed && bannerUnitId.isNotBlank()
    val showBanner = bannerRcOn || showBannerFallback
    val showNative = nativeRcOn && !nativeFailed && !showBannerFallback

    LaunchedEffect(slotEnabled, showBanner, bannerUnitId) {
        AdsLoadLog.trace(
            bannerSlotLabel,
            "slot compose enabled=$slotEnabled showBanner=$showBanner " +
                "bannerRcOn=$bannerRcOn fallback=$showBannerFallback " +
                "nativeRcOn=$nativeRcOn nativeState=$nativeLoadState unitId=$bannerUnitId",
        )
    }

    if (!showBanner && !showNative) {
        LaunchedEffect(Unit) { onVisibleChanged?.invoke(false) }
        return
    }

    var bannerLoaded by remember(bannerUnitId, showBannerFallback) {
        mutableStateOf(false)
    }
    var bannerFailed by remember(bannerUnitId, showBannerFallback) {
        mutableStateOf(false)
    }
    var bannerHeight by remember(bannerUnitId, bannerPresentation) {
        mutableStateOf(bannerPendingMinHeight(bannerUnitId, bannerPresentation))
    }

    val slotOccupyingSpace =
        when {
            showBanner && !bannerFailed -> true
            showNative && nativeLoadState != NativeAdLoadState.Failed -> true
            else -> false
        }

    LaunchedEffect(slotOccupyingSpace) { onVisibleChanged?.invoke(slotOccupyingSpace) }

    if (showBanner && bannerFailed && !showNative && !reserveSlotSpace) {
        return
    }
    if (!showBanner && !showNative) {
        return
    }

    val formatMinHeight =
        expectedSlotMinHeight(
            showBanner = showBanner,
            showNative = showNative,
            bannerUnitId = bannerUnitId,
            bannerPresentation = bannerPresentation,
            nativeTemplate = nativeTemplate,
        )

    val reservedMinHeight =
        if (reserveSlotSpace && formatMinHeight > 0.dp) {
            formatMinHeight
        } else {
            when {
                showBanner && bannerLoaded && bannerPresentation == BannerPresentation.HomeLarge ->
                    LargeBannerSlotHeight
                showBanner && bannerLoaded -> bannerHeight
                showNative && nativeLoadState == NativeAdLoadState.Loaded ->
                    nativeTemplateMinHeight(nativeTemplate)
                else -> 0.dp
            }
        }

    val bannerStillLoading = showBanner && !bannerLoaded && !bannerFailed
    val bannerPendingHeight =
        if (bannerStillLoading && reserveSlotSpace) {
            bannerPendingMinHeight(bannerUnitId, bannerPresentation)
        } else {
            null
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    when {
                        bannerPendingHeight != null -> Modifier.height(bannerPendingHeight)
                        reservedMinHeight > 0.dp -> Modifier.heightIn(min = reservedMinHeight)
                        else -> Modifier
                    },
                ),
    ) {
        if (showBanner && !bannerFailed) {
            val bannerSlot: @Composable () -> Unit = {
                when (bannerPresentation) {
                    BannerPresentation.HomeLarge ->
                        HomeBottomBannerAd(
                            unitId = bannerUnitId,
                            slotLabel = bannerSlotLabel,
                            rcParam = bannerRcParam.ifBlank { nativeRcParam },
                            modifier = Modifier.fillMaxWidth(),
                            onLoaded = {
                                bannerLoaded = true
                                bannerFailed = false
                            },
                            onFailed = {
                                bannerLoaded = false
                                bannerFailed = true
                            },
                        )
                    BannerPresentation.Adaptive ->
                        AdaptiveBannerEmbeddedAd(
                            unitId = bannerUnitId,
                            slotLabel = bannerSlotLabel,
                            rcParam = bannerRcParam.ifBlank { nativeRcParam },
                            modifier = Modifier.fillMaxWidth(),
                            onLoaded = { height ->
                                bannerHeight =
                                    height.coerceAtLeast(
                                        bannerPendingMinHeight(bannerUnitId, bannerPresentation),
                                    )
                                bannerLoaded = true
                                bannerFailed = false
                            },
                            onFailed = {
                                bannerLoaded = false
                                bannerFailed = true
                            },
                        )
                }
            }
            if (bannerPresentation == BannerPresentation.HomeLarge) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .largeBannerSlotFrame(),
                ) {
                    bannerSlot()
                }
            } else {
                bannerSlot()
            }
        }

        if (showNative && nativeLoadState != NativeAdLoadState.Failed) {
            key(nativeUnitId, nativeTemplate, nativeRcOn) {
                NativeEmbeddedAd(
                    unitId = nativeUnitId,
                    template = nativeTemplate,
                    slotLabel = nativeSlotLabel,
                    rcParam = nativeRcParam,
                    horizontalContentPadding = horizontalContentPadding,
                    modifier = Modifier.fillMaxWidth(),
                    onLoadStateChanged = { nativeLoadState = it },
                )
            }
        }
    }
}

private fun expectedSlotMinHeight(
    showBanner: Boolean,
    showNative: Boolean,
    bannerUnitId: String,
    bannerPresentation: BannerPresentation,
    nativeTemplate: NativeAdTemplate,
): Dp =
    when {
        showBanner -> bannerPendingMinHeight(bannerUnitId, bannerPresentation)
        showNative -> nativeTemplateMinHeight(nativeTemplate)
        else -> 0.dp
    }

private fun nativeTemplateMinHeight(nativeTemplate: NativeAdTemplate): Dp =
    when (nativeTemplate) {
        NativeAdTemplate.WITH_MEDIA -> NativeAdSlotHeights.WithMedia
        NativeAdTemplate.WITHOUT_MEDIA -> NativeAdSlotHeights.WithoutMedia
        NativeAdTemplate.DISABLED -> 0.dp
    }

private fun bannerPendingMinHeight(
    bannerUnitId: String,
    bannerPresentation: BannerPresentation,
): Dp =
    when (bannerPresentation) {
        BannerPresentation.HomeLarge -> LargeBannerSlotHeight
        BannerPresentation.Adaptive ->
            if (AdMobGoogleTestUnits.isMediumRectangleUnit(bannerUnitId)) {
                MediumRectangleSlotHeight
            } else {
                AdaptiveBannerSlotMinHeight
            }
    }

/** Splash top / bottom — banner or single-flight cached native (one AdMob request per splash). */
@Composable
fun SplashScreenAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 5.dp,
    bannerSlotLabel: String = "splash large banner 320x100",
    bannerRcParam: String = AdUnitIds.Rc.SPLASH_ADAPTIVE_BANNER,
) {
    if (!slotEnabled) return

    val bannerRcOn = adaptiveBannerEnabled && bannerUnitId.isNotBlank()
    val nativeRcOn =
        !bannerRcOn &&
            nativeTemplate != NativeAdTemplate.DISABLED &&
            nativeUnitId.isNotBlank()

    if (nativeRcOn) {
        SplashNativeEmbeddedAdSlot(
            unitId = nativeUnitId,
            template = nativeTemplate,
            slotLabel = nativeSlotLabel,
            rcParam = nativeRcParam,
            modifier = modifier,
            horizontalContentPadding = horizontalContentPadding,
        )
    } else {
        AdaptiveOrNativeAdSlot(
            slotEnabled = true,
            adaptiveBannerEnabled = adaptiveBannerEnabled,
            bannerUnitId = bannerUnitId,
            nativeTemplate = nativeTemplate,
            nativeUnitId = nativeUnitId,
            nativeSlotLabel = nativeSlotLabel,
            nativeRcParam = nativeRcParam,
            modifier = modifier,
            horizontalContentPadding = horizontalContentPadding,
            bannerSlotLabel = bannerSlotLabel,
            bannerRcParam = bannerRcParam,
            nativeFailBannerFallback = false,
            bannerPresentation = BannerPresentation.HomeLarge,
        )
    }
}

/** Onboarding top / bottom — same large banner as Home; collapses when ad does not load. */
@Composable
fun OnboardingScreenAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
    bannerSlotLabel: String = "onboarding large banner 320x100",
    bannerRcParam: String = AdUnitIds.Rc.ONBOARDING_ADAPTIVE_BANNER,
    onVisibleChanged: ((Boolean) -> Unit)? = null,
) {
    AdaptiveOrNativeAdSlot(
        slotEnabled = slotEnabled,
        adaptiveBannerEnabled = adaptiveBannerEnabled,
        bannerUnitId = bannerUnitId,
        nativeTemplate = nativeTemplate,
        nativeUnitId = nativeUnitId,
        nativeSlotLabel = nativeSlotLabel,
        nativeRcParam = nativeRcParam,
        modifier = modifier,
        horizontalContentPadding = horizontalContentPadding,
        bannerSlotLabel = bannerSlotLabel,
        bannerRcParam = bannerRcParam,
        nativeFailBannerFallback = false,
        bannerPresentation = BannerPresentation.HomeLarge,
        reserveSlotSpace = false,
        onVisibleChanged = onVisibleChanged,
    )
}

/** Reels feed bottom — RC adaptive banner or native (strict format; no cross-fallback). */
@Composable
fun ReelsScreenBottomAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
    bannerSlotLabel: String = "reels adaptive banner",
    bannerRcParam: String = AdUnitIds.Rc.REELS_ADAPTIVE_BANNER,
    onVisibleChanged: ((Boolean) -> Unit)? = null,
) {
    AdaptiveOrNativeAdSlot(
        slotEnabled = slotEnabled,
        adaptiveBannerEnabled = adaptiveBannerEnabled,
        bannerUnitId = bannerUnitId,
        nativeTemplate = nativeTemplate,
        nativeUnitId = nativeUnitId,
        nativeSlotLabel = nativeSlotLabel,
        nativeRcParam = nativeRcParam,
        modifier = modifier,
        horizontalContentPadding = horizontalContentPadding,
        bannerSlotLabel = bannerSlotLabel,
        bannerRcParam = bannerRcParam,
        nativeFailBannerFallback = false,
        bannerPresentation = BannerPresentation.Adaptive,
        onVisibleChanged = onVisibleChanged,
    )
}

/** Home top / bottom — strict RC format (no cross-format fallback on load failure). */
@Composable
fun HomeScreenAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
    bannerSlotLabel: String = "home adaptive banner",
    bannerRcParam: String = AdUnitIds.Rc.HOME_ADAPTIVE_BANNER,
    useLargeBanner: Boolean = false,
    onVisibleChanged: ((Boolean) -> Unit)? = null,
) {
    AdaptiveOrNativeAdSlot(
        slotEnabled = slotEnabled,
        adaptiveBannerEnabled = adaptiveBannerEnabled,
        bannerUnitId = bannerUnitId,
        nativeTemplate = nativeTemplate,
        nativeUnitId = nativeUnitId,
        nativeSlotLabel = nativeSlotLabel,
        nativeRcParam = nativeRcParam,
        modifier = modifier,
        horizontalContentPadding = horizontalContentPadding,
        bannerSlotLabel = bannerSlotLabel,
        bannerRcParam = bannerRcParam,
        nativeFailBannerFallback = false,
        bannerPresentation =
            if (useLargeBanner) {
                BannerPresentation.HomeLarge
            } else {
                BannerPresentation.Adaptive
            },
        onVisibleChanged = onVisibleChanged,
    )
}

/** @deprecated Use [HomeScreenAdSlot]. */
@Composable
fun HomeBottomAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
) {
    HomeScreenAdSlot(
        slotEnabled = slotEnabled,
        adaptiveBannerEnabled = adaptiveBannerEnabled,
        bannerUnitId = bannerUnitId,
        nativeTemplate = nativeTemplate,
        nativeUnitId = nativeUnitId,
        nativeSlotLabel = nativeSlotLabel,
        nativeRcParam = nativeRcParam,
        modifier = modifier,
        horizontalContentPadding = horizontalContentPadding,
        bannerSlotLabel = "home adaptive bottom banner",
        useLargeBanner = true,
    )
}

/** Language screen — same large banner (320×100) as Home. */
@Composable
fun LanguageScreenAdSlot(
    slotEnabled: Boolean,
    adaptiveBannerEnabled: Boolean,
    bannerUnitId: String,
    nativeTemplate: NativeAdTemplate,
    nativeUnitId: String,
    nativeSlotLabel: String,
    nativeRcParam: String,
    modifier: Modifier = Modifier,
    horizontalContentPadding: Dp = 0.dp,
    onVisibleChanged: ((Boolean) -> Unit)? = null,
    bannerSlotLabel: String = "language large banner 320x100",
    bannerRcParam: String = AdUnitIds.Rc.LANGUAGE_ADAPTIVE_BANNER,
) {
    AdaptiveOrNativeAdSlot(
        slotEnabled = slotEnabled,
        adaptiveBannerEnabled = adaptiveBannerEnabled,
        bannerUnitId = bannerUnitId,
        nativeTemplate = nativeTemplate,
        nativeUnitId = nativeUnitId,
        nativeSlotLabel = nativeSlotLabel,
        nativeRcParam = nativeRcParam,
        modifier = modifier,
        horizontalContentPadding = horizontalContentPadding,
        bannerSlotLabel = bannerSlotLabel,
        bannerRcParam = bannerRcParam,
        nativeFailBannerFallback = false,
        bannerPresentation = BannerPresentation.HomeLarge,
        onVisibleChanged = onVisibleChanged,
    )
}
