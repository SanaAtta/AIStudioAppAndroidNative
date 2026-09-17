package com.aiartgenerator.imagegenerator.videogenerator.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.BillingProductIds
import com.aiartgenerator.imagegenerator.videogenerator.model.PremiumCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.SubscriptionPlan
import com.aiartgenerator.imagegenerator.videogenerator.model.SubscriptionPlanId
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class SubscriptionBillingManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activity: Activity? = null

    var isReady: Boolean = false
        private set
    var isPurchasing: Boolean = false
        private set
    var displayPlans: List<SubscriptionPlan> = PremiumCatalog.plans
        private set

    private val productDetailsByPlan = mutableMapOf<SubscriptionPlanId, ProductDetails>()
    private var pendingSuccess: (() -> Unit)? = null
    private var pendingFailure: ((String) -> Unit)? = null
    private var connecting = false
    private val onReadyQueue = mutableListOf<() -> Unit>()
    /** Play can briefly return an empty purchase list right after a successful buy. */
    private var suppressDemoteUntilMs: Long = 0L
    private var postPurchaseResync: Runnable? = null

    companion object {
        private const val POST_PURCHASE_DEMOTE_GRACE_MS = 5_000L
        private const val POST_PURCHASE_RESYNC_MS = 2_000L
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val owned = purchases.orEmpty().filter(::isActiveConfiguredSubscription)
                if (owned.isEmpty()) {
                    val wasPurchasing = isPurchasing
                    isPurchasing = false
                    syncActiveSubscriptions()
                    if (wasPurchasing) {
                        pendingFailure?.invoke(appContext.getString(R.string.premium_billing_failed))
                        clearPendingCallbacks()
                    }
                    return@PurchasesUpdatedListener
                }
                owned.forEach(::acknowledgeIfNeeded)
                completePurchase()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                isPurchasing = false
                pendingFailure?.invoke(appContext.getString(R.string.premium_billing_cancelled))
                clearPendingCallbacks()
            }
            else -> {
                isPurchasing = false
                pendingFailure?.invoke(
                    billingResult.debugMessage.ifBlank {
                        appContext.getString(R.string.premium_billing_failed)
                    },
                )
                clearPendingCallbacks()
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun attachActivity(activity: Activity) {
        this.activity = activity
    }

    fun detachActivity(activity: Activity? = null) {
        if (activity == null || this.activity === activity) {
            this.activity = null
        }
    }

    fun start(onReady: (() -> Unit)? = null) {
        if (!BillingProductIds.isConfigured) {
            displayPlans = PremiumCatalog.plans
            isReady = true
            onReady?.invoke()
            return
        }

        if (onReady != null) {
            synchronized(onReadyQueue) { onReadyQueue.add(onReady) }
        }

        if (billingClient.isReady) {
            isReady = true
            queryProducts { drainOnReady() }
            return
        }

        if (connecting) return
        connecting = true

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isReady = true
                    queryProducts { drainOnReady() }
                } else {
                    displayPlans = PremiumCatalog.plans
                    isReady = true
                    drainOnReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                isReady = false
            }
        })
    }

    fun refreshPurchases(onDone: (() -> Unit)? = null) {
        if (!BillingProductIds.isConfigured) {
            onDone?.invoke()
            return
        }
        if (!billingClient.isReady) {
            start(onDone)
            return
        }
        syncActiveSubscriptions(onDone)
    }

    fun restorePurchases(onDone: ((Boolean) -> Unit)? = null) {
        refreshPurchases {
            onDone?.invoke(PremiumAccess.isPremiumUser())
        }
    }

    fun endConnection() {
        postPurchaseResync?.let(mainHandler::removeCallbacks)
        postPurchaseResync = null
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        connecting = false
        isReady = false
        isPurchasing = false
        clearPendingCallbacks()
    }

    fun purchase(
        planId: SubscriptionPlanId,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        if (isPurchasing) return

        if (!BillingProductIds.isConfigured) {
            Toast.makeText(
                appContext,
                appContext.getString(R.string.premium_billing_not_configured),
                Toast.LENGTH_SHORT,
            ).show()
            onFailure(appContext.getString(R.string.premium_billing_not_configured))
            return
        }

        val hostActivity = activity
        if (hostActivity == null) {
            onFailure(appContext.getString(R.string.premium_billing_failed))
            return
        }

        val productDetails = productDetailsByPlan[planId]
        if (productDetails == null) {
            onFailure(appContext.getString(R.string.premium_billing_product_missing))
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
        if (offerToken.isNullOrBlank()) {
            onFailure(appContext.getString(R.string.premium_billing_product_missing))
            return
        }

        pendingSuccess = onSuccess
        pendingFailure = onFailure
        isPurchasing = true

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        val launchResult = billingClient.launchBillingFlow(hostActivity, params)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            isPurchasing = false
            onFailure(
                launchResult.debugMessage.ifBlank {
                    appContext.getString(R.string.premium_billing_failed)
                },
            )
            clearPendingCallbacks()
        }
    }

    private fun drainOnReady() {
        val listeners = synchronized(onReadyQueue) {
            val copy = onReadyQueue.toList()
            onReadyQueue.clear()
            copy
        }
        listeners.forEach { it.invoke() }
    }

    private fun queryProducts(onReady: (() -> Unit)? = null) {
        val products = BillingProductIds.configuredIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build(),
            ProductDetailsResponseListener { billingResult, queryProductDetailsResult ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    displayPlans = PremiumCatalog.plans
                    syncActiveSubscriptions(onReady)
                    return@ProductDetailsResponseListener
                }

                queryProductDetailsResult.productDetailsList.forEach { details ->
                    val planId = BillingProductIds.planIdForProduct(details.productId)
                        ?: return@forEach
                    productDetailsByPlan[planId] = details
                }

                displayPlans = PremiumCatalog.plans.map { dummyPlan ->
                    productDetailsByPlan[dummyPlan.id]?.toDisplayPlan(dummyPlan) ?: dummyPlan
                }
                syncActiveSubscriptions(onReady)
            },
        )
    }

    private fun isActiveConfiguredSubscription(purchase: Purchase): Boolean {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return false
        val knownIds = BillingProductIds.configuredIds.toSet()
        return purchase.products.any { it in knownIds }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgeParams) { }
    }

    private fun completePurchase() {
        isPurchasing = false
        PremiumAccess.applySubscriptionActive(true)
        pendingSuccess?.invoke()
        clearPendingCallbacks()
        // Keep premium through the brief window where queryPurchasesAsync may lag.
        suppressDemoteUntilMs = System.currentTimeMillis() + POST_PURCHASE_DEMOTE_GRACE_MS
        syncActiveSubscriptions()
        schedulePostPurchaseResync()
    }

    private fun schedulePostPurchaseResync() {
        postPurchaseResync?.let(mainHandler::removeCallbacks)
        val resync = Runnable { syncActiveSubscriptions() }
        postPurchaseResync = resync
        mainHandler.postDelayed(resync, POST_PURCHASE_RESYNC_MS)
    }

    private fun clearPendingCallbacks() {
        pendingSuccess = null
        pendingFailure = null
    }

    private fun syncActiveSubscriptions(onDone: (() -> Unit)? = null) {
        if (!billingClient.isReady) {
            onDone?.invoke()
            return
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.filter(::isActiveConfiguredSubscription)
                owned.forEach(::acknowledgeIfNeeded)
                when {
                    owned.isNotEmpty() -> {
                        suppressDemoteUntilMs = 0L
                        PremiumAccess.applySubscriptionActive(true)
                    }
                    System.currentTimeMillis() < suppressDemoteUntilMs -> {
                        // Do not demote while Play purchase list is still settling.
                    }
                    else -> PremiumAccess.applySubscriptionActive(false)
                }
            }
            onDone?.invoke()
        }
    }

    private fun ProductDetails.toDisplayPlan(fallback: SubscriptionPlan): SubscriptionPlan {
        val pricing = subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull { it.priceAmountMicros > 0 }
            ?: subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()

        val periodRes = when (pricing?.billingPeriod) {
            "P1W" -> R.string.premium_per_week
            "P1M" -> R.string.premium_per_month
            else -> fallback.pricePeriodRes
        }

        return fallback.copy(
            priceText = pricing?.formattedPrice?.takeIf { it.isNotBlank() } ?: fallback.priceText,
            pricePeriodRes = periodRes,
        )
    }
}
