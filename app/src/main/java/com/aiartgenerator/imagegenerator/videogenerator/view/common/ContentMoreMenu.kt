package com.aiartgenerator.imagegenerator.videogenerator.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

@Composable
fun ContentMoreMenu(
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false,
    tint: Color? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        if (isOverlay) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.report_more_options),
                    tint = tint ?: white,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.report_more_options),
                    tint = tint ?: HomeOnBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(GeneratorCardBackground)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.report_menu_item),
                        color = HomeOnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onReportClick()
                },
            )
        }
    }
}
