package com.aiartgenerator.imagegenerator.videogenerator.ui.components

import android.view.View
import android.widget.ImageView
import coil.load
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

internal fun bindNativeSmallIcon(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    val iv =
        nativeAdView.iconView as? ImageView
            ?: nativeAdView.findViewById<ImageView>(R.id.ad_app_icon)
            ?: return

    iv.scaleType = ImageView.ScaleType.CENTER_CROP

    nativeAd.icon?.drawable?.let { drawable ->
        iv.setImageDrawable(drawable)
        iv.visibility = View.VISIBLE
        return
    }

    nativeAd.icon?.uri?.let { uri ->
        iv.visibility = View.VISIBLE
        iv.load(uri) {
            crossfade(false)
            listener(onError = { _, _ ->
                iv.setImageDrawable(null)
                iv.visibility = View.GONE
            })
        }
        return
    }

    val fallback = nativeAd.images?.firstOrNull()
    if (fallback == null) {
        iv.setImageDrawable(null)
        iv.visibility = View.GONE
        return
    }

    fallback.drawable?.let { drawable ->
        iv.setImageDrawable(drawable)
        iv.visibility = View.VISIBLE
        return
    }

    fallback.uri?.let { uri ->
        iv.visibility = View.VISIBLE
        iv.load(uri) {
            crossfade(false)
            listener(onError = { _, _ ->
                iv.setImageDrawable(null)
                iv.visibility = View.GONE
            })
        }
        return
    }

    iv.setImageDrawable(null)
    iv.visibility = View.GONE
}