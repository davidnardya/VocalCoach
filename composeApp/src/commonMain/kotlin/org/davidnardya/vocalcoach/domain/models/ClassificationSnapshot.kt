package org.davidnardya.vocalcoach.domain.models

data class LabelScore(
    val label: String,
    val score: Float,
)

data class ClassificationSnapshot(
    val topLabel: String,
    val topScore: Float,
    val ranked: List<LabelScore> = emptyList(),
    val audioBuffer: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ClassificationSnapshot
        if (topLabel != other.topLabel) return false
        if (topScore != other.topScore) return false
        if (ranked != other.ranked) return false
        if (!audioBuffer.contentEquals(other.audioBuffer)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = topLabel.hashCode()
        result = 31 * result + topScore.hashCode()
        result = 31 * result + ranked.hashCode()
        result = 31 * result + audioBuffer.contentHashCode()
        return result
    }
}
