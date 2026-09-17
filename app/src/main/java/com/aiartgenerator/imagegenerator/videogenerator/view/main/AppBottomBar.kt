package com.aiartgenerator.imagegenerator.videogenerator.view.main

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.BottomNavItem
import com.aiartgenerator.imagegenerator.videogenerator.model.bottomNavEndItems
import com.aiartgenerator.imagegenerator.videogenerator.model.bottomNavStartItems
import com.aiartgenerator.imagegenerator.videogenerator.model.isSelected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeNavBar
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeNavSelected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeNavUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.OnboardingGradientEnd
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.OnboardingGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

object BottomNavSpec {
    val barHeight: Dp = 72.dp
    val createButtonSize: Dp = 54.dp
    val createButtonCorner: Dp = 16.dp
    val createLift: Dp = 10.dp
    val contentHeight: Dp get() = barHeight + createLift

    /** Bar content height plus the system navigation-bar inset. */
    @Composable
    fun totalHeight(): Dp {
        val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        return contentHeight + navBarInset
    }
}

private val CreateButtonGradient = Brush.linearGradient(
    colors = listOf(OnboardingGradientStart, OnboardingGradientEnd),
    start = Offset(0f, 0f),
    end = Offset(200f, 200f),
)

@Composable
fun AppBottomBar(
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomNavSpec.contentHeight)
                .align(Alignment.BottomCenter)
                .graphicsLayer { clip = false },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomNavSpec.barHeight)
                    .align(Alignment.BottomCenter)
                    .background(HomeNavBar),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomNavSpec.barHeight)
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp)
                    .graphicsLayer { clip = false },
                verticalAlignment = Alignment.CenterVertically,
            ) {
            bottomNavStartItems.forEach { item ->
                BottomNavCell(
                    item = item,
                    selected = item.isSelected(currentRoute),
                    onClick = trackedClick("nav_${item.route}") { onItemClick(item) },
                    modifier = Modifier.weight(1f),
                )
            }

            CreateNavCell(
                onClick = trackedClick("nav_create", onClick = onCreateClick),
                modifier = Modifier.weight(1f),
            )

            bottomNavEndItems.forEach { item ->
                BottomNavCell(
                    item = item,
                    selected = item.isSelected(currentRoute),
                    onClick = trackedClick("nav_${item.route}") { onItemClick(item) },
                    modifier = Modifier.weight(1f),
                )
            }
            }
        }
    }
}

@Composable
private fun CreateNavCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(BottomNavSpec.createButtonCorner)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BottomNavSpec.createButtonSize)
                .offset(y = -BottomNavSpec.createLift)
                 .brandButtonShadow(shape)
                .background(CreateButtonGradient, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create",
                tint = white,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun BottomNavCell(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) HomeNavSelected else HomeNavUnselected
    val title = stringResource(item.titleRes)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter = painterResource(id = item.iconRes),
            contentDescription = title,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(contentColor),
        )
        Text(
            text = title,
            color = contentColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
