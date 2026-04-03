package org.davidnardya.vocalcoach.domain.usecase.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "TranscriptionService"

actual class TranscriptionService(private val context: Context) {
    actual suspend fun transcribe(audioBuffer: FloatArray): String {
        val appContext = context.applicationContext
        val pcmFrames = audioBuffer.size
        Log.d(TAG, "transcribe: PCM frames=$pcmFrames")

        if (pcmFrames == 0) {
            Log.w(TAG, "transcribe: empty buffer — skipping SpeechRecognizer")
            return ""
        }

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "transcribe: RECORD_AUDIO not granted — cannot run SpeechRecognizer")
            return ""
        }

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            Log.e(TAG, "transcribe: SpeechRecognizer not available on this device")
            return ""
        }

        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "DEBUG: Sending $pcmFrames samples to STT engine")
            Log.d(TAG, "DEBUG: Transcription Started... (Android STT uses live microphone; PCM buffer is not fed to the API)")

            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            }

            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    Log.w(TAG, "SpeechRecognizer onError code=$error")
                    if (continuation.isActive) continuation.resume("")
                    recognizer.destroy()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull() ?: ""
                    Log.d(TAG, "DEBUG: Transcription Result: '$result'")
                    if (continuation.isActive) continuation.resume(result)
                    recognizer.destroy()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull()
                    if (result != null) {
                        Log.d(TAG, "DEBUG: Partial Transcription Result: '$result'")
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            recognizer.setRecognitionListener(listener)
            recognizer.startListening(intent)

            continuation.invokeOnCancellation {
                recognizer.cancel()
                recognizer.destroy()
            }
        }
    }
}
