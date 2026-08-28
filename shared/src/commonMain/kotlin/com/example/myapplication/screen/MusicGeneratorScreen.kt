package com.example.myapplication.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.network.AiApi
import com.example.myapplication.safety.PromptSafety
import com.example.myapplication.theme.DisabledButton
import com.example.myapplication.theme.HomeBackground
import com.example.myapplication.theme.HomeNavSelected
import com.example.myapplication.theme.HomeOnBackground
import com.example.myapplication.theme.Hint
import com.example.myapplication.theme.ScreenSurface
import com.example.myapplication.ui.BackTopBar
import kotlinx.coroutines.launch

@Composable
fun MusicGeneratorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val api = remember { AiApi() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        BackTopBar(title = "AI Music Generator", onBack = onBack)
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Describe your music",
                color = HomeOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Lo-fi chill beat with soft piano...", color = Hint) },
                minLines = 3,
            )
            Button(
                onClick = {
                    if (PromptSafety.containsUnsafeContent(prompt)) {
                        errorMessage = PromptSafety.UNSAFE_MESSAGE
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        resultText = null
                        api.generateMusic(prompt.trim())
                            .onSuccess {
                                resultText = "Audio ready"
                            }
                            .onFailure { errorMessage = it.message ?: "Generation failed" }
                        isLoading = false
                    }
                },
                enabled = prompt.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeNavSelected,
                    disabledContainerColor = DisabledButton,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = if (isLoading) "Generating..." else "Generate",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScreenSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = HomeNavSelected)
                }
            }
            errorMessage?.let { message ->
                Text(text = message, color = Color(0xFFFF6B6B), fontSize = 14.sp)
            }
            resultText?.let { text ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScreenSurface)
                        .padding(20.dp),
                ) {
                    Text(text = text, color = HomeOnBackground, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
