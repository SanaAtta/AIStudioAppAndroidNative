package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.billing.SubscriptionSync
import com.aiartgenerator.imagegenerator.videogenerator.model.PremiumCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.SubscriptionPlanId

class PremiumController(
    appContext: Context,
) {
    private val appContext = appContext.applicationContext
    private val billingManager = SubscriptionSync.manager(this.appContext)

    var selectedPlan by mutableStateOf(SubscriptionPlanId.Monthly)
        private set
    var displayPlans by mutableStateOf(PremiumCatalog.plans)
        private set
    var isBillingReady by mutableStateOf(false)
        private set
    var isPurchasing by mutableStateOf(false)
        private set
    var isRestoring by mutableStateOf(false)
        private set

    fun bindActivity(activity: ComponentActivity) {
        billingManager.attachActivity(activity)
    }

    fun unbindActivity() {
        // MainActivity owns the BillingClient host; do not detach on paywall dispose.
    }

    fun startBilling() {
        billingManager.start {
            syncFromBilling()
        }
    }

    fun stopBilling() {
        // Keep the process BillingClient connected so launch/resume/restore stay in sync.
        syncFromBilling()
    }

    fun onPlanSelected(planId: SubscriptionPlanId) {
        selectedPlan = planId
    }

    fun subscribe(onSuccess: () -> Unit) {
        billingManager.purchase(
            planId = selectedPlan,
            onSuccess = {
                syncFromBilling()
                onSuccess()
            },
            onFailure = { message ->
                syncFromBilling()
                if (message.isNotBlank()) {
                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
                }
            },
        )
        syncFromBilling()
    }

    fun restorePurchases(onResult: (Boolean) -> Unit = {}) {
        if (isRestoring || isPurchasing) return
        isRestoring = true
        billingManager.restorePurchases { active ->
            isRestoring = false
            syncFromBilling()
            onResult(active)
        }
    }

    val canSubscribe: Boolean
        get() = isBillingReady && !isPurchasing && !isRestoring

    private fun syncFromBilling() {
        displayPlans = billingManager.displayPlans
        isBillingReady = billingManager.isReady
        isPurchasing = billingManager.isPurchasing
    }
}
