package org.davidnardya.vocalcoach.data

import cocoapods.TensorFlowLiteTaskAudio.TFLAudioClassifier
import cocoapods.TensorFlowLiteTaskAudio.TFLAudioClassifierOptions
import cocoapods.TensorFlowLiteTaskAudio.TFLAudioTensor
import cocoapods.TensorFlowLiteTaskAudio.TFLCategory
import cocoapods.TensorFlowLiteTaskAudio.TFLClassifications
import cocoapods.TensorFlowLiteTaskAudio.TFLFloatBuffer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.davidnardya.vocalcoach.domain.models.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.models.LabelScore
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier

@OptIn(ExperimentalForeignApi::class)
class IosTfLiteAudioClassifierAdapter : AudioClassifier {

    private var classifier: TFLAudioClassifier? = null
    private var audioTensor: TFLAudioTensor? = null

    override suspend fun initialize(modelBytes: ByteArray) = withContext(Dispatchers.Default) {
        close()
        val path = modelBytes.writeToTemporaryTfliteFile("soundclassifier_with_metadata.tflite")
        val options = TFLAudioClassifierOptions(modelPath = path)
        options.classificationOptions.maxResults = 5
        val c = TFLAudioClassifier.audioClassifierWithOptions(options, null)
            ?: error("TFLAudioClassifier failed to initialize")
        classifier = c
        audioTensor = c.createInputAudioTensor()
    }

    override fun inputSampleRateHz(): Int {
        val rate = audioTensor?.audioFormat?.sampleRate ?: error("Classifier not initialized")
        return rate.toInt()
    }

    override fun requiredMonoPcmFrameLength(): Int {
        val tensor = audioTensor ?: error("Classifier not initialized")
        val channels = tensor.audioFormat.channelCount
        if (channels == 0uL) error("Invalid channel count")
        return (tensor.bufferSize / channels).toInt()
    }

    override suspend fun classifyMonoPcm16(frame: ShortArray): ClassificationSnapshot? =
        withContext(Dispatchers.Default) {
            val tensor = audioTensor ?: return@withContext null
            val c = classifier ?: return@withContext null
            val floats = FloatArray(frame.size)
            for (i in frame.indices) {
                floats[i] = frame[i] / 32768f
            }
            val loaded = floats.usePinned { pinned ->
                val fb = TFLFloatBuffer(pinned.addressOf(0), floats.size.convert())
                tensor.loadBuffer(fb, offset = 0u, size = floats.size.convert(), error = null)
            }
            if (!loaded) return@withContext null
            val result = c.classifyWithAudioTensor(tensor, null) ?: return@withContext null

            @Suppress("UNCHECKED_CAST")
            val heads = result.classifications as List<Any>
            if (heads.isEmpty()) return@withContext null
            val head = heads.first() as? TFLClassifications ?: return@withContext null
            @Suppress("UNCHECKED_CAST")
            val categories = head.categories as List<Any>
            if (categories.isEmpty()) return@withContext null
            val ranked = categories.mapNotNull { item ->
                val cat = item as? TFLCategory ?: return@mapNotNull null
                val label = cat.label ?: return@mapNotNull null
                val score = cat.score.toFloat()
                LabelScore(label = label, score = score)
            }
            if (ranked.isEmpty()) return@withContext null
            val top = ranked.maxBy { it.score }
            ClassificationSnapshot(
                topLabel = top.label,
                topScore = top.score,
                ranked = ranked,
            )
        }

    override fun close() {
        classifier = null
        audioTensor = null
    }
}
