package com.example.myapplication.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.network.AiApi
import com.example.myapplication.theme.HomeBackground
import com.example.myapplication.theme.HomeMuted
import com.example.myapplication.theme.HomeNavSelected
import com.example.myapplication.theme.HomeOnBackground
import com.example.myapplication.theme.Hint
import com.example.myapplication.theme.ScreenSurface
import com.example.myapplication.ui.BackTopBar
import kotlinx.coroutines.launch

private data class ChatMessage(
    val role: String,
    val content: String,
)

@Composable
fun ChatbotScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val api = remember { AiApi() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        BackTopBar(title = "AI Chatbot", onBack = onBack)
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        if (messages.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = HomeMuted,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "Ask me anything",
                        color = HomeMuted,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = HomeNavSelected,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScreenSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF2C2C2E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(color = HomeOnBackground, fontSize = 15.sp),
                cursorBrush = SolidColor(HomeNavSelected),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) {
                            Text("Type a message...", color = Hint, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isEmpty() || isLoading) return@IconButton
                    input = ""
                    messages.add(ChatMessage("user", text))
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        val history = messages.map { it.role to it.content }
                        api.chat(history)
                            .onSuccess { reply ->
                                messages.add(ChatMessage("assistant", reply))
                            }
                            .onFailure { errorMessage = it.message ?: "Chat failed" }
                        isLoading = false
                    }
                },
                enabled = input.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (input.isNotBlank() && !isLoading) HomeNavSelected
                        else HomeNavSelected.copy(alpha = 0.45f),
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) Color(0xFF1A3A6B) else Color(0xFF2C2C2E))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                color = HomeOnBackground,
                fontSize = 15.sp,
                fontWeight = if (isUser) FontWeight.Normal else FontWeight.Normal,
            )
        }
    }
}
