package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiartgenerator.imagegenerator.videogenerator.R

enum class SubscriptionPlanId {
    Monthly,
    Weekly,
}

data class PremiumFeature(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

data class PremiumCarouselSlide(
    @DrawableRes val imageRes: Int,
    @StringRes val labelRes: Int,
)

data class SubscriptionPlan(
    val id: SubscriptionPlanId,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int? = null,
    val priceText: String,
    @StringRes val pricePeriodRes: Int,
    val isBestChoice: Boolean = false,
    val saveBadgeRes: Int? = null,
)

object PremiumCatalog {
    val carouselSlides = listOf(
        PremiumCarouselSlide(R.drawable.slider_1, R.string.premium_slide_image),
        PremiumCarouselSlide(R.drawable.slider_2, R.string.premium_slide_video),
        PremiumCarouselSlide(R.drawable.slider_3, R.string.premium_slide_music),
        PremiumCarouselSlide(R.drawable.slider_4, R.string.premium_slide_edit),
        PremiumCarouselSlide(R.drawable.slider_5, R.string.premium_slide_image),
    )

    val features = listOf(
        PremiumFeature(
            iconRes = R.drawable.ic_unlimited,
            titleRes = R.string.premium_feature_unlimited_title,
            subtitleRes = R.string.premium_feature_unlimited_subtitle,
        ),
        PremiumFeature(
            iconRes = R.drawable.ic_no_ads,
            titleRes = R.string.premium_feature_no_ads_title,
            subtitleRes = R.string.premium_feature_no_ads_subtitle,
        ),
        PremiumFeature(
            iconRes = R.drawable.ic_priority,
            titleRes = R.string.premium_feature_priority_title,
            subtitleRes = R.string.premium_feature_priority_subtitle,
        ),
        PremiumFeature(
            iconRes = R.drawable.ic_downloads,
            titleRes = R.string.premium_feature_downloads_title,
            subtitleRes = R.string.premium_feature_downloads_subtitle,
        ),
        PremiumFeature(
            iconRes = R.drawable.ic_unlock,
            titleRes = R.string.premium_feature_early_title,
            subtitleRes = R.string.premium_feature_early_subtitle,
        ),
    )

    val plans = listOf(
        SubscriptionPlan(
            id = SubscriptionPlanId.Monthly,
            titleRes = R.string.premium_plan_monthly,
            subtitleRes = R.string.premium_plan_monthly_subtitle,
            priceText = "",
            pricePeriodRes = R.string.premium_per_month,
            isBestChoice = true,
            saveBadgeRes = R.string.premium_save_badge,
        ),
        SubscriptionPlan(
            id = SubscriptionPlanId.Weekly,
            titleRes = R.string.premium_plan_weekly,
            subtitleRes = R.string.premium_plan_weekly_subtitle,
            priceText = "",
            pricePeriodRes = R.string.premium_per_week,
        ),
    )
}
