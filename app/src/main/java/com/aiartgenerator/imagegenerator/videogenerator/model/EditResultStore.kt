package com.aiartgenerator.imagegenerator.videogenerator.model

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AiEditLaunchMode {
    /** Home / Create / direct entry — show Upload an Image. */
    Direct,
    /** Generated Image Result → AI Edit — seed image, hide upload card. */
    FromGenerated,
}

data class AiEditLaunch(
    val mode: AiEditLaunchMode,
    val imageUri: Uri?,
)

object EditResultStore {
    data class Payload(
        val originalUri: Uri?,
        val resultBytes: ByteArray?,
        val resultUri: Uri?,
        val toolLabel: String,
        val toolPrompt: String,
    )

    var current: Payload? = null
    private var applyEditedImageOnResume: Boolean = false

    private var pendingLaunch: AiEditLaunch? = null
    private val _editorLaunchEpoch = MutableStateFlow(0)
    val editorLaunchEpoch: StateFlow<Int> = _editorLaunchEpoch.asStateFlow()

    /** Image Result → AI Edit: pass the generated image in without re-uploading. */
    fun prepareFromGeneratedImage(uri: Uri?) {
        pendingLaunch = AiEditLaunch(AiEditLaunchMode.FromGenerated, uri)
        _editorLaunchEpoch.value = _editorLaunchEpoch.value + 1
    }

    /** Direct / Create entry: empty editor with Upload an Image card. */
    fun prepareDirectEntry() {
        pendingLaunch = AiEditLaunch(AiEditLaunchMode.Direct, null)
        _editorLaunchEpoch.value = _editorLaunchEpoch.value + 1
    }

    /** @deprecated Prefer [prepareFromGeneratedImage]. */
    fun setPendingEditorImage(uri: Uri?) = prepareFromGeneratedImage(uri)

    fun consumeEditorLaunch(): AiEditLaunch? {
        val launch = pendingLaunch
        pendingLaunch = null
        return launch
    }

    /** @deprecated Prefer [consumeEditorLaunch]. */
    fun consumePendingEditorImage(): Uri? {
        val launch = pendingLaunch ?: return null
        pendingLaunch = null
        return launch.imageUri
    }

    fun requestApplyEditedImageOnResume() {
        applyEditedImageOnResume = true
    }

    fun consumeEditedImageForEditor(): Uri? {
        if (!applyEditedImageOnResume) return null
        applyEditedImageOnResume = false
        return current?.resultUri
    }

    fun clear() {
        current = null
        applyEditedImageOnResume = false
        pendingLaunch = null
    }
}
