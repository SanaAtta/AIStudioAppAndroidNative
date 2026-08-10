package com.example.myapplication.view.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.controller.AiVideoGeneratorController
import com.example.myapplication.model.StyleOption
import com.example.myapplication.view.common.GeneratedVideoCard
import com.example.myapplication.view.common.GenerationLoadingCard
import kotlinx.coroutines.launch
import com.example.myapplication.model.VideoDuration
import com.example.myapplication.model.VideoResolution
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeMuted
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import com.example.myapplication.view.theme.MyApplicationTheme

private val Surface = Color(0xFF1C1C1E)
private val SurfaceAlt = Color(0xFF2C2C2E)
private val Hint = Color(0xFF8E8E93)
private val DisabledButton = Color(0xFF3A3A3C)
private val SelectedFill = Color(0xFF1A2A4A)

@Composable
fun AiVideoGeneratorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiVideoGeneratorController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(controller.errorMessage) {
        controller.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            controller.clearError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HomeOnBackground,
                )
            }
            Text(
                text = "AI Video Generator",
                color = HomeOnBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            PromptSection(
                text = controller.prompt,
                maxChars = controller.maxChars,
                onTextChange = controller::onPromptChange,
                onSuggestClick = controller::onSuggest,
            )
            StyleSection(
                styles = controller.styles,
                selectedId = controller.selectedStyleId,
                onSelected = controller::onStyleSelected,
            )
            ResolutionSection(
                resolutions = controller.resolutions,
                selectedId = controller.selectedResolutionId,
                onSelected = controller::onResolutionSelected,
            )
            DurationSection(
                durations = controller.durations,
                selectedId = controller.selectedDurationId,
                onSelected = controller::onDurationSelected,
            )
            GenerateVideoButton(
                enabled = controller.canGenerate,
                isLoading = controller.isLoading,
                onClick = { scope.launch { controller.onGenerate(context) } },
            )
            if (controller.isLoading) {
                GenerationLoadingCard(label = "Generating video…")
            }
            controller.resultUri?.let { uri ->
                GeneratedVideoCard(videoUri = uri)
            }
        }
    }
}

@Composable
private fun PromptSection(
    text: String,
    maxChars: Int,
    onTextChange: (String) -> Unit,
    onSuggestClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Generate Video",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Surface)
                .padding(16.dp),
        ) {
            Text(
                text = "Enter Prompt here",
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                textStyle = TextStyle(
                    color = HomeOnBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(HomeNavSelected),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Describe what you want to generate....",
                                color = Hint,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceAlt)
                        .clickable(onClick = onSuggestClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = HomeOnBackground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Suggest me",
                        color = HomeOnBackground,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = "${text.length}/$maxChars characters",
                    color = Hint,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun StyleSection(
    styles: List<StyleOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Select Style",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            styles.forEach { style ->
                Column(
                    modifier = Modifier
                        .width(92.dp)
                        .clickable { onSelected(style.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(style.tileColor)
                            .then(
                                if (style.id == selectedId) {
                                    Modifier.border(
                                        BorderStroke(2.dp, HomeNavSelected),
                                        RoundedCornerShape(14.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = style.title,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = style.title,
                        color = HomeOnBackground,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolutionSection(
    resolutions: List<VideoResolution>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Resolution",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            resolutions.forEach { resolution ->
                val selected = resolution.id == selectedId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) SelectedFill else Surface)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(1.5.dp, HomeNavSelected),
                                    RoundedCornerShape(14.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelected(resolution.id) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = resolution.label,
                        color = HomeOnBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resolution.subtitle,
                        color = Hint,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationSection(
    durations: List<VideoDuration>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Duration",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            durations.forEach { duration ->
                val selected = duration.id == selectedId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) SelectedFill else Surface)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(1.5.dp, HomeNavSelected),
                                    RoundedCornerShape(14.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelected(duration.id) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = duration.label,
                        color = HomeOnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerateVideoButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeNavSelected,
            contentColor = Color.White,
            disabledContainerColor = DisabledButton,
            disabledContentColor = HomeMuted,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = if (isLoading) "Generating…" else "Generate Video",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiVideoGeneratorScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiVideoGeneratorScreen(onBack = {})
    }
}
