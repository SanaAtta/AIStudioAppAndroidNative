package com.aiartgenerator.imagegenerator.videogenerator.view.common

import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

enum class ResultActionStyle {
    Primary,
    Secondary,
}

@Composable
fun ResultActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ResultActionStyle = ResultActionStyle.Primary,
    selected: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    analyticsName: String? = null,
) {
    val trackedOnClick =
        trackedClick(
            itemId = analyticsName ?: ("result_" + label.lowercase().replace(" ", "_")),
            onClick = onClick,
        )
    val resolvedStyle = when {
        style == ResultActionStyle.Primary -> ResultActionStyle.Primary
        selected -> ResultActionStyle.Primary
        else -> ResultActionStyle.Secondary
    }
    val contentColor = when (resolvedStyle) {
        ResultActionStyle.Primary -> white
        ResultActionStyle.Secondary -> HomeOnBackground
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .then(
                when (resolvedStyle) {
                    ResultActionStyle.Primary -> {
                        Modifier
                            .brandButtonShadow(shape)
                            .background(ProGradientBrush)
                    }
                    ResultActionStyle.Secondary -> {
                        Modifier
                            .background(GeneratorCardBackground)
                            .border(1.dp, GeneratorInputBorder, shape)
                    }
                },
            )
            .clickable(onClick = trackedOnClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
