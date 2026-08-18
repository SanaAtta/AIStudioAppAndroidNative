package com.example.myapplication.ads

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Incremented on the main thread after Firebase Remote Config applies [AdsControl] and [AdUnitIds].
 * Composables read [state] so native ad views re-key and reload when RC arrives.
 */
object AdsConfigRevision {
    val state: MutableState<Int> = mutableStateOf(0)

    fun bump() {
        state.value++
    }
}
