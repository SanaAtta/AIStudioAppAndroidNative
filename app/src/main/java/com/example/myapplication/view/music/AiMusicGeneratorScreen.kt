package com.example.myapplication.view.music

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
import androidx.compose.material.icons.filled.Close
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
import com.example.myapplication.controller.AiMusicGeneratorController
import com.example.myapplication.model.MusicGenre
import com.example.myapplication.view.common.GeneratedAudioCard
import com.example.myapplication.view.common.GenerationLoadingCard
import kotlinx.coroutines.launch
import com.example.myapplication.model.VocalOption
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeMuted
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import com.example.myapplication.view.theme.MyApplicationTheme

private val Surface = Color(0xFF1C1C1E)
private val SurfaceAlt = Color(0xFF2C2C2E)
private val Hint = Color(0xFF8E8E93)
private val DisabledButton = Color(0xFF3A3A3C)

@Composable
fun AiMusicGeneratorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiMusicGeneratorController() }
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
                text = "AI Music Generator",
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
                onClear = controller::onClearPrompt,
                onSurpriseClick = controller::onSurpriseMe,
            )
            GenreSection(
                genres = controller.genres,
                selectedId = controller.selectedGenreId,
                onSelected = controller::onGenreSelected,
            )
            VocalSection(
                vocals = controller.vocals,
                selectedId = controller.selectedVocalId,
                onSelected = controller::onVocalSelected,
            )
            GenerateMusicButton(
                enabled = controller.canGenerate,
                isLoading = controller.isLoading,
                onClick = { scope.launch { controller.onGenerate(context) } },
            )
            if (controller.isLoading) {
                GenerationLoadingCard(label = "Generating music…")
            }
            controller.resultUri?.let { uri ->
                GeneratedAudioCard(audioUri = uri, title = "Generated track")
            }
        }
    }
}

@Composable
private fun PromptSection(
    text: String,
    maxChars: Int,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSurpriseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Describe Song",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enter Prompt here",
                    color = HomeOnBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = Hint,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onClear),
                    )
                }
            }
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
                                text = "Describe your song (e.g.: A chill song in rainy weather)",
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
                        .clickable(onClick = onSurpriseClick)
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
                        text = "Surprise me",
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
private fun GenreSection(
    genres: List<MusicGenre>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Genre (optional)",
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
            genres.forEach { genre ->
                Column(
                    modifier = Modifier
                        .width(92.dp)
                        .clickable { onSelected(genre.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(genre.tileColor)
                            .then(
                                if (genre.id == selectedId) {
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
                            imageVector = genre.icon,
                            contentDescription = genre.title,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = genre.title,
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
private fun VocalSection(
    vocals: List<VocalOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Vocal Selection",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            vocals.forEach { vocal ->
                val selected = vocal.id == selectedId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(2.dp, HomeNavSelected),
                                    RoundedCornerShape(14.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelected(vocal.id) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = vocal.shortLabel,
                        color = HomeOnBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = vocal.fullLabel,
                        color = Hint,
                        fontSize = 11.sp,
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
private fun GenerateMusicButton(
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
            text = if (isLoading) "Generating…" else "Generate Music",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiMusicGeneratorScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiMusicGeneratorScreen(onBack = {})
    }
}
