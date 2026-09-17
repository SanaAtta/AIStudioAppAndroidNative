package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppBarDivider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground

@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            Text(
                text = title,
                color = HomeOnBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailingContent()
            if (showProAndSettings) {
                AppBarProSettingsActions(showPro = showPro, showSettings = showSettings)
            }
        }
        HorizontalDivider(color = AppBarDivider)
    }
}
