package org.davidnardya.vocalcoach.domain

interface AudioClassifier {
    suspend fun initialize(modelBytes: ByteArray)

    fun inputSampleRateHz(): Int

    fun requiredMonoPcmFrameLength(): Int

    suspend fun classifyMonoPcm16(frame: ShortArray): ClassificationSnapshot?

    fun close()
}
