package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException

/** Plays a raw voice sample for preview — never synthesized or AI-processed audio. */
object VoicePreviewPlayer {
    class Handle internal constructor(
        val player: MediaPlayer,
        private val fileDescriptor: ParcelFileDescriptor?,
    ) {
        fun release() {
            runCatching { if (player.isPlaying) player.stop() }
            player.release()
            runCatching { fileDescriptor?.close() }
        }
    }

    fun open(
        context: Context,
        uri: Uri,
        localPath: String? = null,
        onComplete: () -> Unit,
    ): Handle {
        val path = localPath?.takeIf { File(it).exists() }
        val mediaPlayer = MediaPlayer()
        val pfd = if (path != null) {
            mediaPlayer.setDataSource(path)
            null
        } else {
            openContentSource(context, uri, mediaPlayer)
        }
        mediaPlayer.setOnCompletionListener {
            onComplete()
            mediaPlayer.seekTo(0)
        }
        mediaPlayer.prepare()
        return Handle(mediaPlayer, pfd)
    }

    private fun openContentSource(
        context: Context,
        uri: Uri,
        mediaPlayer: MediaPlayer,
    ): ParcelFileDescriptor? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Unable to open voice sample")
        mediaPlayer.setDataSource(pfd.fileDescriptor, 0, pfd.statSize)
        return pfd
    }
}
