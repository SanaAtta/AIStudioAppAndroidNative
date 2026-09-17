package com.aiartgenerator.imagegenerator.videogenerator.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import java.util.concurrent.ConcurrentHashMap

class AdmobNativeManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val activeAds = ConcurrentHashMap<String, NativeAdWrapper>()
    private val loadingRequests = ConcurrentHashMap<String, Long>()
    private val adRequest by lazy { AdRequest.Builder().build() }

    fun loadNativeAd(
        adContainer: FrameLayout,
        adUnitId: String,
        adSize: NativeAdSize,
        shimmerContainer: FrameLayout? = null,
        colorConfig: NativeAdColorConfig = NativeAdColorConfig.default(),
        source: String = "default",
        onLoaded: ((NativeAd) -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null,
    ) {
        loadNativeAdInternal(
            adContainer = adContainer,
            shimmerContainer = shimmerContainer,
            adUnitId = adUnitId,
            layoutRes = NativeAdLayouts.layoutRes(adSize),
            shimmerLayoutRes = NativeAdLayouts.shimmerLayoutRes(adSize),
            colorConfig = colorConfig,
            source = source,
            onLoaded = onLoaded,
            onFailed = onFailed,
        )
    }

    fun showNativeAdIfAvailable(
        adUnitId: String,
        adContainer: FrameLayout,
        adSize: NativeAdSize,
        shimmerContainer: FrameLayout? = null,
        colorConfig: NativeAdColorConfig = NativeAdColorConfig.default(),
        onShown: ((NativeAd) -> Unit)? = null,
    ): Boolean {
        val wrapper = activeAds[adUnitId] ?: return false
        if (wrapper.impressionRecorded) return false

        return try {
            val adView = LayoutInflater.from(appContext)
                .inflate(NativeAdLayouts.layoutRes(adSize), adContainer, false) as NativeAdView

            NativeAdViewBinder.bind(wrapper.ad, adView, colorConfig)
            hideShimmer(shimmerContainer)
            adContainer.removeAllViews()
            adContainer.addView(adView)
            adContainer.visibility = View.VISIBLE
            wrapper.impressionRecorded = true
            onShown?.invoke(wrapper.ad)
            true
        } catch (_: Exception) {
            destroyAd(adUnitId)
            false
        }
    }

    fun destroyAd(adUnitId: String) {
        activeAds.remove(adUnitId)?.let { wrapper ->
            runCatching { wrapper.ad.destroy() }
        }
        loadingRequests.keys.removeAll { it.startsWith("$adUnitId::") }
    }

    private fun loadNativeAdInternal(
        adContainer: FrameLayout,
        shimmerContainer: FrameLayout?,
        adUnitId: String,
        layoutRes: Int,
        shimmerLayoutRes: Int,
        colorConfig: NativeAdColorConfig,
        source: String,
        onLoaded: ((NativeAd) -> Unit)?,
        onFailed: ((LoadAdError) -> Unit)?,
    ) {
        if (adUnitId.isBlank()) {
            onFailed?.invoke(LoadAdError(0, "Ad unit id is empty", "internal", null, null))
            return
        }

        val loadKey = "$adUnitId::$source"
        val currentTime = System.currentTimeMillis()
        val existingLoadTime = loadingRequests.putIfAbsent(loadKey, currentTime)
        if (existingLoadTime != null && currentTime - existingLoadTime < LOAD_TIMEOUT_MS) {
            onFailed?.invoke(LoadAdError(0, "Ad load already in progress", "internal", null, null))
            return
        }
        loadingRequests[loadKey] = currentTime

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                loadNativeAdInternal(
                    adContainer,
                    shimmerContainer,
                    adUnitId,
                    layoutRes,
                    shimmerLayoutRes,
                    colorConfig,
                    source,
                    onLoaded,
                    onFailed,
                )
            }
            return
        }

        showShimmer(shimmerContainer, shimmerLayoutRes)
        adContainer.visibility = View.GONE

        try {
            val adLoader = AdLoader.Builder(appContext, adUnitId)
                .forNativeAd { nativeAd ->
                    loadingRequests.remove(loadKey)
                    activeAds.remove(adUnitId)?.ad?.destroy()
                    activeAds[adUnitId] = NativeAdWrapper(nativeAd, impressionRecorded = false)
                    renderNativeAd(
                        nativeAd = nativeAd,
                        adContainer = adContainer,
                        shimmerContainer = shimmerContainer,
                        layoutRes = layoutRes,
                        colorConfig = colorConfig,
                        onLoaded = onLoaded,
                        onFailed = onFailed,
                        adUnitId = adUnitId,
                    )
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadingRequests.remove(loadKey)
                        onFailed?.invoke(error)
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setRequestMultipleImages(false)
                        .build(),
                )
                .build()

            adLoader.loadAd(adRequest)
        } catch (_: Exception) {
            loadingRequests.remove(loadKey)
            onFailed?.invoke(LoadAdError(0, "AdLoader initialization failed", "internal", null, null))
        }
    }

    private fun renderNativeAd(
        nativeAd: NativeAd,
        adContainer: FrameLayout,
        shimmerContainer: FrameLayout?,
        layoutRes: Int,
        colorConfig: NativeAdColorConfig,
        onLoaded: ((NativeAd) -> Unit)?,
        onFailed: ((LoadAdError) -> Unit)?,
        adUnitId: String,
    ) {
        try {
            val adView = LayoutInflater.from(appContext)
                .inflate(layoutRes, adContainer, false) as NativeAdView

            NativeAdViewBinder.bind(nativeAd, adView, colorConfig)
            hideShimmer(shimmerContainer)
            adContainer.removeAllViews()
            adContainer.addView(adView)
            adContainer.visibility = View.VISIBLE
            onLoaded?.invoke(nativeAd)
        } catch (_: Exception) {
            activeAds.remove(adUnitId)
            runCatching { nativeAd.destroy() }
            onFailed?.invoke(LoadAdError(0, "Render error", "internal", null, null))
        }
    }

    private fun showShimmer(shimmerContainer: FrameLayout?, shimmerLayoutRes: Int) {
        shimmerContainer?.let { container ->
            container.removeAllViews()
            val shimmerView = LayoutInflater.from(appContext)
                .inflate(shimmerLayoutRes, container, false)
            container.addView(shimmerView)
            container.visibility = View.VISIBLE
        }
    }

    private fun hideShimmer(shimmerContainer: FrameLayout?) {
        shimmerContainer?.visibility = View.GONE
    }

    private data class NativeAdWrapper(
        val ad: NativeAd,
        var impressionRecorded: Boolean,
    )

    companion object {
        private const val LOAD_TIMEOUT_MS = 30_000L
    }
}
