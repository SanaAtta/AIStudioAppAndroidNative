package com.example.myapplication.controller

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.example.myapplication.model.VoiceCatalog
import com.example.myapplication.model.VoiceOption
import com.example.myapplication.network.AiApi
import com.example.myapplication.util.AndroidFileHelper
import java.io.File

enum class SongSource {
    Upload,
    Record,
}

class AiCoverController(
    private val api: AiApi = AiApi(),
) {
    var selectedMode by mutableStateOf(SongSource.Record)
        private set
    var songUri by mutableStateOf<Uri?>(null)
        private set
    var songDisplayName by mutableStateOf<String?>(null)
        private set
    var songSource by mutableStateOf<SongSource?>(null)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var recordingSeconds by mutableIntStateOf(0)
        private set
    var selectedVoiceId by mutableStateOf("no_voice")
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var isCreating by mutableStateOf(false)
        private set
    var resultUri by mutableStateOf<Uri?>(null)
        private set

    val voices: List<VoiceOption> get() = VoiceCatalog.voices
    val hasSong: Boolean get() = songUri != null && !isRecording
    val canCreate: Boolean get() = hasSong && !isCreating

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var audioCounter = 0

    fun selectMode(mode: SongSource) {
        if (mode == selectedMode) return
        if (isRecording) {
            stopRecordingInternal(discard = true)
        }
        selectedMode = mode
    }

    fun onSongUploaded(context: Context, uri: Uri) {
        stopRecordingInternal(discard = true)
        selectedMode = SongSource.Upload
        songUri = uri
        songDisplayName = queryDisplayName(context, uri)
        songSource = SongSource.Upload
    }

    fun startRecording(context: Context): Boolean {
        if (isRecording) return true
        return try {
            val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
            val file = File(dir, "cover_record_${System.currentTimeMillis()}.m4a")
            val recorder = createRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordingFile = file
            songUri = null
            songDisplayName = null
            songSource = null
            recordingSeconds = 0
            selectedMode = SongSource.Record
            isRecording = true
            true
        } catch (e: Exception) {
            releaseRecorder()
            recordingFile?.delete()
            recordingFile = null
            statusMessage = "Could not start recording"
            false
        }
    }

    fun stopRecording(context: Context): Boolean {
        if (!isRecording) return false
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            val file = recordingFile ?: return false
            audioCounter += 1
            songUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            songDisplayName = "Audio%03d".format(audioCounter)
            songSource = SongSource.Record
            selectedMode = SongSource.Record
            true
        } catch (e: Exception) {
            releaseRecorder()
            recordingFile?.delete()
            recordingFile = null
            isRecording = false
            statusMessage = "Recording failed"
            false
        }
    }

    fun clearSong() {
        if (isRecording) {
            stopRecordingInternal(discard = true)
        }
        songUri = null
        songDisplayName = null
        songSource = null
        recordingFile?.delete()
        recordingFile = null
        recordingSeconds = 0
    }

    fun tickRecordingSecond() {
        if (isRecording) {
            recordingSeconds += 1
        }
    }

    fun onVoiceSelected(voiceId: String) {
        selectedVoiceId = voiceId
    }

    suspend fun onCreateCover(context: Context) {
        val source = songUri ?: return
        if (!canCreate) return
        isCreating = true
        statusMessage = null
        try {
            val voiceTitle = voices.firstOrNull { it.id == selectedVoiceId }?.title ?: "No Voice"
            val audioBytes = AndroidFileHelper.readBytes(context, source)
            val bytes = api.generateCover(
                audioBytes = audioBytes,
                voiceTitle = voiceTitle,
            ).getOrElse { throw it }
            val file = AndroidFileHelper.saveToCache(context, bytes, "cover_${System.currentTimeMillis()}.mp3")
            resultUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            statusMessage = "AI Cover created successfully"
        } catch (e: Exception) {
            statusMessage = e.message ?: "Cover creation failed"
        } finally {
            isCreating = false
        }
    }

    fun clearStatus() {
        statusMessage = null
    }

    fun release() {
        stopRecordingInternal(discard = true)
    }

    private fun stopRecordingInternal(discard: Boolean) {
        if (mediaRecorder == null && !isRecording) return
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        releaseRecorder()
        isRecording = false
        if (discard) {
            recordingFile?.delete()
            recordingFile = null
            recordingSeconds = 0
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }

    private fun createRecorder(context: Context): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(index)
                if (!name.isNullOrBlank()) return name
            }
        }
        return "Selected song"
    }
}
