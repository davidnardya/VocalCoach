@file:OptIn(ExperimentalForeignApi::class)

package org.davidnardya.vocalcoach.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.davidnardya.vocalcoach.BuildKonfig
import org.davidnardya.vocalcoach.data.IosFileAudioRecorder
import org.davidnardya.vocalcoach.data.IosNotificationScheduler
import org.davidnardya.vocalcoach.data.IosPcmAudioRecorder
import org.davidnardya.vocalcoach.domain.repository.AudioRecorder
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import org.davidnardya.vocalcoach.domain.repository.PcmFrameAudioRecorder
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind

actual fun createGeminiHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 120_000
    }
}

actual fun readGeminiApiKey(): String = BuildKonfig.GEMINI_API_KEY

actual fun createPcmFrameAudioRecorder(): PcmFrameAudioRecorder = IosPcmAudioRecorder()

actual fun createFileAudioRecorder(): AudioRecorder = IosFileAudioRecorder()

actual fun readRecordedAudioBytes(path: String): ByteArray {
    val file = fopen(path, "rb") ?: return byteArrayOf()
    return try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt()
        if (size <= 0) return byteArrayOf()
        rewind(file)
        val buffer = ByteArray(size)
        buffer.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, size.toULong(), file)
        }
        buffer
    } finally {
        fclose(file)
    }
}

actual fun createNotificationScheduler(): NotificationScheduler = IosNotificationScheduler()
