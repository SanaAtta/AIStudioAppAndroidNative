package com.example.myapplication.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Thin wrapper around [GoogleMobileAdsConsentManager] for splash + [AdsControl].
 * Logcat filter: `AdsConsent`
 */
object AdsConsentManager {
    private const val TAG = "AdsConsent"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var consentGatheredThisSession = false

    @Volatile
    private var consentGatheringInProgress = false

    /** Avoid UMP binder IPC on every Compose recomposition / ad poll loop. */
    @Volatile
    private var cachedCanRequestAds: Boolean? = null

    private val heavyInitDone = CompletableDeferred<Unit>()

    fun hasGatheredConsentThisSession(): Boolean = consentGatheredThisSession

    /** Fast — sets context only; safe on main during [Application.onCreate]. */
    fun init(applicationContext: Context) {
        appContext = applicationContext.applicationContext
    }

    /** UMP / Mobile Ads consent SDK — run off the main thread (binder IPC). */
    fun completeHeavyInit() {
        val ctx = appContext
        if (ctx == null) {
            if (!heavyInitDone.isCompleted) heavyInitDone.complete(Unit)
            return
        }
        runCatching {
            GoogleMobileAdsConsentManager.getInstance(ctx)
            Log.d(TAG, "init admobAppId=${BuildConfig.ADMOB_APP_ID}")
        }.onFailure { error ->
            Log.w(TAG, "heavy init failed", error)
        }
        if (!heavyInitDone.isCompleted) heavyInitDone.complete(Unit)
        refreshCanRequestAdsCache()
    }

    private suspend fun awaitHeavyInit() {
        if (heavyInitDone.isCompleted) return
        withTimeoutOrNull(10_000L) { heavyInitDone.await() }
    }

    private fun manager(): GoogleMobileAdsConsentManager {
        val ctx = appContext ?: error("AdsConsentManager.init() must be called first")
        return GoogleMobileAdsConsentManager.getInstance(ctx)
    }

    suspend fun gatherConsent(activity: Activity) {
        awaitHeavyInit()
        if (consentGatheredThisSession) {
            Log.d(TAG, "gatherConsent skipped — already completed this session")
            return
        }
        if (consentGatheringInProgress) {
            Log.d(TAG, "gatherConsent waiting — flow already in progress")
            waitForConsentGatheringComplete()
            return
        }

        consentGatheringInProgress = true
        val consentManager = manager()
        logInitialConsentStatus(consentManager)

        if (isConsentAlreadyResolved(consentManager)) {
            appContext?.let { GdprConsentPrefs.setGdprCompleted(it, true) }
            consentGatheredThisSession = true
            consentGatheringInProgress = false
            refreshCanRequestAdsCache()
            AdsConfigRevision.bump()
            Log.d(
                TAG,
                "gatherConsent skipped — consent already OBTAINED/NOT_REQUIRED, canRequestAds=true",
            )
            return
        }

        try {
            suspendCancellableCoroutine<Unit> { cont ->
                consentManager.gatherConsent(activity) { consentError ->
                    handleConsentGatheringComplete(
                        context = activity.applicationContext,
                        consentManager = consentManager,
                        consentError = consentError,
                    )
                    consentGatheredThisSession = true
                    consentGatheringInProgress = false
                    refreshCanRequestAdsCache()
                    AdsConfigRevision.bump()
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        } catch (e: Throwable) {
            consentGatheringInProgress = false
            throw e
        }
    }

    private suspend fun waitForConsentGatheringComplete() {
        while (consentGatheringInProgress && !consentGatheredThisSession) {
            kotlinx.coroutines.delay(50)
        }
    }

    fun canRequestAds(): Boolean {
        cachedCanRequestAds?.let { return it }
        return refreshCanRequestAdsCache()
    }

    private fun refreshCanRequestAdsCache(): Boolean {
        val result = computeCanRequestAds()
        cachedCanRequestAds = result
        return result
    }

    private fun computeCanRequestAds(): Boolean {
        val ctx = appContext ?: run {
            Log.d(TAG, "canRequestAds=false (appContext null)")
            return false
        }
        return runCatching {
            val consentManager = GoogleMobileAdsConsentManager.getInstance(ctx)
            when {
                consentManager.canRequestAds -> true
                consentManager.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED -> true
                consentGatheredThisSession &&
                    consentManager.consentStatus == ConsentInformation.ConsentStatus.UNKNOWN -> {
                    Log.w(
                        TAG,
                        "canRequestAds: UNKNOWN after gather — allowing ads (offline fallback)",
                    )
                    true
                }
                else -> false
            }
        }.getOrElse { error ->
            Log.w(TAG, "canRequestAds check failed", error)
            consentGatheredThisSession
        }
    }

    fun invalidateCanRequestAdsCache() {
        cachedCanRequestAds = null
    }

    fun isConsentRequired(): Boolean {
        val ctx = appContext ?: return false
        return GoogleMobileAdsConsentManager.getInstance(ctx).consentStatus ==
            ConsentInformation.ConsentStatus.REQUIRED
    }

    private fun isConsentAlreadyResolved(consentManager: GoogleMobileAdsConsentManager): Boolean {
        if (!consentManager.canRequestAds) return false
        return when (consentManager.consentStatus) {
            ConsentInformation.ConsentStatus.OBTAINED,
            ConsentInformation.ConsentStatus.NOT_REQUIRED,
            -> true
            else -> false
        }
    }

    private fun logInitialConsentStatus(consentManager: GoogleMobileAdsConsentManager) {
        when (consentManager.consentStatus) {
            ConsentInformation.ConsentStatus.REQUIRED ->
                Log.d(TAG, "consent REQUIRED: ${consentManager.consentStatus}")
            ConsentInformation.ConsentStatus.NOT_REQUIRED ->
                Log.d(TAG, "consent NOT_REQUIRED: ${consentManager.consentStatus}")
            ConsentInformation.ConsentStatus.OBTAINED -> {
                appContext?.let { GdprConsentPrefs.setGdprCompleted(it, true) }
                Log.d(TAG, "consent OBTAINED: ${consentManager.consentStatus}")
            }
            ConsentInformation.ConsentStatus.UNKNOWN ->
                Log.d(TAG, "consent UNKNOWN: ${consentManager.consentStatus}")
            else ->
                Log.d(TAG, "consent status=${consentManager.consentStatus}")
        }
    }

    private fun handleConsentGatheringComplete(
        context: Context,
        consentManager: GoogleMobileAdsConsentManager,
        consentError: FormError?,
    ) {
        if (consentError != null) {
            Log.w(
                TAG,
                "Consent error code=${consentError.errorCode} message=${consentError.message}",
            )
            refreshCanRequestAdsCache()
            return
        }

        if (consentManager.isConsentFormAvailable) {
            Log.d(TAG, "Consent accepted/rejected")
            if (consentManager.canRequestAds) {
                Log.d(TAG, "Consent gathered canRequestAds=true")
                GdprConsentPrefs.setGdprCompleted(context, true)
                AdsConfigRevision.bump()
            } else {
                Log.w(TAG, "Consent form shown but canRequestAds=false")
            }
            refreshCanRequestAdsCache()
            return
        }

        if (consentManager.canRequestAds) {
            Log.d(TAG, "No consent form required (non EU / not required)")
            GdprConsentPrefs.setGdprCompleted(context, true)
            refreshCanRequestAdsCache()
            AdsConfigRevision.bump()
        } else {
            Log.w(TAG, "Consent finished canRequestAds=false formAvailable=false")
            refreshCanRequestAdsCache()
        }
    }
}
