package com.example.myapplication.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HomeNavSelected,
    onPrimary = Color.White,
    secondary = HomeSeeAll,
    background = HomeBackground,
    surface = HomeSurface,
    onBackground = HomeOnBackground,
    onSurface = HomeOnBackground,
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
