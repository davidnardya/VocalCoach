package org.davidnardya.vocalcoach.domain

data class LabelScore(
    val label: String,
    val score: Float,
)

data class ClassificationSnapshot(
    val topLabel: String,
    val topScore: Float,
    val ranked: List<LabelScore> = emptyList(),
)
