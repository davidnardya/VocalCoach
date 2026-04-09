package org.davidnardya.vocalcoach.domain.repository

/**
 * Mono PCM frame source for real-time classifiers (e.g. TFLite).
 */
interface PcmFrameAudioRecorder {
    val isRecording: Boolean

    fun start(sampleRateHz: Int, frameLengthInSamples: Int)

    fun stop()

    suspend fun readMonoFrame(buffer: ShortArray): Int
}
