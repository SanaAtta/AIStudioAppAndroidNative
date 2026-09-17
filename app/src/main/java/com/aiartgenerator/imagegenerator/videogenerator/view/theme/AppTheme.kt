package com.aiartgenerator.imagegenerator.videogenerator.view.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppThemeColors = staticCompositionLocalOf { DarkAppThemeColors }
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun AppThemeProvider(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (isDarkTheme) DarkAppThemeColors else LightAppThemeColors
    CompositionLocalProvider(
        LocalAppThemeColors provides colors,
        LocalIsDarkTheme provides isDarkTheme,
    ) {
        SystemBarThemeEffect(isDarkTheme = isDarkTheme)
        content()
    }
}

@Composable
private fun SystemBarThemeEffect(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }
}

@Composable
@ReadOnlyComposable
fun appThemeColors(): AppThemeColors = LocalAppThemeColors.current
