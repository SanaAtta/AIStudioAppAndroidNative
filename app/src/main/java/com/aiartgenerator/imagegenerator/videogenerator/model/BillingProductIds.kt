package com.aiartgenerator.imagegenerator.videogenerator.model

object BillingProductIds {
    /** Google Play subscription product id for the weekly plan. */
    const val WEEKLY = "weekly_subscription"

    /** Google Play subscription product id for the monthly plan. */
    const val MONTHLY = "monthly_subscription"

    val configuredIds: List<String>
        get() = listOf(WEEKLY, MONTHLY).filter { it.isNotBlank() }

    val isConfigured: Boolean
        get() = configuredIds.isNotEmpty()

    fun idForPlan(planId: SubscriptionPlanId): String = when (planId) {
        SubscriptionPlanId.Weekly -> WEEKLY
        SubscriptionPlanId.Monthly -> MONTHLY
    }

    fun planIdForProduct(productId: String): SubscriptionPlanId? = when (productId) {
        WEEKLY -> SubscriptionPlanId.Weekly
        MONTHLY -> SubscriptionPlanId.Monthly
        else -> null
    }
}
