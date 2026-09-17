package com.aiartgenerator.imagegenerator.videogenerator.analytics

/** Wraps a click lambda so every invocation logs a Firebase [AppAnalytics.click]. */
fun trackedClick(
    itemId: String,
    screen: String? = null,
    extra: String? = null,
    onClick: () -> Unit,
): () -> Unit =
    {
        AppAnalytics.click(itemId = itemId, screen = screen, extra = extra)
        onClick()
    }
