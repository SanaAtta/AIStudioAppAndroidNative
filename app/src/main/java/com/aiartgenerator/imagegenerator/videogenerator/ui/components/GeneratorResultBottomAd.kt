package com.aiartgenerator.imagegenerator.videogenerator.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdUnitIds
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsRemoteConfig
import com.aiartgenerator.imagegenerator.videogenerator.ads.rememberAdsAllowed

enum class GeneratorResultAdKind { Image, Video, Music }

/** Bottom native/banner slot for image / video / music result screens. */
@Composable
fun GeneratorResultBottomAd(
    kind: GeneratorResultAdKind,
    modifier: Modifier = Modifier,
) {
    if (!rememberAdsAllowed()) return

    val adsConfigRev by AdsConfigRevision.state
    val adsReady = AdsRemoteConfig.isAdsConfigReady()
    val bottomAd =
        remember(adsConfigRev, adsReady, kind) {
            if (!adsReady) {
                AdsControl.hiddenAdSlotFormat()
            } else {
                when (kind) {
                    GeneratorResultAdKind.Image -> AdsControl.imageGeneratorResultBottomAdSlotFormat()
                    GeneratorResultAdKind.Video -> AdsControl.videoGeneratorResultBottomAdSlotFormat()
                    GeneratorResultAdKind.Music -> AdsControl.musicGeneratorResultBottomAdSlotFormat()
                }
            }
        }
    val show =
        adsReady &&
            when (kind) {
                GeneratorResultAdKind.Image -> AdsControl.imageGeneratorShowResultBottomAd()
                GeneratorResultAdKind.Video -> AdsControl.videoGeneratorShowResultBottomAd()
                GeneratorResultAdKind.Music -> AdsControl.musicGeneratorShowResultBottomAd()
            }
    if (!show) return

    val nativeUnit =
        remember(adsConfigRev, kind) {
            when (kind) {
                GeneratorResultAdKind.Image -> AdUnitIds.imageResultNative()
                GeneratorResultAdKind.Video -> AdUnitIds.videoResultNative()
                GeneratorResultAdKind.Music -> AdUnitIds.musicResultNative()
            }
        }
    val bannerUnit =
        remember(adsConfigRev, kind) {
            when (kind) {
                GeneratorResultAdKind.Image -> AdUnitIds.imageResultAdaptiveBanner()
                GeneratorResultAdKind.Video -> AdUnitIds.videoResultAdaptiveBanner()
                GeneratorResultAdKind.Music -> AdUnitIds.musicResultAdaptiveBanner()
            }
        }
    val (slotLabel, rcParam) =
        when (kind) {
            GeneratorResultAdKind.Image ->
                "image result native bottom ad" to AdUnitIds.Rc.IMAGE_RESULT_NATIVE
            GeneratorResultAdKind.Video ->
                "video result native bottom ad" to AdUnitIds.Rc.VIDEO_RESULT_NATIVE
            GeneratorResultAdKind.Music ->
                "music result native bottom ad" to AdUnitIds.Rc.MUSIC_RESULT_NATIVE
        }

    LanguageScreenAdSlot(
        slotEnabled = true,
        adaptiveBannerEnabled = bottomAd.adaptiveBanner,
        bannerUnitId = bannerUnit,
        nativeTemplate = bottomAd.nativeTemplate,
        nativeUnitId = nativeUnit,
        nativeSlotLabel = slotLabel,
        nativeRcParam = rcParam,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .padding(top = 4.dp, bottom = 8.dp),
    )
}
