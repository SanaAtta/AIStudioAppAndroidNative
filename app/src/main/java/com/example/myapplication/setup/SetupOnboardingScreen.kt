package com.example.myapplication.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ads.AdUnitIds
import com.example.myapplication.ads.AdsConfigRevision
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsRemoteConfig
import com.example.myapplication.ui.components.OnboardingScreenAdSlot
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val body: String)

/** Pages 1–5 only — no last-screen native ad / exit interstitial. */
private val pages =
    listOf(
        OnboardingPage("AI Images", "Turn prompts into stunning images in seconds."),
        OnboardingPage("AI Video", "Create short videos from your ideas."),
        OnboardingPage("AI Music", "Generate music tracks from text prompts."),
        OnboardingPage("AI Chat", "Ask anything and get instant answers."),
        OnboardingPage("Wallpapers", "Browse and apply AI wallpapers."),
    )

@Composable
fun SetupOnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()

    val topAd =
        remember(adsConfigRev, adsReady) {
            if (adsReady) AdsControl.onboardingTopAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
        }
    val bottomAd =
        remember(adsConfigRev, adsReady) {
            if (adsReady) AdsControl.onboardingBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
        }
    val showTop = adsReady && AdsControl.onboardingShowTopAdSlot()
    val showBottom = adsReady && AdsControl.onboardingShowBottomAdSlot()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.onboardingNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.onboardingAdaptiveBanner() }

    fun complete() {
        SetupPrefs.markOnboardingDone(context)
        onFinished()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220))
                .padding(16.dp),
    ) {
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
            Spacer(modifier = Modifier.height(12.dp))
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    item.body,
                    color = Color(0xFF8B9BB4),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { complete() }) { Text("Skip", color = Color.White) }
            Button(
                onClick = {
                    if (pagerState.currentPage >= pages.lastIndex) {
                        complete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            ) {
                Text(if (pagerState.currentPage >= pages.lastIndex) "Continue" else "Next")
            }
        }
        if (showBottom) {
            Spacer(modifier = Modifier.height(12.dp))
            OnboardingScreenAdSlot(
                slotEnabled = true,
                adaptiveBannerEnabled = bottomAd.adaptiveBanner,
                bannerUnitId = bannerUnit,
                nativeTemplate = bottomAd.nativeTemplate,
                nativeUnitId = nativeUnit,
                nativeSlotLabel = "onboarding native bottom ad",
                nativeRcParam = AdUnitIds.Rc.ONBOARDING_NATIVE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
