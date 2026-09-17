package com.aiartgenerator.imagegenerator.videogenerator.view.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsConfigRevision
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess

data class FreeGenerationGate(
    val locked: Boolean,
    val showAdBadge: Boolean,
    val onLockedClick: (() -> Unit)?,
)

@Composable
fun rememberFreeGenerationGate(
    kind: FreeUsageKind,
    rewardedAdOnGenerate: Boolean = false,
): FreeGenerationGate {
    val usage by FreeUsageLimits.usageFlow.collectAsState()
    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()
    val adsRev = AdsConfigRevision.state.value
    val onProClick = LocalOnProClick.current
    return remember(kind, usage, isPremium, adsRev, rewardedAdOnGenerate, onProClick) {
        val locked = !isPremium && usage.used(kind) >= kind.freeLimit
        val showAdBadge =
            rewardedAdOnGenerate &&
                !isPremium &&
                !locked &&
                (
                    AdsControl.generateProgressRewardedEnabled() ||
                        AdsControl.generateProgressRewardedInterstitialEnabled()
                )
        FreeGenerationGate(
            locked = locked,
            showAdBadge = showAdBadge,
            onLockedClick = onProClick,
        )
    }
}
