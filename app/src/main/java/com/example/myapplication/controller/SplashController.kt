package com.example.myapplication.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SplashController(
    private val displayMs: Long = 2_200L,
) {
    var isFinished by mutableStateOf(false)
        private set

    val splashDisplayMs: Long get() = displayMs

    fun onSplashFinished() {
        isFinished = true
    }
}
