package com.aiartgenerator.imagegenerator.videogenerator.view.edit

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.controller.AiEditController
import com.aiartgenerator.imagegenerator.videogenerator.model.EditResultStore
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ContentMoreMenu
import com.aiartgenerator.imagegenerator.videogenerator.view.common.GeneratedImageCard
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ReportContentModal
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionButton
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResultActionStyle
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@Composable
fun AiEditResultScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload = EditResultStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { AiEditController() }
    var showReportModal by remember { mutableStateOf(false) }

    if (payload == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.ai_edit_result_empty), color = HomeMuted)
        }
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        AppTopBar(
            title = stringResource(R.string.ai_edit_result_title),
            onBack = onBack,
            showProAndSettings = false,
            trailingContent = {
                ContentMoreMenu(
                    isOverlay = false,
                    onReportClick = { showReportModal = true },
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = payload.toolLabel,
                color = HomeOnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.ai_edit_edited_with, payload.toolLabel),
                color = HomeMuted,
                fontSize = 14.sp,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GeneratorCardBackground)
                    .border(1.dp, GeneratorInputBorder, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GeneratedImageCard(
                    bytes = payload.resultBytes,
                    fileUri = payload.resultUri,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            Text(
                text = stringResource(R.string.ai_edit_before_after),
                color = HomeOnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EditCompareTile(
                    label = stringResource(R.string.ai_edit_original),
                    uri = payload.originalUri,
                    modifier = Modifier.weight(1f),
                )
                EditCompareTile(
                    label = stringResource(R.string.ai_edit_edited),
                    bytes = payload.resultBytes,
                    uri = payload.resultUri,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ResultActionButton(
                    label = stringResource(R.string.ai_edit_download),
                    icon = Icons.Filled.Download,
                    style = ResultActionStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            val saved = controller.downloadFromPayload(context, payload)
                            Toast.makeText(
                                context,
                                if (saved) {
                                    context.getString(R.string.ai_edit_saved_gallery)
                                } else {
                                    context.getString(R.string.ai_edit_download_failed)
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                )
                ResultActionButton(
                    label = stringResource(R.string.ai_edit_share),
                    icon = Icons.Filled.Share,
                    style = ResultActionStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        payload.resultUri?.let { uri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(R.string.ai_edit_share_chooser),
                                ),
                            )
                        }
                    },
                )
                ResultActionButton(
                    label = stringResource(R.string.ai_edit_edit_again),
                    icon = Icons.Filled.AutoFixHigh,
                    style = ResultActionStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onBack,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, GeneratorInputBorder, RoundedCornerShape(16.dp))
                    .background(GeneratorCardBackground)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_edit_prompt_label),
                    color = HomeMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = payload.toolPrompt,
                    color = HomeOnBackground,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }

    ReportContentModal(
        show = showReportModal,
        contentId = payload.resultUri?.toString().orEmpty(),
        contentType = ReportContentType.Image,
        prompt = payload.toolPrompt,
        mediaUri = payload.resultUri?.toString(),
        onDismiss = { showReportModal = false },
    )
}

@Composable
private fun EditCompareTile(
    label: String,
    modifier: Modifier = Modifier,
    uri: android.net.Uri? = null,
    bytes: ByteArray? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = label, color = HomeMuted, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .background(GeneratorCardBackground),
            contentAlignment = Alignment.Center,
        ) {
            GeneratedImageCard(
                bytes = bytes,
                fileUri = uri,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228)
@Composable
private fun AiEditResultScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        AiEditResultScreen(onBack = {})
    }
}
