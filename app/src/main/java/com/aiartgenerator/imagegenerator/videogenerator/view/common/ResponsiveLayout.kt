package com.aiartgenerator.imagegenerator.videogenerator.view.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ScreenWidthClass {
    Compact,
    Medium,
    Expanded,
}

enum class ScreenHeightClass {
    Compact,
    Medium,
    Tall,
}

data class AppLayoutMetrics(
    val widthClass: ScreenWidthClass,
    val heightClass: ScreenHeightClass,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp?,
    val sectionSpacing: Dp,
    val isCompactWidth: Boolean,
    val isCompactHeight: Boolean,
    val titleScale: Float,
    val bodyScale: Float,
    val buttonHeight: Dp,
    val chatBubbleMaxWidth: Dp,
) {
    fun scaledSp(base: Float): TextUnit = (base * bodyScale).sp
    fun scaledTitleSp(base: Float): TextUnit = (base * titleScale).sp

    fun gridColumnCount(default: Int = 3): Int = when {
        screenWidthDp >= 720 -> 4
        screenWidthDp >= 480 -> 3
        screenWidthDp < 340 -> 2
        else -> default
    }
}

@Composable
fun rememberAppLayoutMetrics(): AppLayoutMetrics {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp

    return remember(screenWidthDp, screenHeightDp) {
        val widthClass = when {
            screenWidthDp >= 600 -> ScreenWidthClass.Expanded
            screenWidthDp >= 400 -> ScreenWidthClass.Medium
            else -> ScreenWidthClass.Compact
        }
        val heightClass = when {
            screenHeightDp >= 780 -> ScreenHeightClass.Tall
            screenHeightDp >= 640 -> ScreenHeightClass.Medium
            else -> ScreenHeightClass.Compact
        }
        val compactWidth = widthClass == ScreenWidthClass.Compact
        val compactHeight = heightClass == ScreenHeightClass.Compact

        AppLayoutMetrics(
            widthClass = widthClass,
            heightClass = heightClass,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            horizontalPadding = when (widthClass) {
                ScreenWidthClass.Compact -> if (screenWidthDp < 340) 12.dp else 16.dp
                ScreenWidthClass.Medium -> 20.dp
                ScreenWidthClass.Expanded -> 24.dp
            },
            contentMaxWidth = when (widthClass) {
                ScreenWidthClass.Expanded -> 560.dp
                ScreenWidthClass.Medium -> if (screenWidthDp >= 480) 480.dp else null
                ScreenWidthClass.Compact -> null
            },
            sectionSpacing = when {
                compactHeight -> 8.dp
                compactWidth -> 10.dp
                else -> 12.dp
            },
            isCompactWidth = compactWidth,
            isCompactHeight = compactHeight,
            titleScale = when (widthClass) {
                ScreenWidthClass.Compact -> if (screenWidthDp < 340) 0.88f else 0.94f
                ScreenWidthClass.Expanded -> 1.05f
                ScreenWidthClass.Medium -> 1f
            },
            bodyScale = when {
                compactWidth && compactHeight -> 0.9f
                compactWidth || compactHeight -> 0.94f
                else -> 1f
            },
            buttonHeight = when {
                compactHeight -> 50.dp
                compactWidth -> 52.dp
                else -> 56.dp
            },
            chatBubbleMaxWidth = (screenWidthDp * 0.76f).coerceIn(220f, 320f).dp,
        )
    }
}

fun Modifier.responsiveContentWidth(metrics: AppLayoutMetrics): Modifier {
    val maxWidth = metrics.contentMaxWidth ?: return fillMaxWidth()
    return fillMaxWidth().widthIn(max = maxWidth)
}

@Composable
fun ResponsiveWidthContainer(
    modifier: Modifier = Modifier,
    content: @Composable (AppLayoutMetrics) -> Unit,
) {
    val metrics = rememberAppLayoutMetrics()
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.responsiveContentWidth(metrics)) {
            content(metrics)
        }
    }
}

@Composable
fun ResponsiveScrollColumn(
    modifier: Modifier = Modifier,
    metrics: AppLayoutMetrics = rememberAppLayoutMetrics(),
    bottomPadding: Dp = 12.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.(AppLayoutMetrics) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .responsiveContentWidth(metrics)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = metrics.horizontalPadding)
                .padding(bottom = bottomPadding),
            verticalArrangement = verticalArrangement,
        ) {
            content(metrics)
        }
    }
}

@Composable
fun ResponsiveScreenRoot(
    modifier: Modifier = Modifier,
    content: @Composable (AppLayoutMetrics) -> Unit,
) {
    val metrics = rememberAppLayoutMetrics()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .responsiveContentWidth(metrics),
        ) {
            content(metrics)
        }
    }
}
