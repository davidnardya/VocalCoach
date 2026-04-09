package org.davidnardya.vocalcoach.core.util

import android.media.AudioManager
import android.media.ToneGenerator

actual fun playRecordingArmCue() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        try {
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } finally {
            tg.release()
        }
    } catch (_: Throwable) {
        // Ignore if tone generator unavailable (e.g. emulator without audio).
    }
}
