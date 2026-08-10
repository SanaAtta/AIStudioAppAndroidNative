package com.example.myapplication.theme

import androidx.compose.ui.graphics.Color

val SplashBackground = Color(0xFF0B1220)
val SplashAccent = Color(0xFF2DD4BF)
val SplashOnBackground = Color(0xFFE8EEF7)
val SplashMuted = Color(0xFF8B9BB4)

val HomeBackground = Color(0xFF000000)
val HomeSurface = Color(0xFF121212)
val HomeNavBar = Color(0xFF1A1A1A)
val HomeOnBackground = Color(0xFFFFFFFF)
val HomeMuted = Color(0xFFB0B0B0)
val HomeSeeAll = Color(0xFF4A9EFF)
val HomeNavSelected = Color(0xFF4A9EFF)
val HomeNavUnselected = Color(0xFF9E9E9E)

val ImageCardStart = Color(0xFF5B2C8A)
val ImageCardEnd = Color(0xFF1A3A6B)
val VideoCardStart = Color(0xFF1A4A8A)
val VideoCardEnd = Color(0xFF0D2A5A)
val MusicCardStart = Color(0xFF0D5A5A)
val MusicCardEnd = Color(0xFF0A3A3A)
val ChatCardStart = Color(0xFF5A3A1A)
val ChatCardEnd = Color(0xFF3A2510)

val ScreenSurface = Color(0xFF1C1C1E)
val ScreenSurfaceAlt = Color(0xFF2C2C2E)
val Hint = Color(0xFF8E8E93)
val DisabledButton = Color(0xFF3A3A3C)

fun Long.toComposeColor(): Color = Color(this)
