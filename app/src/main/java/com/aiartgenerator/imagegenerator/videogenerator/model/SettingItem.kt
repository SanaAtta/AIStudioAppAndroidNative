package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class SettingItem(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val trailingText: String? = null,
)

data class SettingSection(
    @StringRes val titleRes: Int,
    val items: List<SettingItem>,
)
