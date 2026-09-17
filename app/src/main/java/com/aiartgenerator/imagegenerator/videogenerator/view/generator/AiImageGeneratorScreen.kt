package com.aiartgenerator.imagegenerator.videogenerator.view.generator

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiImageGeneratorController
import com.aiartgenerator.imagegenerator.videogenerator.controller.PromptSafety
import com.aiartgenerator.imagegenerator.videogenerator.model.AspectRatioOption
import com.aiartgenerator.imagegenerator.videogenerator.model.GenerationSession
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import com.aiartgenerator.imagegenerator.videogenerator.model.StyleOption
import com.aiartgenerator.imagegenerator.videogenerator.view.common.BackScreenTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GenerateActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberFreeGenerationGate
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PlusJakartaSansFamily
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch

private val Hint = Color(0xFF8E8E93)
private val SectionSpacing = 24.dp
private val LabelSpacing = 12.dp
private val InputShape = RoundedCornerShape(18.dp)
private val ChipShape = RoundedCornerShape(12.dp)
private val StyleCardShape = RoundedCornerShape(16.dp)

@Composable
fun AiImageGeneratorScreen(
    onBack: () -> Unit,
    onGenerating: () -> Unit,
    onResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiImageGeneratorController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generateGate = rememberFreeGenerationGate(
        kind = FreeUsageKind.Image,
        rewardedAdOnGenerate = true,
    )

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
                title = stringResource(R.string.image_generator_title),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = metrics.horizontalPadding)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            ) {
                PromptSection(
                    text = controller.prompt,
                    hint = GeneratorDefaultPrompts.forImageStyle(controller.selectedStyleId),
                    maxChars = controller.maxChars,
                    enabled = !controller.isLoading,
                    onTextChange = controller::onPromptChange,
                )
                StyleSection(
                    styles = controller.styles,
                    selectedId = controller.selectedStyleId,
                    enabled = !controller.isLoading,
                    onSelected = controller::onStyleSelected,
                )
                AspectRatioSection(
                    options = controller.aspectRatios,
                    selectedId = controller.selectedAspectRatioId,
                    enabled = !controller.isLoading,
                    onSelected = controller::onAspectRatioSelected,
                )
            }

            GenerateActionButton(
                label = stringResource(R.string.generate_image),
                enabled = controller.canGenerate || generateGate.locked,
                isLoading = controller.isLoading,
                loadingLabel = stringResource(R.string.generating_image),
                locked = generateGate.locked,
                onLockedClick = generateGate.onLockedClick,
                showAdBadge = generateGate.showAdBadge,
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
                    GenerationSession.start(GenerationSession.Kind.Image) {
                        var ok = false
                        ctrl.onGenerate(ctx, onSuccess = { ok = true })
                        if (!ok) {
                            error(ctrl.errorMessage ?: "Image generation failed")
                        }
                    }
                    onGenerating()
                },
                modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun PromptSection(
    text: String,
    hint: String,
    maxChars: Int,
    onTextChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(stringResource(R.string.image_prompt_label))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (text.isNotEmpty() && enabled) {
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
                if (enabled) {
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
        }
        Spacer(modifier = Modifier.height(LabelSpacing))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(InputShape)
                .background(Color.White.copy(alpha = if (enabled) 0.07f else 0.04f))
                .border(1.dp, GeneratorInputBorder, InputShape)
                .padding(16.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                readOnly = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                textStyle = TextStyle(
                    fontFamily = PlusJakartaSansFamily,
                    color = if (enabled) SplashOnBackground else SplashOnBackground.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
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
                                text = hint,
                                color = Hint,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Text(
                text = stringResource(R.string.char_counter, text.length, maxChars),
                color = Hint,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun StyleSection(
    styles: List<StyleOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.styles))
        Spacer(modifier = Modifier.height(LabelSpacing))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            styles.forEach { style ->
                StyleCard(
                    style = style,
                    selected = style.id == selectedId,
                    enabled = enabled,
                    onClick = { if (enabled) onSelected(style.id) },
                )
            }
        }
    }
}

@Composable
private fun StyleCard(
    style: StyleOption,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(112.dp)
            .height(132.dp)
            .clip(StyleCardShape)
            .border(
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) ProGradientStart else GeneratorInputBorder,
                ),
                shape = StyleCardShape,
            )
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.55f),
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
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = style.title,
                color = white,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AspectRatioSection(
    options: List<AspectRatioOption>,
    selectedId: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.aspect_ratio))
        Spacer(modifier = Modifier.height(LabelSpacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { option ->
                val selected = option.id == selectedId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ChipShape)
                        .then(
                            if (selected) {
                                Modifier.background(ProGradientBrush)
                            } else {
                                Modifier.background(ChipUnselected)
                            },
                        )
                        .then(
                            if (enabled) {
                                Modifier.clickable { onSelected(option.id) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) white else HomeMuted,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = SplashOnBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AiImageGeneratorScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        AiImageGeneratorScreen(
            onBack = {},
            onGenerating = {}, onResult = {},
        )
    }
}
