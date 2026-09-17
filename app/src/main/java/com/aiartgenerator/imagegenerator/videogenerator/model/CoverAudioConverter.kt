package com.aiartgenerator.imagegenerator.videogenerator.model

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Converts any supported input audio into a WAV file for deAPI voice_clone uploads. */
object CoverAudioConverter {
    private const val WAV_HEADER_SIZE = 44
    private const val MAX_REF_SECONDS = 10

    data class PreparedWav(
        val file: File,
        val durationSeconds: Int,
    )

    fun prepareWav(context: Context, uri: Uri): PreparedWav {
        val cacheDir = File(context.cacheDir, "cover_upload").apply { mkdirs() }
        val inputFile = File(cacheDir, "input_${System.currentTimeMillis()}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            inputFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Unable to read voice sample")

        if (inputFile.length() == 0L) {
            inputFile.delete()
            throw IOException("Voice sample file is empty")
        }

        val outputWav = File(cacheDir, "cover_ref.wav")
        return if (isWavFile(inputFile)) {
            trimOrCopyWav(inputFile, outputWav)
            inputFile.delete()
            PreparedWav(outputWav, durationSecondsForWav(outputWav))
        } else {
            decodeToWav(context, uri, inputFile, outputWav)
            inputFile.delete()
            PreparedWav(outputWav, durationSecondsForWav(outputWav))
        }
    }

    private fun trimOrCopyWav(source: File, dest: File) {
        RandomAccessFile(source, "r").use { raf ->
            if (raf.length() < WAV_HEADER_SIZE) {
                throw IOException("Invalid WAV file")
            }
            raf.seek(28L)
            val sampleRate = readLittleEndianInt(raf)
            raf.seek((WAV_HEADER_SIZE - 4).toLong())
            val dataSize = readLittleEndianInt(raf)
            if (sampleRate <= 0 || dataSize <= 0) {
                throw IOException("Invalid WAV file")
            }

            val maxPcmBytes = sampleRate * MAX_REF_SECONDS * 2
            val pcmBytesToCopy = minOf(dataSize, maxPcmBytes)
            raf.seek(WAV_HEADER_SIZE.toLong())
            val pcm = ByteArray(pcmBytesToCopy)
            raf.readFully(pcm)

            FileOutputStream(dest).use { out ->
                out.write(ByteArray(WAV_HEADER_SIZE))
                out.write(pcm)
            }
            writeWavHeader(
                file = dest,
                pcmDataSize = pcmBytesToCopy,
                sampleRate = sampleRate,
                channels = 1,
                bitsPerSample = 16,
            )
        }
    }

    private fun isWavFile(file: File): Boolean {
        if (file.length() < WAV_HEADER_SIZE) return false
        return RandomAccessFile(file, "r").use { raf ->
            val riff = ByteArray(4)
            raf.readFully(riff)
            riff.contentEquals("RIFF".toByteArray())
        }
    }

    private fun decodeToWav(context: Context, uri: Uri, inputFile: File, outputWav: File) {
        val extractor = MediaExtractor()
        try {
            runCatching { extractor.setDataSource(context, uri, null) }
                .getOrElse { extractor.setDataSource(inputFile.absolutePath) }

            var trackIndex = -1
            var inputFormat: MediaFormat? = null
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    trackIndex = index
                    inputFormat = format
                    break
                }
            }
            if (trackIndex < 0 || inputFormat == null) {
                throw IOException("Unsupported audio format. Use wav, mp3, flac, ogg, or m4a.")
            }

            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("Unsupported audio format. Use wav, mp3, flac, ogg, or m4a.")
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val maxPcmBytes = sampleRate * MAX_REF_SECONDS * 2 // mono 16-bit

            extractor.selectTrack(trackIndex)
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val pcmStream = FileOutputStream(outputWav)
            pcmStream.write(ByteArray(WAV_HEADER_SIZE))
            val bufferInfo = MediaCodec.BufferInfo()
            var totalPcmBytes = 0
            var inputDone = false

            try {
                while (true) {
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(10_000)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime,
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }

                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                    when {
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (inputDone) break
                        }
                        outputIndex >= 0 -> {
                            if (bufferInfo.size > 0 && totalPcmBytes < maxPcmBytes) {
                                val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                                val chunkSize = minOf(bufferInfo.size, maxPcmBytes - totalPcmBytes)
                                val chunk = ByteArray(chunkSize)
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.get(chunk, 0, chunkSize)
                                pcmStream.write(chunk)
                                totalPcmBytes += chunkSize
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 ||
                                totalPcmBytes >= maxPcmBytes
                            ) {
                                break
                            }
                        }
                    }
                }
            } finally {
                pcmStream.close()
                runCatching { codec.stop() }
                codec.release()
            }

            if (totalPcmBytes == 0) {
                outputWav.delete()
                throw IOException("Unable to decode voice sample")
            }

            writeWavHeader(
                file = outputWav,
                pcmDataSize = totalPcmBytes,
                sampleRate = sampleRate,
                channels = 1,
                bitsPerSample = 16,
            )
        } finally {
            extractor.release()
        }
    }

    private fun durationSecondsForWav(file: File): Int {
        return RandomAccessFile(file, "r").use { raf ->
            raf.seek(28L)
            val sampleRate = readLittleEndianInt(raf)
            raf.seek((WAV_HEADER_SIZE - 4).toLong())
            val dataSize = readLittleEndianInt(raf)
            if (sampleRate <= 0) return@use 0
            (dataSize / (sampleRate * 2)).coerceAtLeast(1)
        }
    }

    private fun readLittleEndianInt(raf: RandomAccessFile): Int {
        val bytes = ByteArray(4)
        raf.readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun writeWavHeader(
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
