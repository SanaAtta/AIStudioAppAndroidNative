package com.aiartgenerator.imagegenerator.videogenerator.view.premium

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aiartgenerator.imagegenerator.videogenerator.controller.PremiumController

@Composable
fun rememberPremiumController(): PremiumController {
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val controller = remember { PremiumController(context.applicationContext) }

    DisposableEffect(activity) {
        activity?.let(controller::bindActivity)
        controller.startBilling()
        onDispose {
            controller.stopBilling()
            controller.unbindActivity()
        }
    }

    return controller
}
