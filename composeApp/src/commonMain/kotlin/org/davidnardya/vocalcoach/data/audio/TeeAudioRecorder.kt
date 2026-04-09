package org.davidnardya.vocalcoach.data.audio

import org.davidnardya.vocalcoach.domain.repository.PcmFrameAudioRecorder

/**
 * Copies each full PCM frame into [capture] while delegating to the real [PcmFrameAudioRecorder] used by TFLite.
 */
class TeeAudioRecorder(
    private val inner: PcmFrameAudioRecorder,
    private val capture: VersePcmCapture,
) : PcmFrameAudioRecorder {

    override val isRecording: Boolean get() = inner.isRecording

    override fun start(sampleRateHz: Int, frameLengthInSamples: Int) {
        inner.start(sampleRateHz, frameLengthInSamples)
    }

    override fun stop() {
        inner.stop()
    }

    override suspend fun readMonoFrame(buffer: ShortArray): Int {
        val n = inner.readMonoFrame(buffer)
        if (n == buffer.size) {
            capture.appendFrame(buffer.copyOf())
        }
        return n
    }
}
