package com.lhy.deepseekmobile.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * Records 16 kHz mono PCM into a WAV file. WAV (instead of m4a) keeps the
 * upload universally accepted by Whisper-compatible endpoints.
 */
class AudioRecorder(private val context: Context) {

    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    private var file: File? = null

    @Volatile
    var level: Float = 0f
        private set

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    /** Returns the destination file, or null when recording could not start. */
    fun start(onLevel: (Float) -> Unit): File? {
        if (!hasPermission()) return null
        val sampleRate = 16_000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBuffer, 4096)
        fun create(source: Int): AudioRecord? {
            val candidate = runCatching {
                AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            }.getOrNull() ?: return null
            if (candidate.state != AudioRecord.STATE_INITIALIZED) {
                candidate.release()
                return null
            }
            return candidate
        }

        val recorder = create(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            ?: create(MediaRecorder.AudioSource.MIC)
            ?: return null

        val out = File(context.cacheDir, "voice-${System.currentTimeMillis()}.wav")
        val stream = runCatching { FileOutputStream(out).also { it.write(ByteArray(HEADER_SIZE)) } }.getOrNull()
        if (stream == null) {
            recorder.release()
            return null
        }

        record = recorder
        file = out
        running = true
        recorder.startRecording()

        worker = thread(name = "voice-recorder", isDaemon = true) {
            val buffer = ShortArray(bufferSize / 2)
            var written = 0L
            val maxBytes = sampleRate.toLong() * 2 * MAX_SECONDS
            try {
                while (running && written < maxBytes) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    val bytes = ByteArray(read * 2)
                    var sum = 0.0
                    for (i in 0 until read) {
                        val value = buffer[i].toInt()
                        bytes[i * 2] = (value and 0xFF).toByte()
                        bytes[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
                        sum += value.toDouble() * value
                    }
                    stream.write(bytes)
                    written += bytes.size
                    val rms = sqrt(sum / read).toFloat()
                    level = (rms / 7000f).coerceIn(0f, 1f)
                    onLevel(level)
                }
            } catch (_: Exception) {
                // Stop button or permission revocation — fall through to header patching.
            } finally {
                runCatching { stream.flush() }
                runCatching { stream.close() }
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
                patchHeader(out, sampleRate)
            }
        }
        return out
    }

    /** Stops recording and returns the file once the worker has closed it. */
    fun stop(): File? {
        running = false
        runCatching { worker?.join(1200) }
        worker = null
        record = null
        return file
    }

    fun cancel() {
        running = false
        runCatching { worker?.join(600) }
        worker = null
        record = null
        file?.delete()
        file = null
    }

    fun level(): Float = level

    private fun patchHeader(file: File, sampleRate: Int) {
        val dataSize = runCatching { (file.length() - HEADER_SIZE).coerceAtLeast(0) }.getOrDefault(0)
        if (dataSize <= 0) return
        val header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt((36 + dataSize).toInt())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1) // PCM
        header.putShort(1) // mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataSize.toInt())
        runCatching {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(header.array())
            }
        }
    }

    companion object {
        private const val HEADER_SIZE = 44
        private const val MAX_SECONDS = 120
    }
}
