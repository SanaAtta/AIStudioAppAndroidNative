package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppBarDivider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.proBadgeGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

private val AppBarActionHeight = 36.dp
private val AppBarActionIconSize = 22.dp

val LocalOnProClick = staticCompositionLocalOf<(() -> Unit)?> { null }
val LocalOnSettingsClick = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun AppBarProSettingsActions(
    modifier: Modifier = Modifier,
    showPro: Boolean = true,
    showSettings: Boolean = true,
    onProClick: (() -> Unit)? = LocalOnProClick.current,
    onSettingsClick: (() -> Unit)? = LocalOnSettingsClick.current,
) {
    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()
    val showProBadge = showPro && !isPremium && AdsControl.showHomeAppBarPro()
    if (!showProBadge && !showSettings) return
    if (showProBadge && onProClick == null && showSettings && onSettingsClick == null) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showProBadge && onProClick != null) {
            ProBadgeButton(onClick = trackedClick("pro_badge", onClick = onProClick))
        }
        if (showSettings && onSettingsClick != null) {
            Box(
                modifier = Modifier
                    .size(AppBarActionHeight)
                    .clickable(onClick = trackedClick("settings", onClick = onSettingsClick)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = HomeOnBackground,
                    modifier = Modifier.size(AppBarActionIconSize),
                )
            }
        }
    }
}

@Composable
private fun ProBadgeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50.dp)
    val shimmer = rememberInfiniteTransition(label = "proBadgeShimmer")
    val translate by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 220f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "proBadgeShimmerTranslate",
    )
    Row(
        modifier = modifier
            .height(AppBarActionHeight)
            .proBadgeGradientBackground(shape)
            .drawWithContent {
                drawContent()
                val width = size.width
                val highlight = translate / 220f
                val startX = -width * 0.45f + (width * 1.9f * highlight)
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.35f to Color.White.copy(alpha = 0.10f),
                            0.5f to Color.White.copy(alpha = 0.28f),
                            0.65f to Color.White.copy(alpha = 0.10f),
                            1f to Color.Transparent,
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(startX + width * 0.55f, size.height),
                    ),
                    blendMode = BlendMode.SrcOver,
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = white,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.home_pro),
            color = white,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun MainTabTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (subtitle != null) 4.dp else 0.dp),
        ) {
            Text(
                text = title,
                color = HomeOnBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = HomeMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AppBarProSettingsActions()
    }
}

@Composable
fun BackScreenTopBar(
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showProAndSettings: Boolean = true,
    showPro: Boolean = true,
    showSettings: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = trackedClick("back", onClick = onBack)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = HomeOnBackground,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = HomeOnBackground,
                    fontSize = if (subtitle != null) 22.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = HomeMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailingContent()
            if (showProAndSettings) {
                AppBarProSettingsActions(showPro = showPro, showSettings = showSettings)
            }
        }
        HorizontalDivider(color = AppBarDivider)
    }
}
