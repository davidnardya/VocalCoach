@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.davidnardya.vocalcoach.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import org.davidnardya.vocalcoach.domain.AudioRecorder
import org.davidnardya.vocalcoach.interop.vcaudio.vc_audio_sample
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFrameCount
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setPreferredSampleRate
import platform.AVFAudio.setActive
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalForeignApi::class)
class IosPcmAudioRecorder : AudioRecorder {

    private val sampleChannel = Channel<Short>(Channel.UNLIMITED)

    private var engine: AVAudioEngine? = null

    private var recording: Boolean = false

    override val isRecording: Boolean get() = recording

    override fun start(sampleRateHz: Int, frameLengthInSamples: Int) {
        stop()
        recording = true
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            AVAudioSessionCategoryOptionAllowBluetooth,
            null,
        )
        session.setPreferredSampleRate(sampleRateHz.toDouble(), null)
        session.setActive(true, null)

        val eng = AVAudioEngine()
        val input = eng.inputNode
        val format = input.outputFormatForBus(0u)
        val bufferFrames: AVAudioFrameCount = 4096u
        input.installTapOnBus(
            bus = 0u,
            bufferSize = bufferFrames,
            format = format,
        ) { pcmBuffer: AVAudioPCMBuffer?, _ ->
            if (pcmBuffer == null || !recording) {
                return@installTapOnBus
            }
            val frames = pcmBuffer.frameLength.toInt()
            if (frames <= 0) {
                return@installTapOnBus
            }
            val floatPlanes = pcmBuffer.floatChannelData ?: return@installTapOnBus
            val ch = format.channelCount.toInt().coerceAtLeast(1)
            for (sampleIndex in 0 until frames) {
                var acc = 0f
                for (channelIndex in 0 until ch) {
                    acc += vc_audio_sample(floatPlanes, channelIndex, sampleIndex)
                }
                val mono = acc / ch
                val s = (mono.coerceIn(-1f, 1f) * 32767f).toInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt(),
                ).toShort()
                sampleChannel.trySend(s)
            }
        }
        eng.prepare()
        val ok = eng.startAndReturnError(null)
        if (!ok) {
            recording = false
            eng.inputNode.removeTapOnBus(0u)
            return
        }
        engine = eng
    }

    override fun stop() {
        recording = false
        val eng = engine
        engine = null
        eng?.inputNode?.removeTapOnBus(0u)
        eng?.stop()
        while (sampleChannel.tryReceive().isSuccess) {
        }
        AVAudioSession.sharedInstance().setActive(false, null)
    }

    override suspend fun readMonoFrame(buffer: ShortArray): Int {
        if (!recording) return 0
        return try {
            withContext(Dispatchers.Default) {
                repeat(buffer.size) { i ->
                    buffer[i] = sampleChannel.receive()
                }
                buffer.size
            }
        } catch (_: CancellationException) {
            0
        }
    }
}
