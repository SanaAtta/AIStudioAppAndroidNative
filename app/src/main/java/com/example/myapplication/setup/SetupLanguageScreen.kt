package com.example.myapplication.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ads.AdUnitIds
import com.example.myapplication.ads.AdsConfigRevision
import com.example.myapplication.ads.AdsControl
import com.example.myapplication.ads.AdsRemoteConfig
import com.example.myapplication.ui.components.LanguageScreenAdSlot

private data class LangOption(val tag: String, val label: String)

private val languages =
    listOf(
        LangOption("en", "English"),
        LangOption("ur", "Urdu"),
        LangOption("ar", "Arabic"),
        LangOption("hi", "Hindi"),
        LangOption("tr", "Turkish"),
    )

@Composable
fun SetupLanguageScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    var selected by remember { mutableStateOf("en") }

    val topAd =
        remember(adsConfigRev, adsReady) {
            if (adsReady) AdsControl.languageTopAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
        }
    val bottomAd =
        remember(adsConfigRev, adsReady) {
            if (adsReady) AdsControl.languageBottomAdSlotFormat() else AdsControl.hiddenAdSlotFormat()
        }
    val showTop = adsReady && AdsControl.languageShowTopAdSlot()
    val showBottom = adsReady && AdsControl.languageShowBottomAdSlot()
    val nativeUnit = remember(adsConfigRev) { AdUnitIds.languageNative() }
    val bannerUnit = remember(adsConfigRev) { AdUnitIds.languageAdaptiveBanner() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220))
                .padding(16.dp),
    ) {
        if (showTop) {
            LanguageScreenAdSlot(
                slotEnabled = true,
                adaptiveBannerEnabled = topAd.adaptiveBanner,
                bannerUnitId = bannerUnit,
                nativeTemplate = topAd.nativeTemplate,
                nativeUnitId = nativeUnit,
                nativeSlotLabel = "language native top ad",
                nativeRcParam = AdUnitIds.Rc.LANGUAGE_NATIVE,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            "Choose Language",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(languages) { lang ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { selected = lang.tag }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = selected == lang.tag, onClick = { selected = lang.tag })
                    Text(lang.label, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Button(
            onClick = {
                SetupPrefs.markLanguageDone(context, selected)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
        if (showBottom) {
            Spacer(modifier = Modifier.height(12.dp))
            LanguageScreenAdSlot(
                slotEnabled = true,
                adaptiveBannerEnabled = bottomAd.adaptiveBanner,
                bannerUnitId = bannerUnit,
                nativeTemplate = bottomAd.nativeTemplate,
                nativeUnitId = nativeUnit,
                nativeSlotLabel = "language native bottom ad",
                nativeRcParam = AdUnitIds.Rc.LANGUAGE_NATIVE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
