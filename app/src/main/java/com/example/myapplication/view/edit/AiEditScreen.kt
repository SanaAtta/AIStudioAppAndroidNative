package com.example.myapplication.view.edit

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.controller.AiEditController
import com.example.myapplication.view.common.GeneratedImageCard
import com.example.myapplication.view.common.GenerationLoadingCard
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeMuted
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import com.example.myapplication.view.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EditSurface = Color(0xFF1C1C1E)
private val EditSurfaceAlt = Color(0xFF2C2C2E)
private val EditHint = Color(0xFF8E8E93)
private val EditDisabledButton = Color(0xFF3A3A3C)

@Composable
fun AiEditScreen(modifier: Modifier = Modifier) {
    val controller = remember { AiEditController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        controller.onImageSelected(uri)
    }

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
        Text(
            text = "AI Edit",
            color = HomeOnBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ReferenceImageSection(
                imageUri = controller.selectedImageUri,
                onUploadClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
            )
            EditPromptSection(
                text = controller.editText,
                maxChars = controller.maxChars,
                onTextChange = controller::onEditTextChange,
                onSuggestClick = controller::onSuggest,
            )
            StyleChipsRow(
                styles = controller.styles,
                selectedStyle = controller.selectedStyle,
                onStyleSelected = controller::onStyleSelected,
            )
            GenerateButton(
                enabled = controller.canGenerate,
                isLoading = controller.isLoading,
                onClick = { scope.launch { controller.onGenerate(context) } },
            )
            if (controller.isLoading) {
                GenerationLoadingCard(label = "Applying edit…")
            }
            if (controller.resultBytes != null || controller.resultUri != null) {
                GeneratedImageCard(
                    bytes = controller.resultBytes,
                    fileUri = controller.resultUri,
                )
            }
        }
    }
}

@Composable
private fun ReferenceImageSection(
    imageUri: Uri?,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Reference Image",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(EditSurface)
                .clickable(onClick = onUploadClick),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri != null) {
                GalleryImagePreview(
                    uri = imageUri,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        tint = HomeOnBackground,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap to Upload Image",
                        color = HomeMuted,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryImagePreview(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var imageBitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        imageBitmap = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }
    }

    when (val bitmap = imageBitmap) {
        null -> CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = HomeNavSelected,
            strokeWidth = 2.dp,
        )
        else -> Image(
            bitmap = bitmap,
            contentDescription = "Selected reference image",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun EditPromptSection(
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
            text = "Edit",
            color = HomeOnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(EditSurface)
                .padding(16.dp),
        ) {
            Text(
                text = "Describe Your Edit",
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
                    .height(72.dp),
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
                                text = "Example: Make it more dramatic.",
                                color = EditHint,
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
                        .background(EditSurfaceAlt)
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
                    color = EditHint,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun StyleChipsRow(
    styles: List<String>,
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        styles.forEach { style ->
            val selected = style == selectedStyle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(EditSurface)
                    .then(
                        if (selected) {
                            Modifier.border(
                                BorderStroke(1.5.dp, HomeNavSelected),
                                RoundedCornerShape(22.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onStyleSelected(style) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = style,
                    color = HomeOnBackground,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun GenerateButton(
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
            disabledContainerColor = EditDisabledButton,
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
            text = if (isLoading) "Generating…" else "Generate",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AiEditScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiEditScreen()
    }
}
