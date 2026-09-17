package com.aiartgenerator.imagegenerator.videogenerator.view.video

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiVideoGeneratorController
import com.aiartgenerator.imagegenerator.videogenerator.controller.PromptSafety
import com.aiartgenerator.imagegenerator.videogenerator.model.GenerationSession
import com.aiartgenerator.imagegenerator.videogenerator.model.AspectRatioOption
import com.aiartgenerator.imagegenerator.videogenerator.model.CameraMotion
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideo
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedVideosRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import com.aiartgenerator.imagegenerator.videogenerator.model.StyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.VideoDuration
import com.aiartgenerator.imagegenerator.videogenerator.view.common.BackScreenTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GenerateActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberFreeGenerationGate
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GeneratorSelectionChip
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.chipButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PlusJakartaSansFamily
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Hint = Color(0xFF8E8E93)

@Composable
fun AiVideoGeneratorScreen(
    onBack: () -> Unit,
    onGenerating: () -> Unit,
    onResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiVideoGeneratorController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generateGate = rememberFreeGenerationGate(FreeUsageKind.Video)
    val lifecycleOwner = LocalLifecycleOwner.current
    val recentVideosState = GeneratedVideosRepository.observeItems()
    val recentVideos by recentVideosState

    LaunchedEffect(Unit) {
        GeneratedVideosRepository.load(context)
        controller.applyEditDraftIfAny()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                GeneratedVideosRepository.load(context)
                controller.applyEditDraftIfAny()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(controller.errorMessage) {
        controller.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            controller.clearError()
        }
    }

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            BackScreenTopBar(
                onBack = onBack,
                title = stringResource(R.string.video_generator_title),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = metrics.horizontalPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
            VideoPromptSection(
                text = controller.prompt,
                onTextChange = controller::onPromptChange,
            )
            DurationSliderSection(
                durations = controller.durations,
                selectedId = controller.selectedDurationId,
                onSelected = controller::onDurationSelected,
            )
            CameraMotionSection(
                motions = controller.cameraMotions,
                selectedId = controller.selectedCameraMotionId,
                onSelected = controller::onCameraMotionSelected,
            )
            VideoStyleSection(
                styles = controller.styles,
                selectedId = controller.selectedStyleId,
                onSelected = controller::onStyleSelected,
            )
            AspectRatioSection(
                options = controller.aspectRatios,
                selectedId = controller.selectedAspectRatioId,
                onSelected = controller::onAspectRatioSelected,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GenerateActionButton(
                    label = stringResource(R.string.generate_video),
                    icon = Icons.Filled.Videocam,
                    enabled = controller.canGenerate || generateGate.locked,
                    isLoading = controller.isLoading,
                    loadingLabel = stringResource(R.string.generating_video),
                    locked = generateGate.locked,
                    onLockedClick = generateGate.onLockedClick,
                    onClick = {
                        if (PromptSafety.containsUnsafeContent(controller.prompt)) {
                            controller.clearError()
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_prompt_unsafe),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@GenerateActionButton
                        }
                        val ctrl = controller
                        val ctx = context
                        GenerationSession.start(GenerationSession.Kind.Video) {
                            var ok = false
                            ctrl.onGenerate(ctx, onSuccess = { ok = true })
                            if (!ok) {
                                error(ctrl.errorMessage ?: "Video generation failed")
                            }
                        }
                        onGenerating()
                    },
                    shape = RoundedCornerShape(13.dp),
                )
                if (recentVideos.isNotEmpty()) {
                    RecentVideosSection(
                        videos = recentVideos,
                        onVideoClick = { video ->
                            GeneratedVideosRepository.loadIntoStore(context, video)
                            onResult()
                        },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun VideoPromptSection(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.video_prompt_label),
                color = HomeOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (text.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.clear),
                        color = Hint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onTextChange("") }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.paste),
                    color = ProGradientStart,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                onTextChange(clip)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GeneratorCardBackground)
                .border(1.dp, GeneratorInputBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            textStyle = TextStyle(
                fontFamily = PlusJakartaSansFamily,
                color = HomeOnBackground,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(ProGradientStart),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (text.isEmpty()) {
                        Text(
                            text = GeneratorDefaultPrompts.VIDEO,
                            color = Hint,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationSliderSection(
    durations: List<VideoDuration>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = durations.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val selectedLabel = durations.getOrNull(selectedIndex)?.label.orEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.duration),
                color = HomeMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = selectedLabel,
                color = ProGradientStart,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { value ->
                durations.getOrNull(value.roundToInt())?.let { onSelected(it.id) }
            },
            valueRange = 0f..(durations.lastIndex.coerceAtLeast(0)).toFloat(),
            steps = (durations.size - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = ProGradientStart,
                inactiveTrackColor = GeneratorInputBorder,
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(2.dp, white, CircleShape)
                        .background(ProGradientStart, CircleShape),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(5.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = ProGradientStart,
                        inactiveTrackColor = GeneratorInputBorder,
                    ),
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            durations.forEach { duration ->
                Text(
                    text = duration.label,
                    color = Hint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraMotionSection(
    motions: List<CameraMotion>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.camera_motion),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            motions.forEach { motion ->
                GeneratorSelectionChip(
                    label = motion.label,
                    selected = motion.id == selectedId,
                    onClick = { onSelected(motion.id) },
                )
            }
        }
    }
}

@Composable
private fun VideoStyleSection(
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
            text = stringResource(R.string.style),
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
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .height(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            BorderStroke(
                                width = if (style.id == selectedId) 2.dp else 1.dp,
                                color = if (style.id == selectedId) ProGradientStart else GeneratorInputBorder,
                            ),
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelected(style.id) },
                ) {
                    Image(
                        painter = painterResource(id = style.previewRes),
                        contentDescription = style.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = style.title,
                            color = white,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRatioSection(
    options: List<AspectRatioOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.aspect_ratio),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                GeneratorSelectionChip(
                    label = option.label,
                    selected = option.id == selectedId,
                    onClick = { onSelected(option.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RecentVideosSection(
    videos: List<GeneratedVideo>,
    onVideoClick: (GeneratedVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.recent_videos),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            videos.take(5).forEach { video ->
                RecentVideoRow(
                    video = video,
                    onClick = { onVideoClick(video) },
                )
            }
        }
    }
}

@Composable
private fun RecentVideoRow(
    video: GeneratedVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GeneratorCardBackground)
            .border(1.dp, GeneratorInputBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProGradientBrush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = white,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${video.durationSeconds}s · ${video.styleTitle}",
                color = HomeMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiVideoGeneratorScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiVideoGeneratorScreen(onBack = {}, onGenerating = {}, onResult = {})
    }
}
