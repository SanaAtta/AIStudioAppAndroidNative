package com.aiartgenerator.imagegenerator.videogenerator.view.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.OnboardingScreenAdSlot
import com.aiartgenerator.imagegenerator.videogenerator.controller.OnboardingController
import com.aiartgenerator.imagegenerator.videogenerator.model.OnboardingPage
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.actionGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch

private val OnboardingImageCorner = 28.dp

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { OnboardingController() }
    val pagerState = rememberPagerState(pageCount = { controller.pages.size })
    val scope = rememberCoroutineScope()
    val currentPage = controller.pages[pagerState.currentPage]
    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val topAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.onboardingTopAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val bottomAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.onboardingBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val showTop = adsReady && AdsControl.onboardingShowTopAdSlot()
    val showBottom = adsReady && AdsControl.onboardingShowBottomAdSlot()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.onboardingNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.onboardingAdaptiveBanner() }

    ResponsiveScreenRoot(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) { metrics ->
        val compact = metrics.isCompactHeight || showBottom || showTop
        val topSpacer = if (compact) 8.dp else 24.dp
        val sectionSpacer = if (compact) 10.dp else 28.dp
        val titleSize = metrics.scaledTitleSp(if (compact) 28f else 32f)
        val bodySize = metrics.scaledSp(if (compact) 13f else 15f)
        val buttonHeight = if (compact) (metrics.buttonHeight - 4.dp) else metrics.buttonHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val isLastPage = controller.isLastPage(pagerState.currentPage)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (!isLastPage) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = white,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            if (showTop) {
                OnboardingScreenAdSlot(
                    slotEnabled = true,
                    adaptiveBannerEnabled = topAd.adaptiveBanner,
                    bannerUnitId = bannerUnit,
                    nativeTemplate = topAd.nativeTemplate,
                    nativeUnitId = nativeUnit,
                    nativeSlotLabel = "onboarding native top ad",
                    nativeRcParam = AdUnitIds.Rc.ONBOARDING_NATIVE,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(topSpacer))
            }

            // fillMaxSize inside weight — avoid aspectRatio overflow that clips the image.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    beyondViewportPageCount = 1,
                ) { pageIndex ->
                    OnboardingImageCard(page = controller.pages[pageIndex])
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacer))

            Text(
                text = stringResource(currentPage.titleRes),
                color = SplashOnBackground,
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = titleSize,
            )
            Spacer(modifier = Modifier.height(if (compact) 6.dp else 12.dp))
            Text(
                text = stringResource(currentPage.descriptionRes),
                color = white,
                fontSize = bodySize,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = bodySize * 1.35f,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(sectionSpacer))

            OnboardingPageIndicator(
                pageCount = controller.pages.size,
                currentPage = pagerState.currentPage,
            )

            Spacer(modifier = Modifier.height(sectionSpacer))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
                    .actionGradientBackground(RoundedCornerShape(10.dp))
                    .clickable {
                        if (isLastPage) {
                            onFinished()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(currentPage.buttonRes),
                    color = white,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (showBottom) {
                OnboardingScreenAdSlot(
                    slotEnabled = true,
                    adaptiveBannerEnabled = bottomAd.adaptiveBanner,
                    bannerUnitId = bannerUnit,
                    nativeTemplate = bottomAd.nativeTemplate,
                    nativeUnitId = nativeUnit,
                    nativeSlotLabel = "onboarding native bottom ad",
                    nativeRcParam = AdUnitIds.Rc.ONBOARDING_NATIVE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OnboardingImageCard(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = stringResource(page.titleRes),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(OnboardingImageCorner)),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun OnboardingPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .then(
                        if (isSelected) {
                            Modifier.width(24.dp)
                        } else {
                            Modifier.size(8.dp)
                        },
                    )
                    .clip(
                        if (isSelected) {
                            RoundedCornerShape(4.dp)
                        } else {
                            CircleShape
                        },
                    )
                    .background(
                        if (isSelected) {
                            ProGradientStart
                        } else {
                            SplashOnBackground.copy(alpha = 0.25f)
                        },
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        OnboardingScreen(onFinished = {})
    }
}
