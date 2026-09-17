package com.aiartgenerator.imagegenerator.videogenerator.view.settings

import com.aiartgenerator.imagegenerator.videogenerator.analytics.AppAnalytics
import com.aiartgenerator.imagegenerator.videogenerator.analytics.trackedClick

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.ads.AdsControl
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.controller.SettingsCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguageCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.LanguagePreferences
import com.aiartgenerator.imagegenerator.videogenerator.model.SettingItem
import com.aiartgenerator.imagegenerator.videogenerator.model.SettingSection
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppTopBar
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppThemeProvider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SettingsCardBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SettingsDivider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SettingsIconBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SettingsIconTint
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SettingsSectionLabel
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SettingsTrailingText
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.brandGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.SplashOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onFavouritesClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()
    val showUpgradeCard = !isPremium && AdsControl.showSettingsUpgradeCard()
    val currentLanguageName = LanguageCatalog.findById(
        LanguagePreferences.getSelectedLanguageId(context),
    )?.name ?: "English"
    val sections = SettingsCatalog.sections()

    ResponsiveScreenRoot(modifier = modifier) { metrics ->
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                showSettings = false,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = metrics.horizontalPadding),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (showUpgradeCard) {
                    UpgradeProBanner(onUpgradeClick = onUpgradeClick)
                    Spacer(modifier = Modifier.height(28.dp))
                }

                AppearanceSection(
                    currentLanguageName = currentLanguageName,
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onLanguageClick = onLanguageClick,
                    onFavouritesClick = onFavouritesClick,
                )

                Spacer(modifier = Modifier.height(20.dp))

                sections.forEach { section ->
                    SettingsSectionBlock(
                        section = section,
                        onItemClick = { itemId ->
                            when (itemId) {
                                "language" -> onLanguageClick()
                                "favourites" -> onFavouritesClick()
                                else -> onItemClick(itemId)
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    currentLanguageName: String,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onFavouritesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_section_appearance),
            color = SettingsSectionLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsCardBackground),
        ) {
            SettingsToggleRow(
                title = stringResource(R.string.settings_dark_mode),
                icon = Icons.Filled.DarkMode,
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange,
            )
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 62.dp),
            )
            SettingsRow(
                item = SettingItem(
                    id = "language",
                    titleRes = R.string.settings_language,
                    icon = Icons.Filled.Translate,
                    trailingText = currentLanguageName,
                ),
                onClick = onLanguageClick,
            )
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 62.dp),
            )
            SettingsRow(
                item = SettingItem(
                    id = "favourites",
                    titleRes = R.string.settings_favourites,
                    icon = Icons.Filled.FavoriteBorder,
                ),
                onClick = onFavouritesClick,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SettingsIconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SettingsIconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = title,
            color = SplashOnBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                AppAnalytics.click(
                    itemId = "settings_toggle",
                    extra = title + "_" + if (value) "on" else "off",
                )
                onCheckedChange(value)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = white,
                checkedTrackColor = ProGradientStart,
                uncheckedThumbColor = white,
                uncheckedTrackColor = SettingsTrailingText.copy(alpha = 0.35f),
            ),
        )
    }
}

@Composable
private fun GoPremiumCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackedUpgrade = trackedClick("settings_go_premium", onClick = onUpgradeClick)
    val cardShape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(cardShape)
            .clickable(onClick = trackedUpgrade),
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_settings_go_premium),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings_premium_crown),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                contentScale = ContentScale.Fit,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_go_premium),
                        color = white,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.settings_go_premium_pro),
                        color = ProGradientStart,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(white)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_go_premium_subtitle),
                    color = white.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 14.sp,
                    maxLines = 2,
                )
            }
            Row(
                modifier = Modifier
                    .offset(y = 22.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(white)
                    .clickable(onClick = trackedUpgrade)

                    .padding(start = 14.dp, end = 5.dp, top = 8.dp, bottom = 8.dp,),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_upgrade),
                    color = ProGradientStart,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ProGradientStart),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = white,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpgradeProBanner(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .brandGradientBackground(RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_premium_crown_badge),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_upgrade_pro),
                color = white,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_upgrade_subtitle),
                color = white.copy(alpha = 0.88f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        val upgradeButtonShape = RoundedCornerShape(50.dp)
        Box(
            modifier = Modifier
                .clip(upgradeButtonShape)
                .background(color = Color.White.copy(alpha = 0.26f))
                .border(
                    width = 0.dp,
                    color = white.copy(alpha = 0.26f),
                    shape = upgradeButtonShape,
                )
                .clickable(onClick = onUpgradeClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_upgrade),
                color = white,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SettingsSectionBlock(
    section: SettingSection,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(section.titleRes),
            color = SettingsSectionLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsCardBackground),
        ) {
            section.items.forEachIndexed { index, item ->
                SettingsRow(
                    item = item,
                    onClick = { onItemClick(item.id) },
                )
                if (index < section.items.lastIndex) {
                    HorizontalDivider(
                        color = SettingsDivider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 62.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    item: SettingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(item.titleRes)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = trackedClick(itemId = "settings_row", extra = item.id, onClick = onClick))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SettingsIconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = title,
                tint = SettingsIconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = title,
            color = SplashOnBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        if (item.trailingText != null) {
            Text(
                text = item.trailingText,
                color = SettingsTrailingText,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SettingsTrailingText,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MyApplicationTheme(dynamicColor = false) {
        AppThemeProvider(isDarkTheme = true) {
            SettingsScreen(
                onBack = {},
                onUpgradeClick = {},
                onLanguageClick = {},
            )
        }
    }
}
