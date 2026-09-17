package com.aiartgenerator.imagegenerator.videogenerator.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val SplashBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().splashBackground

val SplashOnBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().splashOnBackground

val SplashMuted: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().splashMuted

val white = Color(0xFFFFFFFF)
val SplashProgressTrack: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().splashProgressTrack

val LanguageCardBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().languageCardBackground

val LanguageCardBorderUnselected: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().languageCardBorderUnselected

val LanguageSelectedBorder: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().languageSelectedBorder

val LanguageSelectedText: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().languageSelectedText

val HomeBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeBackground

val HomeSurface: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeSurface

val HomeNavBar: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeNavBar

val HomeOnBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeOnBackground

val HomeMuted: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeMuted

val HomeSeeAll: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeSeeAll

val HomeNavSelected: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeNavSelected

val HomeNavUnselected: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().homeNavUnselected

val ImageCardStart = Color(0xFF5B2C8A)
val ImageCardEnd = Color(0xFF1A3A6B)

private fun homeCardGradientBrush(start: Color, mid: Color, end: Color): Brush =
    Brush.linearGradient(
        colorStops = arrayOf(
            0f to start,
            0.4f to mid,
            1f to end,
        ),
        start = Offset.Zero,
        end = Offset(500f, 500f),
    )

val VideoCardGradientBrush: Brush
    @Composable
    @ReadOnlyComposable
    get() {
        val colors = appThemeColors()
        return homeCardGradientBrush(
            colors.videoCardGradientStart,
            colors.videoCardGradientMid,
            colors.videoCardGradientEnd,
        )
    }

val VideoCardBorder: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().videoCardBorder

val VideoCardSubtitle = Color(0xFF9EB0E8)

val MusicCardGradientBrush: Brush
    @Composable
    @ReadOnlyComposable
    get() {
        val colors = appThemeColors()
        return homeCardGradientBrush(
            colors.musicCardGradientStart,
            colors.musicCardGradientMid,
            colors.musicCardGradientEnd,
        )
    }

val MusicCardBorder: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().musicCardBorder

val MusicCardSubtitle = Color(0xFFB8A8E8)

val ChatCardStart = Color(0xFF2A1045)
val ChatCardEnd = Color(0xFF1A1035)

val ChatAiBubble: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().chatAiBubble

val ChatSurface: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().chatSurface

val ChatOnline: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().chatOnline

val ChatInputHint: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().chatInputHint

val ProGradientStart = Color(0xFF7C4DFF)
val ProGradientEnd = Color(0xFF5C2DF5)
val ProGradientBrush = Brush.horizontalGradient(listOf(ProGradientStart, ProGradientEnd))
val SplashAccent = ProGradientStart

val OnboardingGradientStart = Color(0xFF2F80ED)
val OnboardingGradientEnd = Color(0xFF8A2BE2)
val OnboardingButtonBrush = Brush.horizontalGradient(listOf(OnboardingGradientStart, OnboardingGradientEnd))

val SettingsCardBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().settingsCardBackground

val SettingsSectionLabel: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().settingsSectionLabel

val SettingsIconBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().settingsIconBackground

val SettingsIconTint = Color(0xFF9A75F9)

val SettingsTrailingText: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().settingsTrailingText

val SettingsDivider: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().settingsDivider

val SettingsProSubtitle: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().settingsProSubtitle

val SettingsProCrownBg = Color(0xFFFF9500)
val ProBadgeGradientEnd = Color(0xFFFFD60A)
val ProBadgeGradientBrush = Brush.horizontalGradient(listOf(SettingsProCrownBg, ProBadgeGradientEnd))

val AllScreenStart: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().screenGradientStart

val AllScreenStartEnd: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().screenGradientEnd

val AllScreenBgGradientBrush: Brush
    @Composable @ReadOnlyComposable
    get() = Brush.horizontalGradient(listOf(AllScreenStart, AllScreenStartEnd))

val PremiumBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().premiumBackground

val PremiumFeatureBorder = Color(0xFF7C4DFF)
val PremiumFeatureContainer: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().premiumFeatureContainer

val PremiumHeadlineGold = Color(0xFFFFD600)
val PremiumPlanSelectedStart: Color
    @Composable @ReadOnlyComposable
    get() =
        if (LocalIsDarkTheme.current) {
            Color(0xFF5C2DF5)
        } else {
            Color(0xFFD9CFFF)
        }

val PremiumPlanSelectedEnd: Color
    @Composable @ReadOnlyComposable
    get() =
        if (LocalIsDarkTheme.current) {
            Color(0xFF1A1035)
        } else {
            Color(0xFFF7F4FF)
        }

val PremiumPlanUnselected: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().premiumPlanUnselected

val PremiumPlanBorder: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().premiumPlanBorder

val PremiumPlanSelectedBorder: Color
    @Composable @ReadOnlyComposable
    get() =
        if (LocalIsDarkTheme.current) {
            Color(0xFF9A75F9)
        } else {
            Color(0xFF8B7CF6)
        }

val PremiumPlanSelectedContent: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFFFFFFF) else Color(0xFF2A1F5E)
val PremiumSaveBadge = Color(0xFF4CAF50)
val PremiumTrialCheck = Color(0xFF4CAF50)
val PremiumSubscribeText = Color(0xFF600000)
val SubscribeGradientStart = Color(0xFFFF9500)
val SubscribeGradientEnd = Color(0xFFFFBA24)
val SubscribeGradientBrush = Brush.horizontalGradient(listOf(SubscribeGradientStart, SubscribeGradientEnd))

val GeneratorCardBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().generatorCardBackground

val GeneratorInputBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().generatorInputBackground

val GeneratorInputBorder: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().generatorInputBorder

val ChipUnselected: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().chipUnselected

val DisabledButton: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().disabledButton

val DisabledButtonText: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().disabledButtonText

val ToolTileBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().toolTileBackground

val ToolTileText: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().toolTileText

val AppBarDivider: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().appBarDivider

val PremiumBadgeBackground: Color
    @Composable @ReadOnlyComposable get() = appThemeColors().premiumBadgeBackground
