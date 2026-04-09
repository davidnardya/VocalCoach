package org.davidnardya.vocalcoach.data.audio

/**
 * Rolling buffer of mono PCM samples for verse validation (fed by [TeeAudioRecorder]).
 */
class VersePcmCapture(
    private val maxSamples: Int = DEFAULT_MAX_SAMPLES,
) {
    private val samples = ArrayList<Short>(maxSamples.coerceAtMost(32_768))

    fun clear() {
        samples.clear()
    }

    fun appendFrame(frame: ShortArray) {
        for (s in frame) {
            samples.add(s)
            if (samples.size > maxSamples) {
                samples.removeAt(0)
            }
        }
    }

    fun snapshotPcm(): ShortArray = samples.toShortArray()

    companion object {
        /** ~15s at 16 kHz — enough context for a short verse without huge payloads. */
        const val DEFAULT_MAX_SAMPLES: Int = 16_000 * 15
    }
}
