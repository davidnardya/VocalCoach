package org.davidnardya.vocalcoach.domain.repository

interface AudioRecorder {
    val isRecording: Boolean

    fun start(sampleRateHz: Int, frameLengthInSamples: Int)

    fun stop()

    suspend fun readMonoFrame(buffer: ShortArray): Int
}
