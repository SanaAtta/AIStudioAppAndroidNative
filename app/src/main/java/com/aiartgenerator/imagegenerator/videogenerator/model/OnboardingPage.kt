package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiartgenerator.imagegenerator.videogenerator.R

data class OnboardingPage(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val buttonRes: Int,
)

object OnboardingCatalog {
    val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.onboard1,
            titleRes = R.string.onboarding_title_1,
            descriptionRes = R.string.onboarding_desc_1,
            buttonRes = R.string.onboarding_button_1,
        ),
        OnboardingPage(
            imageRes = R.drawable.onboard2,
            titleRes = R.string.onboarding_title_2,
            descriptionRes = R.string.onboarding_desc_2,
            buttonRes = R.string.onboarding_button_2,
        ),
        OnboardingPage(
            imageRes = R.drawable.onboard3,
            titleRes = R.string.onboarding_title_3,
            descriptionRes = R.string.onboarding_desc_3,
            buttonRes = R.string.onboarding_button_3,
        ),
    )
}
