package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier
import org.davidnardya.vocalcoach.domain.repository.PcmFrameAudioRecorder

class AudioClassificationUseCase(
    private val classifier: AudioClassifier,
    private val recorder: PcmFrameAudioRecorder,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var sessionJob: Job? = null

    /** Sample rate of the loaded classifier (valid after [start] begins). */
    private var lastInputSampleRateHz: Int = 16_000

    fun lastInputSampleRateHz(): Int = lastInputSampleRateHz

    suspend fun start(
        scope: CoroutineScope,
        modelBytes: ByteArray,
        onUpdate: (ClassificationSnapshot) -> Unit,
    ) {
        sessionJob?.cancel()
        withContext(ioDispatcher) {
            requireValidTfliteModelBytes(modelBytes)
            classifier.initialize(modelBytes)
        }
        val rate = classifier.inputSampleRateHz().also { lastInputSampleRateHz = it }
        val frameLen = classifier.requiredMonoPcmFrameLength()
        sessionJob = scope.launch(ioDispatcher) {
            try {
                val buffer = ShortArray(frameLen)
                recorder.start(rate, frameLen)
                while (isActive && recorder.isRecording) {
                    val n = recorder.readMonoFrame(buffer)
                    if (n == frameLen) {
                        val snap = classifier.classifyMonoPcm16(buffer) ?: continue
                        onUpdate(snap)
                    }
                }
            } finally {
                recorder.stop()
            }
        }
    }

    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
        recorder.stop()
        classifier.close()
    }
}
