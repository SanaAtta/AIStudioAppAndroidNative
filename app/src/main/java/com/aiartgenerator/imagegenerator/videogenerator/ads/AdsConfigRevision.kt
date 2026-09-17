package com.aiartgenerator.imagegenerator.videogenerator.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess

/**
 * Incremented on the main thread after Firebase Remote Config applies [AdsControl] and [AdUnitIds].
 * Composables read [state] so native ad views re-key and reload when RC arrives.
 * Also bumped when subscription status changes so ads unmount immediately.
 */
object AdsConfigRevision {
    val state: MutableState<Int> = mutableStateOf(0)

    fun bump() {
        state.value++
    }
}

/** True when ads may render. Recomposes as soon as premium or RC changes. */
@Composable
fun rememberAdsAllowed(): Boolean {
    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()
    val revision by AdsConfigRevision.state
    return !isPremium && AdsControl.shouldShowAds() && revision >= 0
}
