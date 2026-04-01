package org.davidnardya.vocalcoach.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.davidnardya.vocalcoach.domain.AudioClassifier
import org.davidnardya.vocalcoach.domain.ClassificationSnapshot
import org.davidnardya.vocalcoach.domain.LabelScore
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier as NativeAudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.io.File

class TfLiteTaskAudioClassifierAdapter(
    private val appContext: Context,
) : AudioClassifier {

    private var native: NativeAudioClassifier? = null
    private var tensorAudio: TensorAudio? = null

    override suspend fun initialize(modelBytes: ByteArray) = withContext(Dispatchers.IO) {
        close()
        val modelFile = File(appContext.cacheDir, MODEL_CACHE_FILE_NAME)
        modelFile.writeBytes(modelBytes)
        val options = NativeAudioClassifier.AudioClassifierOptions.builder()
            .setMaxResults(5)
            .build()
        val classifier = NativeAudioClassifier.createFromFileAndOptions(modelFile, options)
        native = classifier
        tensorAudio = classifier.createInputTensorAudio()
    }

    override fun inputSampleRateHz(): Int {
        val format = native?.requiredTensorAudioFormat ?: error("Classifier not initialized")
        return format.sampleRate
    }

    override fun requiredMonoPcmFrameLength(): Int {
        val ta = tensorAudio ?: error("Classifier not initialized")
        val shape = ta.tensorBuffer.shape
        return shape[1]
    }

    override suspend fun classifyMonoPcm16(frame: ShortArray): ClassificationSnapshot? =
        withContext(Dispatchers.Default) {
            val classifier = native ?: return@withContext null
            val ta = tensorAudio ?: return@withContext null
            ta.load(frame)
            val heads: List<Classifications> = classifier.classify(ta)
            if (heads.isEmpty()) return@withContext null
            val categories: List<Category> = heads[0].categories
            if (categories.isEmpty()) return@withContext null
            val ranked = categories.map { c ->
                LabelScore(label = c.label, score = c.score)
            }
            val top = ranked.maxBy { ls -> ls.score }
            ClassificationSnapshot(
                topLabel = top.label,
                topScore = top.score,
                ranked = ranked,
            )
        }

    override fun close() {
        native?.close()
        native = null
        tensorAudio = null
    }

    private companion object {
        const val MODEL_CACHE_FILE_NAME = "soundclassifier_with_metadata.tflite"
    }
}
