package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SplashController(
    private val displayMs: Long = 5_000L,
) {
    var isFinished by mutableStateOf(false)
        private set

    val splashDisplayMs: Long get() = displayMs

    fun onSplashFinished() {
        isFinished = true
    }
}
