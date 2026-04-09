package org.davidnardya.vocalcoach.data.gemini

import kotlinx.serialization.Serializable

@Serializable
internal data class GenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val error: GeminiErrorBody? = null,
)

@Serializable
internal data class GeminiErrorBody(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null,
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
)

@Serializable
internal data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
)
