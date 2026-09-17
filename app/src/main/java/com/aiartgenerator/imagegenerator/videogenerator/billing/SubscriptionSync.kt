package com.aiartgenerator.imagegenerator.videogenerator.billing

import android.app.Activity
import android.content.Context

/**
 * Process-wide Play Billing client. Subscription status is synced from the store on
 * launch, resume, purchase callbacks, and restore — not from a local flag alone.
 */
object SubscriptionSync {
    private val lock = Any()
    private var manager: SubscriptionBillingManager? = null

    fun init(context: Context) {
        val billing = manager(context)
        billing.start()
    }

    fun manager(context: Context): SubscriptionBillingManager {
        synchronized(lock) {
            val existing = manager
            if (existing != null) return existing
            val created = SubscriptionBillingManager(context.applicationContext)
            manager = created
            return created
        }
    }

    fun attachActivity(activity: Activity) {
        manager?.attachActivity(activity)
    }

    fun detachActivity(activity: Activity) {
        manager?.detachActivity(activity)
    }

    fun refreshPurchases(onDone: (() -> Unit)? = null) {
        val billing = manager
        if (billing == null) {
            onDone?.invoke()
            return
        }
        billing.refreshPurchases(onDone)
    }
}
