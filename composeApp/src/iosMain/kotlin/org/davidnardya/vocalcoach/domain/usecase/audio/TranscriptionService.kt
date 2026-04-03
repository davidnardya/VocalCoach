package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.*
import platform.Foundation.*
import platform.Speech.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

private const val TAG = "TranscriptionService"

actual class TranscriptionService {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun transcribe(audioBuffer: FloatArray): String {
        val pcmCopy = audioBuffer.copyOf()
        println("DEBUG: Sending ${pcmCopy.size} samples to STT engine")

        if (pcmCopy.isEmpty()) {
            println("$TAG: empty buffer — skipping SFSpeechRecognizer")
            return ""
        }

        return suspendCancellableCoroutine { continuation ->
            println("$TAG: DEBUG: Transcription Started...")
            SFSpeechRecognizer.requestAuthorization { status ->
                dispatch_async(dispatch_get_main_queue()) {
                    val authorized =
                        status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
                    if (!authorized) {
                        println("$TAG: SFSpeechRecognizer not authorized (status=$status)")
                        if (continuation.isActive) continuation.resume("")
                        return@dispatch_async
                    }

                    val locale = NSLocale(localeIdentifier = "en-US")
                    val recognizer = SFSpeechRecognizer(locale)
                    if (!recognizer.isAvailable()) {
                        println("$TAG: SFSpeechRecognizer unavailable for locale")
                        if (continuation.isActive) continuation.resume("")
                        return@dispatch_async
                    }

                    val request = SFSpeechAudioBufferRecognitionRequest()
                    request.shouldReportPartialResults = true
                    
                    // On-device recognition is often less aggressive with noise filtering
                    if (recognizer.supportsOnDeviceRecognition) {
                        request.requiresOnDeviceRecognition = true
                    }

                    val format = AVAudioFormat(standardFormatWithSampleRate = 16000.0, channels = 1u)
                    val pcmBuffer = AVAudioPCMBuffer(pCMFormat = format!!, frameCapacity = pcmCopy.size.toUInt())

                    pcmBuffer.frameLength = pcmCopy.size.toUInt()

                    val floatChannelData = pcmBuffer.floatChannelData
                    if (floatChannelData != null) {
                        val data = floatChannelData[0]
                        if (data != null) {
                            for (i in pcmCopy.indices) {
                                data[i] = pcmCopy[i]
                            }
                        }
                    }

                    request.appendAudioPCMBuffer(pcmBuffer)
                    request.endAudio()

                    val task = recognizer.recognitionTaskWithRequest(request) { result, error ->
                        if (error != null) {
                            println("$TAG: recognition error: $error")
                            if (continuation.isActive) continuation.resume("")
                        } else if (result != null) {
                            if (result.isFinal()) {
                                val transcription = result.bestTranscription.formattedString
                                println("$TAG: DEBUG: Transcription Result: '$transcription'")
                                if (continuation.isActive) continuation.resume(transcription)
                            } else {
                                val partial = result.bestTranscription.formattedString
                                println("$TAG: DEBUG: Partial Transcription: '$partial'")
                            }
                        }
                    }

                    continuation.invokeOnCancellation {
                        task.cancel()
                    }
                }
            }
        }
    }
}
