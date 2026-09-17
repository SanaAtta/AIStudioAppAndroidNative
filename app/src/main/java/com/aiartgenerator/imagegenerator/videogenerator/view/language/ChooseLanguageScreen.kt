package com.aiartgenerator.imagegenerator.videogenerator.view.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ui.components.LanguageScreenAdSlot
import com.aiartgenerator.imagegenerator.videogenerator.controller.LanguageController
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguageOption
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.LanguageCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.LanguageCardBorderUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

private val LanguageCardShape = RoundedCornerShape(16.dp)

@Composable
fun ChooseLanguageScreen(
    initialSelectedId: String,
    onDone: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val controller = remember(initialSelectedId) {
        LanguageController(initialSelectedId = initialSelectedId)
    }
    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val topAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.languageTopAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val bottomAd = remember(adsConfigRev, adsReady) {
        if (adsReady) AdsControl.languageBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
    }
    val showTop = adsReady && AdsControl.languageShowTopAdSlot()
    val showBottom = adsReady && AdsControl.languageShowBottomAdSlot()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.languageNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.languageAdaptiveBanner() }

    ResponsiveScreenRoot(
        modifier = modifier.navigationBarsPadding(),
    ) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTop) {
                LanguageScreenAdSlot(
                    slotEnabled = true,
                    adaptiveBannerEnabled = topAd.adaptiveBanner,
                    bannerUnitId = bannerUnit,
                    nativeTemplate = topAd.nativeTemplate,
                    nativeUnitId = nativeUnit,
                    nativeSlotLabel = "language native top ad",
                    nativeRcParam = AdUnitIds.Rc.LANGUAGE_NATIVE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(top = 8.dp),
                )
            }
            if (onBack != null) {
                AppTopBar(
                    title = stringResource(R.string.choose_language),
                    onBack = onBack,
                    showProAndSettings = false,
                    trailingContent = {
                        LanguageDoneButton(
                            onClick = { onDone(controller.selectedLanguageId) },
                        )
                    },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.choose_language),
                        color = SplashOnBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    LanguageDoneButton(
                        onClick = { onDone(controller.selectedLanguageId) },
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = metrics.horizontalPadding)
                    .then(
                        if (onBack != null) {
                            Modifier.padding(top = 16.dp)
                        } else {
                            Modifier
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(
                    items = controller.languages,
                    key = { it.id },
                ) { language ->
                    LanguageRow(
                        language = language,
                        selected = language.id == controller.selectedLanguageId,
                        onClick = { controller.selectLanguage(language.id) },
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
                    nativeSlotLabel = "language native bottom ad",
                    nativeRcParam = AdUnitIds.Rc.LANGUAGE_NATIVE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LanguageDoneButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .brandGradientBackground(RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.language_done),
            color = white,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LanguageRow(
    language: LanguageOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedAccent = Color(0xFF5061E9)
    val borderColor = if (selected) selectedAccent else LanguageCardBorderUnselected
    val textColor = if (selected) selectedAccent else SplashOnBackground

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LanguageCardShape)
            .background(LanguageCardBackground)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = LanguageCardShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = language.flagEmoji,
            fontSize = 26.sp,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = language.name,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        LanguageRadioIndicator(selected = selected)
    }
}

@Composable
private fun LanguageRadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedColor = Color(0xFF5061E9)
    val unselectedColor = LanguageCardBorderUnselected
    Box(
        modifier = modifier
            .size(22.dp)
            .border(
                width = 2.dp,
                color = if (selected) selectedColor else unselectedColor,
                shape = CircleShape,
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectedColor, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChooseLanguageScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        ChooseLanguageScreen(
            initialSelectedId = "en",
            onDone = {},
        )
    }
}
