package com.aiartgenerator.imagegenerator.videogenerator.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.aiartgenerator.imagegenerator.videogenerator.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics helper — logs user clicks / screen actions.
 *
 * Event: [EVENT_CLICK] with params [PARAM_ITEM] (+ optional [PARAM_SCREEN], [PARAM_EXTRA]).
 */
object AppAnalytics {
    const val EVENT_CLICK = "user_click"
    const val PARAM_ITEM = "item_id"
    const val PARAM_SCREEN = "screen"
    const val PARAM_EXTRA = "extra"

    private const val TAG = "AppAnalytics"

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun click(
        itemId: String,
        screen: String? = null,
        extra: String? = null,
    ) {
        val id = sanitize(itemId, 40)
        if (id.isEmpty()) return
        val bundle =
            Bundle().apply {
                putString(PARAM_ITEM, id)
                screen?.let { putString(PARAM_SCREEN, sanitize(it, 40)) }
                extra?.let { putString(PARAM_EXTRA, sanitize(it, 100)) }
            }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "click item=$id screen=$screen extra=$extra")
        }
        runCatching { analytics?.logEvent(EVENT_CLICK, bundle) }
    }

    fun event(name: String, params: Map<String, String> = emptyMap()) {
        val eventName = sanitize(name, 40)
        if (eventName.isEmpty()) return
        val bundle =
            Bundle().apply {
                params.forEach { (k, v) ->
                    putString(sanitize(k, 40), sanitize(v, 100))
                }
            }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "event=$eventName params=$params")
        }
        runCatching { analytics?.logEvent(eventName, bundle) }
    }

    private fun sanitize(value: String, max: Int): String {
        val cleaned =
            value
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9_]+"), "_")
                .trim('_')
        return cleaned.take(max)
    }
}
