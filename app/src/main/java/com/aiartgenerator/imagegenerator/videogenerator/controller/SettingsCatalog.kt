package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.HelpOutline
// import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.SettingItem
import com.aiartgenerator.imagegenerator.videogenerator.model.SettingSection

object SettingsCatalog {
    fun sections(): List<SettingSection> = listOf(
        SettingSection(
            titleRes = R.string.settings_section_account,
            items = listOf(
                SettingItem("privacy", R.string.settings_privacy, Icons.Filled.Lock),
                SettingItem("terms", R.string.settings_terms, Icons.Outlined.Description),
                SettingItem("rate", R.string.settings_rate_app, Icons.Filled.Star),
                SettingItem("share", R.string.settings_share_app, Icons.Filled.Share),
            ),
        ),
        // SettingSection(
        //     titleRes = R.string.settings_section_help,
        //     items = listOf(
        //         // SettingItem("help_center", R.string.settings_help_center, Icons.AutoMirrored.Filled.HelpOutline),
        //         SettingItem("about", R.string.settings_about, Icons.Filled.Info),
        //     ),
        // ),
    )
}
