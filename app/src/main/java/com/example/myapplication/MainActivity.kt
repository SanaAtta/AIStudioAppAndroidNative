package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapplication.setup.SetupNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Always run Splash (with ads) each cold start; Language/Onboarding only first time.
            var showSetup by remember { mutableStateOf(true) }
            if (showSetup) {
                SetupNavHost(onSetupFinished = { showSetup = false })
            } else {
                App()
            }
        }
    }
}
