package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.CoverVoiceStyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoverStore
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratedCoversRepository
import com.aiartgenerator.imagegenerator.videogenerator.model.SingerStyleOption
import com.aiartgenerator.imagegenerator.videogenerator.model.WavRecorder
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiApi
import com.aiartgenerator.imagegenerator.videogenerator.model.network.AiConfig
import com.aiartgenerator.imagegenerator.videogenerator.model.network.ApiProvider
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageKind
import com.aiartgenerator.imagegenerator.videogenerator.billing.FreeUsageLimits
import java.io.File
import java.io.IOException

enum class SongSource {
    Upload,
    Record,
}

class AiCoverController(
    private val api: AiApi = AiApi(),
) {
    private companion object {
        private const val TAG = "AiCover"
        private const val MIN_SAMPLE_SECONDS = 10
        private const val MAX_SAMPLE_SECONDS = 60
    }

    var songUri by mutableStateOf<Uri?>(null)
        private set
    var songLocalPath by mutableStateOf<String?>(null)
        private set
    var songDisplayName by mutableStateOf<String?>(null)
        private set
    var songSource by mutableStateOf<SongSource?>(null)
        private set
    var songDurationSeconds by mutableIntStateOf(0)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var recordingSeconds by mutableIntStateOf(0)
        private set
    var selectedSingerStyleId by mutableStateOf("pop_star")
        private set
    var selectedCoverVoiceStyleId by mutableStateOf("original")
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var isCreating by mutableStateOf(false)
        private set

    val singerStyles: List<SingerStyleOption> get() = CoverCatalog.singerStyles
    val coverVoiceStyles: List<CoverVoiceStyleOption> get() = CoverCatalog.voiceStyles
    val hasSong: Boolean get() = songUri != null && !isRecording
    val canGenerate: Boolean get() = hasSong && !isCreating && !isRecording

    private var wavRecorder: WavRecorder? = null
    private var recordingFile: File? = null

    fun onSongUploaded(context: Context, uri: Uri) {
        stopRecordingInternal(discard = true)
        setVoiceSample(
            context = context,
            uri = uri,
            source = SongSource.Upload,
            displayName = queryDisplayName(context, uri),
            localPath = null,
        )
    }

    fun startRecording(context: Context): Boolean {
        if (isRecording) return true
        if (hasSong) {
            clearSong()
        }
        stopAppAudioPlayback(context)
        return try {
            val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
            val file = File(dir, "cover_record_${System.currentTimeMillis()}.wav")
            val recorder = WavRecorder(file)
            if (!recorder.start()) {
                statusMessage = context.getString(R.string.error_recording_start)
                return false
            }
            wavRecorder = recorder
            recordingFile = file
            songUri = null
            songLocalPath = null
            songDisplayName = null
            songSource = null
            songDurationSeconds = 0
            recordingSeconds = 0
            isRecording = true
            true
        } catch (e: Exception) {
            wavRecorder = null
            recordingFile?.delete()
            recordingFile = null
            statusMessage = context.getString(R.string.error_recording_start)
            false
        }
    }

    fun stopRecording(context: Context): Boolean {
        if (!isRecording) return false
        if (recordingSeconds < MIN_SAMPLE_SECONDS) {
            stopRecordingInternal(discard = true)
            statusMessage = context.getString(R.string.error_record_min_seconds)
            return false
        }
        return try {
            wavRecorder?.stop()
            wavRecorder = null
            isRecording = false
            val file = recordingFile ?: return false
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            setVoiceSample(
                context = context,
                uri = uri,
                source = SongSource.Record,
                displayName = context.getString(R.string.cover_recorded_voice),
                localPath = file.absolutePath,
            )
        } catch (e: Exception) {
            stopRecordingInternal(discard = true)
            statusMessage = context.getString(R.string.error_recording_failed)
            false
        }
    }

    fun clearSong() {
        if (isRecording) {
            stopRecordingInternal(discard = true)
        }
        removePreviousRecordingFile()
        songUri = null
        songLocalPath = null
        songDisplayName = null
        songSource = null
        songDurationSeconds = 0
        recordingSeconds = 0
    }

    fun tickRecordingSecond(context: Context) {
        if (!isRecording) return
        recordingSeconds += 1
        if (recordingSeconds >= MAX_SAMPLE_SECONDS) {
            val saved = stopRecording(context)
            if (saved) {
                statusMessage = context.getString(R.string.cover_recording_max_reached)
            }
        }
    }

    fun onSingerStyleSelected(styleId: String) {
        selectedSingerStyleId = styleId
    }

    fun onCoverVoiceStyleSelected(styleId: String) {
        selectedCoverVoiceStyleId = styleId
    }

    suspend fun onGenerate(context: Context, onSuccess: () -> Unit) {
        if (isCreating) return

        if (!hasSong || songUri == null) {
            statusMessage = context.getString(R.string.error_voice_sample_required)
            return
        }
        if (!FreeUsageLimits.canUse(FreeUsageKind.Cover)) {
            statusMessage = context.getString(R.string.free_limit_reached)
            return
        }

        if (!AiConfig.isCoverGenerationConfigured) {
            statusMessage = context.getString(R.string.error_deapi_key_missing)
            return
        }

        // Snapshot selections at tap time so the cover matches what the user picked.
        val singerStyleId = selectedSingerStyleId
        val voiceStyleId = selectedCoverVoiceStyleId
        val singer = CoverCatalog.findSingerStyle(singerStyleId)
        val voiceStyle = CoverCatalog.findVoiceStyle(voiceStyleId)
        val singerLabel = singer?.label ?: "Pop Star"
        val voiceStyleLabel = voiceStyle?.label ?: "Original"
        val songName = songDisplayName ?: context.getString(R.string.cover_voice_ready)
        val durationSeconds = readAudioDurationSeconds(context, songUri!!)
            ?: songDurationSeconds.coerceAtLeast(MIN_SAMPLE_SECONDS)

        isCreating = true
        statusMessage = null

        try {
            val singerHint = CoverCatalog.singerStyleHint(singerStyleId)
            val voiceHint = CoverCatalog.coverVoiceHint(voiceStyleId)

            Log.i(
                TAG,
                "Using API: ${ApiProvider.label(ApiProvider.DEAPI)} → ai-cover voice_clone " +
                    "(singer=$singerStyleId/$singerLabel, voice=$voiceStyleId/$voiceStyleLabel, source=$songSource)",
            )

            val bytes = api.generateCover(
                context = context,
                songUri = songUri!!,
                singerStyleId = singerStyleId,
                voiceStyleId = voiceStyleId,
                singerStyleHint = singerHint,
                voiceStyleHint = voiceHint,
            ).getOrElse { throw it }

            val cover = GeneratedCoversRepository.add(
                context = context,
                bytes = bytes,
                songName = songName,
                singerStyleLabel = singerLabel,
                voiceStyleLabel = voiceStyleLabel,
                durationSeconds = durationSeconds,
            )
            val uri = GeneratedCoversRepository.getUri(context, cover)
                ?: throw IOException("Unable to access generated cover")

            GeneratedCoverStore.current = GeneratedCoverStore.Payload(
                uri = uri,
                title = cover.title,
                songName = songName,
                singerStyleLabel = singerLabel,
                voiceStyleLabel = voiceStyleLabel,
                durationSeconds = durationSeconds,
                creationId = cover.id,
            )
            FreeUsageLimits.recordSuccessfulUse(FreeUsageKind.Cover)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Cover generation failed: ${e.message}", e)
            statusMessage = e.message ?: context.getString(R.string.error_cover_generation_failed)
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

    private fun setVoiceSample(
        context: Context,
        uri: Uri,
        source: SongSource,
        displayName: String,
        localPath: String? = null,
    ): Boolean {
        val durationSeconds = readAudioDurationSeconds(context, uri)
        val effectiveDuration = durationSeconds ?: recordingSeconds
        if (effectiveDuration < MIN_SAMPLE_SECONDS) {
            if (source == SongSource.Record) {
                recordingFile?.delete()
                recordingFile = null
            }
            statusMessage = context.getString(R.string.error_audio_too_short)
            return false
        }
        if (effectiveDuration > MAX_SAMPLE_SECONDS) {
            if (source == SongSource.Record) {
                recordingFile?.delete()
                recordingFile = null
            }
            statusMessage = context.getString(R.string.error_audio_too_long)
            return false
        }

        if (source == SongSource.Upload) {
            removePreviousRecordingFile()
        }

        songUri = uri
        songLocalPath = localPath
        songDisplayName = displayName
        songSource = source
        songDurationSeconds = durationSeconds ?: recordingSeconds.coerceAtLeast(MIN_SAMPLE_SECONDS)
        recordingSeconds = 0
        return true
    }

    private fun removePreviousRecordingFile() {
        if (songSource == SongSource.Record) {
            recordingFile?.delete()
            recordingFile = null
        }
    }

    private fun stopAppAudioPlayback(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        runCatching { audioManager.abandonAudioFocus(null) }
    }

    private fun stopRecordingInternal(discard: Boolean) {
        if (!isRecording && wavRecorder == null) return
        runCatching { wavRecorder?.stop() }
        wavRecorder = null
        isRecording = false
        if (discard) {
            recordingFile?.delete()
            recordingFile = null
            recordingSeconds = 0
        }
    }

    private fun readAudioDurationSeconds(context: Context, uri: Uri): Int? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.let { (it / 1000L).toInt() }
        }
    }.getOrNull()

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
        return context.getString(R.string.upload_your_voice)
    }
}
