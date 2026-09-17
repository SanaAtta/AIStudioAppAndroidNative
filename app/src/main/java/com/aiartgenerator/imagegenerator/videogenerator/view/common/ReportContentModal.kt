package com.aiartgenerator.imagegenerator.videogenerator.view.common

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ContentReport
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ContentReportRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportContentType
import com.aiartgenerator.imagegenerator.videogenerator.model.report.ReportReason
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ChipUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.GeneratorInputBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PlusJakartaSansFamily
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SuccessGreen = Color(0xFF10B981)
private val ErrorRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContentModal(
    show: Boolean,
    contentId: String,
    contentType: ReportContentType,
    prompt: String? = null,
    mediaUri: String? = null,
    onDismiss: () -> Unit,
    onReportSubmitted: (() -> Unit)? = null,
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var additionalDetails by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (isSubmitting) return

        // 1. Validate required fields
        if (selectedReason == null) {
            validationError = context.getString(R.string.report_validation_select_reason)
            errorMessage = null
            return
        }

        if (selectedReason == ReportReason.Other && additionalDetails.trim().isEmpty()) {
            validationError = context.getString(R.string.report_validation_provide_details)
            errorMessage = null
            return
        }

        validationError = null
        errorMessage = null
        isSubmitting = true

        scope.launch {
            val reason = selectedReason ?: return@launch
            val reasonText = context.getString(reason.titleRes)
            val report = ContentReport(
                contentId = contentId.ifBlank { "generated_${contentType.wireName}_${System.currentTimeMillis()}" },
                contentType = contentType,
                reason = reason,
                reasonText = reasonText,
                prompt = prompt,
                mediaUri = mediaUri,
                additionalInfo = additionalDetails.trim().takeIf { it.isNotEmpty() },
            )

            val result = ContentReportRepository.submitReport(context, report)
            isSubmitting = false

            if (result.isSuccess) {
                submitSuccess = true
                // Reset form fields
                selectedReason = null
                additionalDetails = ""
                validationError = null
                errorMessage = null

                Toast.makeText(
                    context,
                    context.getString(R.string.report_success),
                    Toast.LENGTH_LONG,
                ).show()

                onReportSubmitted?.invoke()
                delay(1500L)
                onDismiss()
            } else {
                // Keep the entered form data and display clear error message
                errorMessage = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.report_failed)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        sheetState = sheetState,
        containerColor = GeneratorCardBackground,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(HomeMuted.copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.report_content_title),
                    color = HomeOnBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.report_content_subtitle),
                    color = HomeMuted,
                    fontSize = 14.sp,
                )
            }

            if (submitSuccess) {
                // Success State View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(52.dp),
                    )
                    Text(
                        text = stringResource(R.string.report_success),
                        color = HomeOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // Reason selection list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ReportReason.entries.forEach { reason ->
                        val isSelected = selectedReason == reason
                        val shape = RoundedCornerShape(14.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(ChipUnselected)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            BorderStroke(1.5.dp, ProGradientStart),
                                            shape,
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            GeneratorInputBorder,
                                            shape,
                                        )
                                    },
                                )
                                .clickable(enabled = !isSubmitting) {
                                    selectedReason = reason
                                    validationError = null
                                    errorMessage = null
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(reason.titleRes),
                                color = HomeOnBackground,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Radio indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (isSelected) ProGradientStart else HomeMuted.copy(alpha = 0.6f),
                                        ),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(ProGradientStart),
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional / required additional details field if "Other" is selected
                AnimatedVisibility(visible = selectedReason == ReportReason.Other) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.report_additional_details),
                            color = HomeMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        BasicTextField(
                            value = additionalDetails,
                            onValueChange = {
                                additionalDetails = it
                                if (validationError != null) validationError = null
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GeneratorInputBackground)
                                .border(1.dp, GeneratorInputBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            textStyle = TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                color = HomeOnBackground,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(ProGradientStart),
                            maxLines = 3,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (additionalDetails.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.report_additional_details),
                                            color = HomeMuted.copy(alpha = 0.6f),
                                            fontSize = 14.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }
                }

                // Validation Error Banner
                AnimatedVisibility(visible = validationError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorRed.copy(alpha = 0.12f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = validationError.orEmpty(),
                            color = ErrorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Error State with Retry
                if (errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ErrorRed.copy(alpha = 0.12f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = errorMessage ?: stringResource(R.string.report_failed),
                            color = ErrorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Action Buttons: Cancel and Submit Report
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Cancel button
                    val cancelShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(cancelShape)
                            .background(GeneratorCardBackground)
                            .border(1.dp, GeneratorInputBorder, cancelShape)
                            .clickable(enabled = !isSubmitting, onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.report_cancel),
                            color = HomeOnBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Submit Report button
                    val submitShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(submitShape)
                            .then(if (!isSubmitting) Modifier.brandButtonShadow(submitShape) else Modifier)
                            .background(ProGradientBrush)
                            .clickable(enabled = !isSubmitting, onClick = ::submit),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSubmitting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = white,
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = stringResource(R.string.report_submitting),
                                    color = white,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else if (errorMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = white,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(R.string.report_retry),
                                    color = white,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.report_submit),
                                color = white,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
