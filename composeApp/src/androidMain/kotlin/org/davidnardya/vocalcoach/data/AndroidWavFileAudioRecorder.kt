package org.davidnardya.vocalcoach.data

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.davidnardya.vocalcoach.data.audio.encodePcm16MonoToWav
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder

private const val SAMPLE_RATE_HZ = 16_000

/**
 * [AudioRecord] capture written as a WAV file under [Context.getCacheDir].
 */
class AndroidWavFileAudioRecorder(
    private val context: Context,
) : AudioRecorder {

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    @Volatile
    private var captureThread: Thread? = null

    private var audioRecord: AudioRecord? = null

    private val pcmChunks = mutableListOf<ShortArray>()
    private val pcmLock = Any()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording() {
        stopRecording()
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, channelConfig, encoding)
        require(minBuf != AudioRecord.ERROR && minBuf != AudioRecord.ERROR_BAD_VALUE) {
            "Invalid AudioRecord buffer size"
        }
        val bufferBytes = max(minBuf, 4096)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE_HZ,
            channelConfig,
            encoding,
            bufferBytes,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }
        synchronized(pcmLock) {
            pcmChunks.clear()
        }
        audioRecord = record
        _isRecording.value = true
        record.startRecording()
        val t = Thread {
            val buf = ShortArray(2048)
            val r = record
            while (_isRecording.value) {
                val n = r.read(buf, 0, buf.size)
                if (n > 0) {
                    synchronized(pcmLock) {
                        pcmChunks.add(buf.copyOf(n))
                    }
                } else if (n < 0) {
                    break
                }
            }
        }
        captureThread = t
        t.start()
    }

    override fun stopRecording(): String? {
        if (!_isRecording.value) return null
        _isRecording.value = false
        val record = audioRecord
        record?.run {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
        }
        captureThread?.join(4000)
        captureThread = null
        audioRecord = null
        record?.release()

        val samples = synchronized(pcmLock) {
            val total = pcmChunks.sumOf { it.size }
            if (total == 0) {
                pcmChunks.clear()
                return@synchronized null
            }
            val out = ShortArray(total)
            var o = 0
            for (chunk in pcmChunks) {
                chunk.copyInto(out, o)
                o += chunk.size
            }
            pcmChunks.clear()
            out
        } ?: return null

        val wav = encodePcm16MonoToWav(samples, SAMPLE_RATE_HZ)
        val file = File(context.cacheDir, "vocal_take_${System.currentTimeMillis()}.wav")
        file.writeBytes(wav)
        return file.absolutePath
    }
}
