package com.aiartgenerator.imagegenerator.videogenerator.view.music

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.aiartgenerator.imagegenerator.videogenerator.model.GenerationSession
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiMusicGeneratorController
import com.aiartgenerator.imagegenerator.videogenerator.controller.PromptSafety
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusic
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedMusicsRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicDurationOption
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicGenreOption
import com.aiartgenerator.imagegenerator.videogenerator.model.MusicMood
import com.aiartgenerator.imagegenerator.videogenerator.model.VoiceStyleOption
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

private val Hint = Color(0xFF8E8E93)

@Composable
fun AiMusicGeneratorScreen(
    onBack: () -> Unit,
    onGenerating: () -> Unit,
    onResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiMusicGeneratorController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generateGate = rememberFreeGenerationGate(FreeUsageKind.Music)
    val lifecycleOwner = LocalLifecycleOwner.current
    val recentMusicState = GeneratedMusicsRepository.observeItems()
    val recentMusic by recentMusicState

    LaunchedEffect(Unit) {
        GeneratedMusicsRepository.load(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                GeneratedMusicsRepository.load(context)
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
                title = stringResource(R.string.music_generator_title),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = metrics.horizontalPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
            MusicPromptSection(
                text = controller.prompt,
                onTextChange = controller::onPromptChange,
            )
            MoodSection(
                moods = controller.moods,
                selectedId = controller.selectedMoodId,
                onSelected = controller::onMoodSelected,
            )
            GenreSection(
                genres = controller.genres,
                selectedId = controller.selectedGenreId,
                onSelected = controller::onGenreSelected,
            )
            DurationSection(
                durations = controller.durations,
                selectedId = controller.selectedDurationId,
                selectedLabel = controller.selectedDurationLabel,
                onSelected = controller::onDurationSelected,
            )
            VoiceStyleSection(
                voiceStyles = controller.voiceStyles,
                selectedId = controller.selectedVoiceStyleId,
                onSelected = controller::onVoiceStyleSelected,
            )
            GenerateActionButton(
                label = stringResource(R.string.generate_music),
                icon = Icons.Filled.MusicNote,
                enabled = controller.canGenerate || generateGate.locked,
                isLoading = controller.isLoading,
                loadingLabel = stringResource(R.string.generating_music),
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
                    GenerationSession.start(GenerationSession.Kind.Music) {
                        var ok = false
                        ctrl.onGenerate(ctx, onSuccess = { ok = true })
                        if (!ok) {
                            error(ctrl.errorMessage ?: "Music generation failed")
                        }
                    }
                    onGenerating()
                },
                shape = RoundedCornerShape(13.dp),
            )
            if (recentMusic.isNotEmpty()) {
                RecentMusicSection(
                    tracks = recentMusic,
                    onTrackClick = { track ->
                        GeneratedMusicsRepository.loadIntoStore(context, track)
                        onResult()
                    },
                )
            }
        }
        }
    }
}

@Composable
private fun MusicPromptSection(
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
                text = stringResource(R.string.music_prompt_label),
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
                            text = GeneratorDefaultPrompts.MUSIC,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodSection(
    moods: List<MusicMood>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.mood),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            moods.forEach { mood ->
                GeneratorSelectionChip(
                    label = "${mood.emoji} ${mood.label}",
                    selected = mood.id == selectedId,
                    onClick = { onSelected(mood.id) },
                )
            }
        }
    }
}

@Composable
private fun GenreSection(
    genres: List<MusicGenreOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.genre),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            genres.forEach { genre ->
                GeneratorSelectionChip(
                    label = genre.label,
                    selected = genre.id == selectedId,
                    onClick = { onSelected(genre.id) },
                )
            }
        }
    }
}

@Composable
private fun DurationSection(
    durations: List<MusicDurationOption>,
    selectedId: String,
    selectedLabel: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.duration),
                color = HomeOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = selectedLabel,
                color = ProGradientStart,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            durations.forEach { duration ->
                GeneratorSelectionChip(
                    label = duration.label,
                    selected = duration.id == selectedId,
                    onClick = { onSelected(duration.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun VoiceStyleSection(
    voiceStyles: List<VoiceStyleOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.voice_style),
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            voiceStyles.forEach { voice ->
                GeneratorSelectionChip(
                    label = voice.label,
                    selected = voice.id == selectedId,
                    onClick = { onSelected(voice.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RecentMusicSection(
    tracks: List<GeneratedMusic>,
    onTrackClick: (GeneratedMusic) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tracks.take(5).forEach { track ->
            RecentMusicRow(
                track = track,
                onClick = { onTrackClick(track) },
            )
        }
    }
}

@Composable
private fun RecentMusicRow(
    track: GeneratedMusic,
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
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = white,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${track.durationSeconds}s · ${track.genreLabel}",
                color = HomeMuted,
                fontSize = 13.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.5.dp, ProGradientStart), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = ProGradientStart,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiMusicGeneratorScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiMusicGeneratorScreen(onBack = {}, onGenerating = {}, onResult = {})
    }
}
