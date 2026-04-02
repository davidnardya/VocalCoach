package org.davidnardya.vocalcoach.data

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import kotlin.math.max

class AndroidPcmAudioRecorder : AudioRecorder {

    @Volatile
    private var recording: Boolean = false

    private var audioRecord: AudioRecord? = null

    override val isRecording: Boolean get() = recording

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start(sampleRateHz: Int, frameLengthInSamples: Int) {
        stop()
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, encoding)
        require(minBuf != AudioRecord.ERROR && minBuf != AudioRecord.ERROR_BAD_VALUE) {
            "Invalid AudioRecord buffer size"
        }
        val bufferBytes = max(minBuf, frameLengthInSamples * 2 * 4)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRateHz,
            channelConfig,
            encoding,
            bufferBytes,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error("AudioRecord failed to initialize")
        }
        audioRecord = record
        record.startRecording()
        recording = true
    }

    override fun stop() {
        recording = false
        audioRecord?.apply {
            try {
                stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        audioRecord = null
    }

    override suspend fun readMonoFrame(buffer: ShortArray): Int = withContext(Dispatchers.IO) {
        val record = audioRecord ?: return@withContext 0
        var total = 0
        val need = buffer.size
        while (total < need && recording) {
            val n = record.read(buffer, total, need - total)
            if (n < 0) return@withContext total
            total += n
        }
        total
    }
}
