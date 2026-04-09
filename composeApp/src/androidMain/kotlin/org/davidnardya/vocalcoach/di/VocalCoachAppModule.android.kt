package org.davidnardya.vocalcoach.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import org.davidnardya.vocalcoach.BuildKonfig
import org.davidnardya.vocalcoach.data.AndroidNotificationScheduler
import org.davidnardya.vocalcoach.data.AndroidPcmAudioRecorder
import org.davidnardya.vocalcoach.data.AndroidWavFileAudioRecorder
import org.davidnardya.vocalcoach.data.TfLiteTaskAudioClassifierAdapter
import org.davidnardya.vocalcoach.domain.repository.AudioClassifier
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.PcmFrameAudioRecorder
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

actual fun createGeminiHttpClient(): HttpClient = HttpClient(Android) {
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 120_000
    }
}

actual fun readGeminiApiKey(): String = BuildKonfig.GEMINI_API_KEY

actual fun createAudioClassifier(): AudioClassifier {
    return TfLiteTaskAudioClassifierAdapter(AndroidPlatformContext.application)
}

actual fun createPcmFrameAudioRecorder(): PcmFrameAudioRecorder = AndroidPcmAudioRecorder()

actual fun createFileAudioRecorder(): AudioRecorder =
    AndroidWavFileAudioRecorder(AndroidPlatformContext.application)

actual fun readRecordedAudioBytes(path: String): ByteArray = java.io.File(path).readBytes()

actual fun createNotificationScheduler(): NotificationScheduler {
    return AndroidNotificationScheduler(AndroidPlatformContext.application)
}
