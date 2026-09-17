package com.aiartgenerator.imagegenerator.videogenerator.view.edit

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiEditController
import com.aiartgenerator.imagegenerator.videogenerator.model.AiEditLaunchMode
import com.aiartgenerator.imagegenerator.videogenerator.model.EditResultStore
import com.aiartgenerator.imagegenerator.videogenerator.model.EditTool
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GenerateActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberFreeGenerationGate
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.common.MainTabTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChatInputHint
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ToolTileBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ToolTileText
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.chipButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PlusJakartaSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun AiEditScreen(
    onBack: (() -> Unit)? = null,
    onResult: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val controller = remember { AiEditController() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editGate = rememberFreeGenerationGate(FreeUsageKind.Edit)
    val lifecycleOwner = LocalLifecycleOwner.current
    val launchEpoch by EditResultStore.editorLaunchEpoch.collectAsState()
    var hideUploadCard by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        hideUploadCard = false
        controller.onImageSelected(uri)
    }

    // Apply Generated Image → AI Edit or Direct entry when launch is prepared.
    LaunchedEffect(launchEpoch) {
        val launch = EditResultStore.consumeEditorLaunch() ?: return@LaunchedEffect
        when (launch.mode) {
            AiEditLaunchMode.FromGenerated -> {
                hideUploadCard = launch.imageUri != null
                controller.onImageSelected(launch.imageUri)
            }
            AiEditLaunchMode.Direct -> {
                hideUploadCard = false
                controller.onImageSelected(null)
            }
        }
    }

    // Returning from AI Edit result (Activity may stay resumed — apply on compose).
    LaunchedEffect(Unit) {
        EditResultStore.consumeEditedImageForEditor()?.let { editedUri ->
            hideUploadCard = true
            controller.onImageSelected(editedUri)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                EditResultStore.consumeEditorLaunch()?.let { launch ->
                    when (launch.mode) {
                        AiEditLaunchMode.FromGenerated -> {
                            hideUploadCard = launch.imageUri != null
                            controller.onImageSelected(launch.imageUri)
                        }
                        AiEditLaunchMode.Direct -> {
                            hideUploadCard = false
                            controller.onImageSelected(null)
                        }
                    }
                }
                EditResultStore.consumeEditedImageForEditor()?.let { editedUri ->
                    hideUploadCard = true
                    controller.onImageSelected(editedUri)
                }
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
            if (onBack != null) {
                AppTopBar(
                    title = stringResource(R.string.ai_edit_title),
                    onBack = onBack,
                    showProAndSettings = false,
                )
            } else {
                MainTabTopBar(
                    title = stringResource(R.string.ai_edit_title),
                    horizontalPadding = metrics.horizontalPadding,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = metrics.horizontalPadding)
                    .padding(top = if (onBack == null) 0.dp else 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            EditSearchBar(
                query = controller.searchQuery,
                onQueryChange = controller::onSearchChange,
            )

            val sourceUri = controller.selectedImageUri
            if (hideUploadCard && sourceUri != null) {
                SourceImagePreview(
                    imageUri = sourceUri,
                )
            } else {
                UploadImageCard(
                    imageUri = controller.selectedImageUri,
                    onUploadClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                )
            }

            val filteredTools = controller.filteredTools(context)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ai_edit_tools_title),
                    color = HomeOnBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.ai_edit_tools_count, filteredTools.size),
                    color = HomeMuted,
                    fontSize = 13.sp,
                )
            }

            EditToolsGrid(
                tools = filteredTools,
                selectedToolId = controller.selectedToolId,
                onToolSelected = controller::onToolSelected,
            )

            if (!controller.isLoading && controller.hasImage && controller.selectedTool == null) {
                Text(
                    text = stringResource(R.string.ai_edit_select_tool),
                    color = HomeMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        val selectedToolLabel = controller.selectedTool?.let { stringResource(it.labelRes) }
        GenerateActionButton(
            label = selectedToolLabel?.let {
                stringResource(R.string.ai_edit_apply_tool, it)
            } ?: stringResource(R.string.ai_edit_apply),
            icon = Icons.Filled.AutoFixHigh,
            enabled = controller.canApply || editGate.locked,
            isLoading = controller.isLoading,
            loadingLabel = stringResource(R.string.ai_edit_editing),
            locked = editGate.locked,
            onLockedClick = editGate.onLockedClick,
            onClick = {
                scope.launch {
                    controller.onApply(context, onSuccess = onResult)
                }
            },
            modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = 16.dp),
        )
        }
    }
}

@Composable
private fun EditSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(GeneratorInputBackground)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.ai_edit_search),
            tint = ChatInputHint,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = PlusJakartaSansFamily,
                color = HomeOnBackground, fontSize = 15.sp),
            cursorBrush = SolidColor(ProGradientStart),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.ai_edit_search_hint),
                            color = ChatInputHint,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun SourceImagePreview(
    imageUri: Uri,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(shape)
            .background(GeneratorCardBackground),
        contentAlignment = Alignment.Center,
    ) {
        GalleryImagePreview(
            uri = imageUri,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun UploadImageCard(
    imageUri: Uri?,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(shape)
            .background(GeneratorCardBackground)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                drawRoundRect(
                    color = ProGradientStart.copy(alpha = 0.45f),
                    size = size,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = dashEffect),
                )
            }
            .clickable(onClick = onUploadClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUri != null) {
            GalleryImagePreview(
                uri = imageUri,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_upload_camera),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    text = stringResource(R.string.ai_edit_upload_title),
                    color = ProGradientStart,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.ai_edit_upload_hint),
                    color = ChatInputHint,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EditToolsGrid(
    tools: List<EditTool>,
    selectedToolId: String?,
    onToolSelected: (EditTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        tools.chunked(4).forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowTools.forEach { tool ->
                    EditToolTile(
                        tool = tool,
                        selected = tool.id == selectedToolId,
                        onClick = { onToolSelected(tool) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowTools.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EditToolTile(
    tool: EditTool,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tool.labelRes)
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .then(if (selected) Modifier.chipButtonShadow(cardShape) else Modifier)
            .clip(cardShape)
            .then(
                if (selected) {
                    Modifier
                        .background(ProGradientStart)
                        .padding(1.5.dp)
                        .clip(cardShape)
                        .background(ToolTileBackground)
                        .border(1.dp, ProGradientStart.copy(alpha = 0.45f), cardShape)
                } else {
                    Modifier
                        .background(ToolTileBackground)
                        .border(1.dp, GeneratorInputBorder, cardShape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = tool.iconRes),
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = label,
            color = if (selected) ProGradientStart else ToolTileText.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            modifier = Modifier.padding(bottom = 2.dp),
        )
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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when (val bitmap = imageBitmap) {
            null -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = ProGradientStart,
                strokeWidth = 2.dp,
            )
            else -> Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.ai_edit_selected_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228)
@Composable
private fun AiEditScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiEditScreen(onBack = {})
    }
}
