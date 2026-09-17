package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.actionGradientBackground

/** Padding for a FAB fixed slightly above the bottom navigation bar on tab screens. */
fun Modifier.fabAboveBottomNav(): Modifier = padding(end = 20.dp, bottom = 16.dp)

@Composable
fun ChatStyleFloatingFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "AI Chat",
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .actionGradientBackground(CircleShape)
            .clickable(onClick = trackedClick("fab_chatbot", onClick = onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_chatbot_fab),
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
        )
    }
}
