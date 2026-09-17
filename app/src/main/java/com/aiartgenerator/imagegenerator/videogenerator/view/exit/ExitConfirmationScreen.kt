package com.aiartgenerator.imagegenerator.videogenerator.view.exit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.NativeAdTemplate
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.AdaptiveBannerEmbeddedAd
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.MediumRectangleEmbeddedAd
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.MediumRectangleSlotHeight
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.NativeEmbeddedAd
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppGradients
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

@Composable
fun ExitConfirmationScreen(
    onStay: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onStay)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppGradients.Screen)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.exit_title),
                color = SplashOnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.exit_subtitle),
                color = HomeMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))
            ExitScreenAdSlot(
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExitOutlinedButton(
                    label = stringResource(R.string.exit_stay),
                    onClick = onStay,
                    modifier = Modifier.weight(1f),
                )
                ExitPrimaryButton(
                    label = stringResource(R.string.exit_confirm),
                    onClick = onExit,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExitScreenAdSlot(modifier: Modifier = Modifier) {
    val adsConfigRev by AdsConfigRevision.state
    val showMrec = remember(adsConfigRev) { AdsControl.exitScreenMediumRectangleBannerEnabled() }
    val showNative = remember(adsConfigRev) { AdsControl.exitScreenMediumNativeEnabled() }
    val showBanner = remember(adsConfigRev) { AdsControl.exitScreenAdaptiveBannerEnabled() }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            showMrec -> {
                MediumRectangleEmbeddedAd(
                    unitId = AdUnitIds.exitMediumRectangle(),
                    slotLabel = "exit medium rectangle",
                    rcParam = AdUnitIds.rcKeyExitMediumRectangle(),
                    modifier = Modifier
                        .width(300.dp)
                        .height(MediumRectangleSlotHeight),
                )
            }
            showNative -> {
                NativeEmbeddedAd(
                    unitId = AdUnitIds.exitNative(),
                    template = NativeAdTemplate.WITH_MEDIA,
                    slotLabel = "exit medium native",
                    rcParam = AdUnitIds.Rc.EXIT_NATIVE,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            showBanner -> {
                AdaptiveBannerEmbeddedAd(
                    unitId = AdUnitIds.exitAdaptiveBanner(),
                    slotLabel = "exit adaptive banner",
                    rcParam = AdUnitIds.Rc.EXIT_ADAPTIVE_BANNER,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ExitPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .brandGradientBackground(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = white,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ExitOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, ProGradientStart.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = SplashOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
