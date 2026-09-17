package com.aiartgenerator.imagegenerator.videogenerator.view.chat

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiChatbotController
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatMessage
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatRole
import com.aiartgenerator.imagegenerator.videogenerator.model.ChatSuggestion
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberVoiceInputState
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChatAiBubble
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChatInputHint
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChatOnline
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.chipButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PlusJakartaSansFamily
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.launch

@Composable
fun AiChatbotScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiChatbotController() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val voiceInput = rememberVoiceInputState(
        onResult = controller::applyVoiceResult,
        onError = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    LaunchedEffect(controller.errorMessage) {
        controller.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            controller.clearError()
        }
    }

    LaunchedEffect(controller.messages.size, controller.isLoading) {
        val lastIndex = controller.messages.lastIndex +
            if (controller.isLoading) 1 else 0
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    ResponsiveScreenRoot(modifier = modifier) { layoutMetrics ->
        val density = LocalDensity.current
        val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        ) {
            ChatHeader(onBack = onBack)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = layoutMetrics.horizontalPadding,
                    vertical = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(controller.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        bubbleMaxWidth = layoutMetrics.chatBubbleMaxWidth,
                    )
                }
                if (controller.isLoading) {
                    item(key = "typing") {
                        TypingIndicatorRow()
                    }
                }
            }

            if (!isKeyboardVisible) {
                ChatSuggestionRow(
                    suggestions = controller.suggestions,
                    selectedId = controller.selectedSuggestionId,
                    onSuggestionClick = { suggestion ->
                        scope.launch { controller.onSuggestionClick(suggestion) }
                    },
                    horizontalPadding = layoutMetrics.horizontalPadding,
                )
            }

            ChatInputBar(
                text = controller.inputText,
                canSend = controller.canSend,
                isListening = voiceInput.isListening,
                onTextChange = controller::onInputChange,
                onCameraClick = {
                    Toast.makeText(context, "Camera coming soon", Toast.LENGTH_SHORT).show()
                },
                onMicClick = voiceInput.startListening,
                onSendClick = { scope.launch { controller.sendMessage() } },
                horizontalPadding = layoutMetrics.horizontalPadding,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = HomeOnBackground,
            )
        }
        ChatAvatar(size = 44.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.chat_title),
                color = HomeOnBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ChatOnline),
                )
                Text(
                    text = stringResource(R.string.chat_status),
                    color = ChatOnline,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ChatAvatar(
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .brandButtonShadow(CircleShape)
            .clip(CircleShape)
            .background(ProGradientBrush),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_aria_spark),
            contentDescription = stringResource(R.string.chat_title),
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    bubbleMaxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    when (message.role) {
        ChatRole.Assistant -> AssistantBubble(
            text = message.text,
            bubbleMaxWidth = bubbleMaxWidth,
            modifier = modifier,
        )
        ChatRole.User -> UserBubble(
            text = message.text,
            bubbleMaxWidth = bubbleMaxWidth,
            modifier = modifier,
        )
    }
}

@Composable
private fun AssistantBubble(
    text: String,
    bubbleMaxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        ChatAvatar(size = 32.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .widthIn(max = bubbleMaxWidth)
                .clip(RoundedCornerShape(18.dp))
                .background(ChatAiBubble)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = parseBoldMarkdown(text, boldColor = HomeOnBackground),
                color = HomeOnBackground,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun UserBubble(
    text: String,
    bubbleMaxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = bubbleMaxWidth)
                .clip(RoundedCornerShape(18.dp))
                .background(ProGradientBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = text,
                color = white,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun TypingIndicatorRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(size = 32.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(ChatAiBubble)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            TypingDots()
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = index * 120),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_$index",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ProGradientStart.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun ChatSuggestionRow(
    suggestions: List<ChatSuggestion>,
    selectedId: String?,
    onSuggestionClick: (ChatSuggestion) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(suggestions, key = { it.id }) { suggestion ->
            ChatSuggestionChip(
                label = suggestion.label,
                selected = suggestion.id == selectedId,
                onClick = { onSuggestionClick(suggestion) },
            )
        }
    }
}

@Composable
private fun ChatSuggestionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (selected) Modifier.chipButtonShadow(shape) else Modifier)
            .then(
                if (selected) {
                    Modifier.background(ProGradientBrush)
                } else {
                    Modifier
                        .background(ChipUnselected)
                        .border(1.dp, GeneratorInputBorder, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) white else HomeOnBackground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    canSend: Boolean,
    isListening: Boolean,
    onTextChange: (String) -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(GeneratorInputBackground)
                .border(1.dp, GeneratorInputBorder, RoundedCornerShape(24.dp))
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCameraClick) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = SplashMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                textStyle = TextStyle(
                fontFamily = PlusJakartaSansFamily,
                
                    color = HomeOnBackground,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(ProGradientStart),
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.chat_input_hint),
                                color = ChatInputHint,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (isListening) {
                            Modifier.background(ProGradientBrush)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = if (isListening) "Listening" else "Voice",
                    tint = if (isListening) white else ProGradientStart,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .brandButtonShadow(CircleShape)
                .clip(CircleShape)
                .then(
                    if (canSend) {
                        Modifier.background(ProGradientBrush)
                    } else {
                        Modifier.background(ProGradientStart.copy(alpha = 0.45f))
                    },
                )
                .clickable(enabled = canSend, onClick = onSendClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = white,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun parseBoldMarkdown(text: String, boldColor: Color): AnnotatedString {
    if (!text.contains("**")) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var index = 0
        var bold = false
        while (index < text.length) {
            if (text.startsWith("**", index)) {
                bold = !bold
                index += 2
                continue
            }
            val nextBold = text.indexOf("**", index).takeIf { it >= 0 } ?: text.length
            val segment = text.substring(index, nextBold)
            if (bold) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(segment)
                }
            } else {
                append(segment)
            }
            index = nextBold
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F5FA)
@Composable
private fun AiChatbotScreenPreview() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        AiChatbotScreen(onBack = {})
    }
}
