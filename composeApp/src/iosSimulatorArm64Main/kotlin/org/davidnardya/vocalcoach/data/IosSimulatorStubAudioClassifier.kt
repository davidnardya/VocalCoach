package org.davidnardya.vocalcoach.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier

/**
 * TensorFlowLiteTaskAudio does not ship an arm64 iOS-simulator slice; linking it fails on Apple Silicon.
 * The real classifier runs on device ([IosTfLiteAudioClassifierAdapter]); simulator builds use this stub.
 */
class IosSimulatorStubAudioClassifier : AudioClassifier {

    override suspend fun initialize(modelBytes: ByteArray) = withContext(Dispatchers.Default) {
        close()
    }

    override fun inputSampleRateHz(): Int = 16_000

    override fun requiredMonoPcmFrameLength(): Int = 512

    override suspend fun classifyMonoPcm16(frame: ShortArray): ClassificationSnapshot? = null

    override fun close() = Unit
}
