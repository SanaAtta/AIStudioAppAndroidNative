package com.example.myapplication.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.theme.ChatCardEnd
import com.example.myapplication.theme.ChatCardStart
import com.example.myapplication.theme.HomeBackground
import com.example.myapplication.theme.HomeOnBackground
import com.example.myapplication.theme.HomeSeeAll
import com.example.myapplication.theme.ImageCardEnd
import com.example.myapplication.theme.ImageCardStart
import com.example.myapplication.theme.MusicCardEnd
import com.example.myapplication.theme.MusicCardStart
import com.example.myapplication.theme.VideoCardEnd
import com.example.myapplication.theme.VideoCardStart

@Composable
fun HomeScreen(
    onImageGeneratorClick: () -> Unit,
    onVideoGeneratorClick: () -> Unit,
    onMusicGeneratorClick: () -> Unit,
    onChatbotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "AI Content Studio",
                color = HomeOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = HomeOnBackground,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeatureCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                gradient = Brush.linearGradient(listOf(ImageCardStart, ImageCardEnd)),
                onClick = onImageGeneratorClick,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "AI Image Generator",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Turn words into stunning images",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                        )
                    }
                    Box(modifier = Modifier.width(120.dp).height(100.dp)) {
                        PreviewTile(
                            color = Color(0xFF7E57C2),
                            modifier = Modifier.align(Alignment.CenterStart).offset(y = 8.dp).rotate(-8f),
                        )
                        PreviewTile(
                            color = Color(0xFF26A69A),
                            modifier = Modifier.align(Alignment.Center).offset(x = 8.dp, y = (-4).dp).rotate(4f),
                        )
                        PreviewTile(
                            color = Color(0xFFEF5350),
                            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-4).dp, y = 10.dp).rotate(10f),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f).height(180.dp),
                    gradient = Brush.verticalGradient(listOf(VideoCardStart, VideoCardEnd)),
                    onClick = onVideoGeneratorClick,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                    ) {
                        Text(
                            text = "AI Video Generator",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create videos from your ideas",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 52.dp, height = 40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                FeatureCard(
                    modifier = Modifier.weight(1f).height(180.dp),
                    gradient = Brush.verticalGradient(listOf(MusicCardStart, MusicCardEnd)),
                    onClick = onMusicGeneratorClick,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                    ) {
                        Text(
                            text = "AI Music Generator",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate music from prompts",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                        )
                    }
                }
            }
            FeatureCard(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                gradient = Brush.horizontalGradient(listOf(ChatCardStart, ChatCardEnd)),
                onClick = onChatbotClick,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "AI Chatbot",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chat smarter, get instant answers",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                        )
                    }
                    Column(
                        modifier = Modifier.width(110.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4A9EFF)),
                        )
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.9f)),
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4A9EFF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubble,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent",
                    color = HomeOnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "See all",
                    color = HomeSeeAll,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(0xFF5D4037, 0xFF37474F, 0xFF455A64).forEach { color ->
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(color)),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color),
    )
}

@Composable
private fun FeatureCard(
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(onClick = onClick),
    ) {
        content()
    }
}
