package org.davidnardya.vocalcoach.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AiVerseValidationResult(
    val score: Int,
    val feedback: String,
)
