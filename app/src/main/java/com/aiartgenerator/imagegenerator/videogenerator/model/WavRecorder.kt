package com.aiartgenerator.imagegenerator.videogenerator.model

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavRecorder(
    private val outputFile: File,
    private val sampleRate: Int = 44_100,
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording = false

    private var totalPcmBytes = 0L

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val channelCount = 1
    private val bitsPerSample = 16

    fun start(): Boolean {
        if (isRecording) return true

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        if (minBuffer <= 0) return false

        val bufferSize = minBuffer * 4
        val record = createAudioRecord(bufferSize) ?: return false

        outputFile.parentFile?.mkdirs()
        outputFile.delete()
        totalPcmBytes = 0L
        audioRecord = record
        isRecording = true
        record.startRecording()

        recordingThread = Thread {
            FileOutputStream(outputFile).use { out ->
                out.write(ByteArray(WAV_HEADER_SIZE))
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        out.write(buffer, 0, read)
                        totalPcmBytes += read
                    }
                }
            }
        }.also { it.start() }
        return true
    }

    fun stop(): Int {
        if (!isRecording) return 0
        isRecording = false
        recordingThread?.join()
        recordingThread = null
        audioRecord?.apply {
            runCatching { stop() }
            release()
        }
        audioRecord = null

        fixWavHeader(
            file = outputFile,
            pcmDataSize = totalPcmBytes.toInt(),
            sampleRate = sampleRate,
            channels = channelCount,
            bitsPerSample = bitsPerSample,
        )
        return (totalPcmBytes / (sampleRate * channelCount * (bitsPerSample / 8))).toInt()
    }

    private fun createAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.MIC)
        }
        for (source in sources) {
            val record = AudioRecord(
                source,
                sampleRate,
                channelConfig,
                audioEncoding,
                bufferSize,
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                return record
            }
            record.release()
        }
        return null
    }

    private companion object {
        private const val WAV_HEADER_SIZE = 44

        private fun fixWavHeader(
            file: File,
            pcmDataSize: Int,
            sampleRate: Int,
            channels: Int,
            bitsPerSample: Int,
        ) {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                val byteRate = sampleRate * channels * bitsPerSample / 8
                val header = ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray())
                header.putInt(36 + pcmDataSize)
                header.put("WAVE".toByteArray())
                header.put("fmt ".toByteArray())
                header.putInt(16)
                header.putShort(1)
                header.putShort(channels.toShort())
                header.putInt(sampleRate)
                header.putInt(byteRate)
                header.putShort((channels * bitsPerSample / 8).toShort())
                header.putShort(bitsPerSample.toShort())
                header.put("data".toByteArray())
                header.putInt(pcmDataSize)
                raf.write(header.array())
            }
        }
    }
}
