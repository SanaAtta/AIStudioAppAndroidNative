package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Splash native — one AdMob request per splash session (same idea as [AppOpenAdManager] splash app-open).
 * Remounting the Compose slot re-binds the cached ad; no duplicate [AdLoader] calls.
 */
object SplashNativeAdManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var sessionAd: NativeAd? = null

    @Volatile
    private var sessionUnitId: String? = null

    @Volatile
    private var sessionTemplate: NativeAdTemplate? = null

    @Volatile
    private var loadInFlight = false

    /** One network request per splash session — no retry after failure. */
    private val loadAttempted = AtomicBoolean(false)

    private val loadListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()

    fun ensureLoad(
        context: Context,
        unitId: String,
        template: NativeAdTemplate,
        slotLabel: String,
        rcParam: String,
    ) {
        if (!AdsControl.shouldShowAds() || unitId.isBlank() || template == NativeAdTemplate.DISABLED) {
            deliverLoadResult(false)
            return
        }
        MobileAdsInitializer.runWhenReady {
            mainHandler.post {
                ensureLoadOnMain(context.applicationContext, unitId, template, slotLabel, rcParam)
            }
        }
    }

    fun awaitLoaded(
        context: Context,
        unitId: String,
        template: NativeAdTemplate,
        slotLabel: String,
        rcParam: String,
        onComplete: (Boolean) -> Unit,
    ) {
        if (hasSessionAd(unitId, template)) {
            mainHandler.post { onComplete(true) }
            return
        }
        loadListeners.add(onComplete)
        ensureLoad(context, unitId, template, slotLabel, rcParam)
    }

    fun borrowAd(
        unitId: String,
        template: NativeAdTemplate,
    ): NativeAd? {
        if (!hasSessionAd(unitId, template)) return null
        return sessionAd
    }

    fun releaseSession() {
        mainHandler.post {
            sessionAd?.destroy()
            sessionAd = null
            sessionUnitId = null
            sessionTemplate = null
            loadInFlight = false
            loadAttempted.set(false)
            loadListeners.clear()
        }
    }

    private fun hasSessionAd(
        unitId: String,
        template: NativeAdTemplate,
    ): Boolean {
        if (unitId.isBlank() || template == NativeAdTemplate.DISABLED) return false
        return sessionAd != null && sessionUnitId == unitId && sessionTemplate == template
    }

    private fun ensureLoadOnMain(
        app: Context,
        unitId: String,
        template: NativeAdTemplate,
        slotLabel: String,
        rcParam: String,
    ) {
        if (!AdsControl.shouldShowAds() || unitId.isBlank() || template == NativeAdTemplate.DISABLED) {
            deliverLoadResult(false)
            return
        }
        if (hasSessionAd(unitId, template)) {
            deliverLoadResult(true)
            return
        }
        if (loadInFlight) return
        if (loadAttempted.get()) {
            deliverLoadResult(false)
            return
        }

        loadAttempted.set(true)
        loadInFlight = true
        sessionUnitId = unitId
        sessionTemplate = template
        AdsLoadLog.requesting(slotLabel, rcParam, unitId)

        AdLoader.Builder(app, unitId)
            .forNativeAd { ad ->
                AdsLoadLog.loaded(slotLabel, rcParam, unitId)
                sessionAd = ad
                loadInFlight = false
                deliverLoadResult(true)
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        AdsLoadLog.failed(slotLabel, rcParam, unitId, error)
                        sessionAd = null
                        loadInFlight = false
                        deliverLoadResult(false)
                    }
                },
            )
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun deliverLoadResult(success: Boolean) {
        val listeners = loadListeners.toList()
        loadListeners.clear()
        listeners.forEach { listener ->
            runCatching { listener(success) }
        }
    }
}
