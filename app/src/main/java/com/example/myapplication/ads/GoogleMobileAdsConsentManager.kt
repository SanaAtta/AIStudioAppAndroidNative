package com.example.myapplication.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.example.myapplication.BuildConfig
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google UMP consent manager (callback API).
 * Logcat filter: `AdsConsent`
 */
class GoogleMobileAdsConsentManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    val canRequestAds: Boolean
        get() = runCatching { consentInformation.canRequestAds() }.getOrDefault(false)

    val consentStatus: Int
        get() = consentInformation.consentStatus

    val isConsentFormAvailable: Boolean
        get() = consentInformation.isConsentFormAvailable

    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener,
    ) {
        scope.launch {
            val debugTestDeviceIds =
                if (BuildConfig.DEBUG) {
                    withContext(Dispatchers.IO) {
                        resolveDebugTestDeviceIds(appContext)
                    }
                } else {
                    emptyList()
                }

            if (BuildConfig.DEBUG && BuildConfig.UMP_RESET_ON_DEBUG_LAUNCH) {
                runCatching {
                    consentInformation.reset()
                    Log.d(
                        TAG,
                        "DEBUG reset consent (ump.reset.on.debug.launch=true in local.properties)",
                    )
                }.onFailure { error ->
                    Log.w(TAG, "DEBUG consent reset failed", error)
                }
            }

            logConsentSnapshot("before-update")
            if (isConsentAlreadyResolved()) {
                Log.d(TAG, "consent already resolved before update — skipping GDPR flow")
                onConsentGatheringCompleteListener.consentGatheringComplete(null)
                return@launch
            }
            val params = buildConsentParams(appContext, debugTestDeviceIds)
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    Log.d(TAG, "requestConsentInfoUpdate success")
                    logConsentSnapshot("after-update")
                    logFormUnavailableHintsIfNeeded()
                    if (!isConsentFormRequired()) {
                        Log.d(
                            TAG,
                            "consent resolved after update — skipping GDPR dialog " +
                                "status=${consentStatusLabel(consentInformation.consentStatus)}",
                        )
                        onConsentGatheringCompleteListener.consentGatheringComplete(null)
                        return@requestConsentInfoUpdate
                    }
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            logFormError("loadAndShowConsentFormIfRequired", formError)
                        } else {
                            Log.d(TAG, "GDPR consent flow finished (no form error)")
                        }
                        logConsentSnapshot("after-form")
                        onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                    }
                },
                { requestError ->
                    logFormError("requestConsentInfoUpdate", requestError)
                    logConsentSnapshot("after-update-failed")
                    onConsentGatheringCompleteListener.consentGatheringComplete(requestError)
                },
            )
        }
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: OnConsentFormDismissedListener,
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    private fun buildConsentParams(
        context: Context,
        debugTestDeviceIds: List<String>,
    ): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "DEBUG build: EEA geography forced")
            val debugBuilder =
                ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            debugTestDeviceIds.forEach { hashedId ->
                debugBuilder.addTestDeviceHashedId(hashedId)
                Log.d(TAG, "DEBUG registered UMP test device id=$hashedId")
            }
            builder.setConsentDebugSettings(debugBuilder.build())
        }
        return builder.build()
    }

    private suspend fun resolveDebugTestDeviceIds(context: Context): List<String> {
        val configured = parseTestDeviceHashedIds(BuildConfig.UMP_TEST_DEVICE_HASHED_IDS)
        val autoId =
            runCatching {
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val adId = adInfo.id?.trim().orEmpty()
                if (adId.isEmpty()) return@runCatching null
                md5Uppercase(adId)
            }.getOrElse { error ->
                Log.w(TAG, "DEBUG failed to auto-resolve UMP test device id", error)
                null
            }
        if (autoId != null) {
            Log.d(TAG, "DEBUG auto UMP test device id from advertising id=$autoId")
        }
        return (configured + listOfNotNull(autoId)).distinct()
    }

    private fun md5Uppercase(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte ->
            "%02X".format(Locale.US, byte)
        }
    }

    private fun parseTestDeviceHashedIds(raw: String): List<String> =
        raw.split(',', ';')
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }

    private fun isConsentAlreadyResolved(): Boolean =
        isConsentFormRequired().not() && consentInformation.canRequestAds()

    /** UMP only needs the dialog while status is REQUIRED. */
    private fun isConsentFormRequired(): Boolean =
        consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED

    private fun logFormUnavailableHintsIfNeeded() {
        if (!consentInformation.isConsentFormAvailable) {
            logFormUnavailableHints()
        }
    }

    private fun logFormUnavailableHints() {
        Log.w(TAG, "consent form NOT available after update — check:")
        Log.w(TAG, "  1) AdMob → Privacy & messaging → GDPR message published for this app id")
        Log.w(TAG, "  2) admob.app.id matches AdMob app (${BuildConfig.ADMOB_APP_ID})")
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "  3) UMP test device id registered (auto or ump.test.device.hashed.id)")
        }
    }

    private fun logConsentSnapshot(phase: String) {
        Log.d(
            TAG,
            "[$phase] status=${consentStatusLabel(consentInformation.consentStatus)} " +
                "formAvailable=${consentInformation.isConsentFormAvailable} " +
                "canRequestAds=${consentInformation.canRequestAds()}",
        )
    }

    private fun consentStatusLabel(status: Int): String =
        when (status) {
            ConsentInformation.ConsentStatus.UNKNOWN -> "UNKNOWN"
            ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
            ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
            else -> "OTHER($status)"
        }

    private fun logFormError(step: String, error: FormError) {
        Log.w(TAG, "$step failed code=${error.errorCode} message=${error.message}")
    }

    companion object {
        private const val TAG = "AdsConsent"

        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context): GoogleMobileAdsConsentManager =
            instance
                ?: synchronized(this) {
                    instance ?: GoogleMobileAdsConsentManager(context.applicationContext)
                        .also { instance = it }
                }
    }
}
