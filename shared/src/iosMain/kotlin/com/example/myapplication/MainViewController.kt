package com.example.myapplication

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController(
    configure = {
        // Avoid hard-crash if Info.plist merge misses this key during Xcode builds.
        enforceStrictPlistSanityCheck = false
    },
) {
    App()
}
