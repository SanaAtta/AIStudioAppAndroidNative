package com.aiartgenerator.imagegenerator.videogenerator.model.report

import android.os.Build
import androidx.annotation.StringRes
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig
import com.aiartgenerator.imagegenerator.videogenerator.R
import java.util.UUID

enum class ReportContentType(val wireName: String) {
    Image("image"),
    Video("video"),
    Audio("audio"),
}

enum class ReportReason(
    val key: String,
    @param:StringRes val titleRes: Int,
) {
    Offensive("offensive_or_abusive", R.string.report_reason_offensive),
    Sexual("sexual_or_inappropriate", R.string.report_reason_sexual),
    Hate("hate_or_discriminatory", R.string.report_reason_hate),
    Violent("violent_or_graphic", R.string.report_reason_violent),
    Other("other", R.string.report_reason_other),
}

data class ContentReport(
    val id: String = UUID.randomUUID().toString(),
    val contentId: String,
    val contentType: ReportContentType,
    val reason: ReportReason,
    val reasonText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val prompt: String? = null,
    val mediaUri: String? = null,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val additionalInfo: String? = null,
)
