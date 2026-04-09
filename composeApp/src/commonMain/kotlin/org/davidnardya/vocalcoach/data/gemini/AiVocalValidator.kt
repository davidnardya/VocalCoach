package org.davidnardya.vocalcoach.data.gemini

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.encodeBase64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.davidnardya.vocalcoach.data.gemini.AiVocalValidator.Companion.GEMINI_MODEL
import org.davidnardya.vocalcoach.di.readRecordedAudioBytes
import org.davidnardya.vocalcoach.domain.developer.DeveloperDebugSettings
import org.davidnardya.vocalcoach.domain.models.AiVerseValidationResult
import org.davidnardya.vocalcoach.domain.models.AiVocalAnalysisResult

/**
 * Validates growled verses via Gemini multimodal (audio + text prompt).
 * Uses a current stable model id; legacy ids like `gemini-1.5-flash` return 404 from the API.
 */
class AiVocalValidator(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val jsonParser: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    },
) {

    @Serializable
    private data class AiVocalAnalysisJson(
        @SerialName("is_successful") val isSuccessful: Boolean = false,
        @SerialName("technique_detected") val techniqueDetected: String = "",
        val feedback: String = "",
        @SerialName("technical_score") val technicalScore: Double? = null,
    )

    /**
     * @param audioBytes Raw audio bytes (e.g. WAV) matching [mimeType]
     * @param mimeType e.g. `audio/wav`, `audio/mp4`, `audio/mpeg`
     * @param currentTarget lyric/phrase currently displayed to the player
     */
    suspend fun validateVerse(
        audioBytes: ByteArray,
        mimeType: String,
        currentTarget: String,
    ): Result<AiVerseValidationResult> {
        if (DeveloperDebugSettings.mockGeminiSuccess.value) {
            return Result.success(
                AiVerseValidationResult(
                    score = 9,
                    feedback = "[DEV MOCK] JSON success — pitch and timing look solid.",
                ),
            )
        }
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not configured."))
        }
        if (audioBytes.isEmpty()) {
            return Result.failure(IllegalStateException("No audio captured."))
        }

        val prompt = legacyVersePrompt(currentTarget)

        val base64Audio = audioBytes.encodeBase64()

        val body = buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            add(
                                buildJsonObject {
                                    put("text", prompt)
                                },
                            )
                            add(
                                buildJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", mimeType)
                                        put("data", base64Audio)
                                    }
                                },
                            )
                        }
                    },
                )
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
            }
        }

        return runCatching {
            logGeminiRequestUrl()
            val response: HttpResponse = httpClient.post(geminiGenerateContentUrl()) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(
                    jsonParser.encodeToString(
                        kotlinx.serialization.json.JsonElement.serializer(),
                        body as JsonElement,
                    ),
                )
            }
            val raw = response.bodyAsText()
            if (!response.status.isSuccess()) {
                error("Gemini HTTP ${response.status.value}: $raw")
            }

            val envelope = jsonParser.decodeFromString(GenerateContentResponse.serializer(), raw)
            envelope.error?.let { err ->
                error(err.message ?: "Gemini API error")
            }
            val text = envelope.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: error("Empty Gemini response")

            parseLegacyVerseJson(text)
        }
    }

    /**
     * Reads audio from [filePath], sends it to Gemini, and expects JSON with
     * `technical_score` (0.0–1.0), `is_successful`, and `feedback`.
     */
    suspend fun analyzeVocal(
        filePath: String,
        mimeType: String,
        currentTarget: String,
    ): Result<AiVocalAnalysisResult> {
        if (DeveloperDebugSettings.mockGeminiSuccess.value) {
            return Result.success(
                AiVocalAnalysisResult(
                    isSuccessful = true,
                    techniqueDetected = "growl",
                    feedback = "[DEV MOCK] Solid extreme vocal — you nailed the phrase.",
                    technicalScore = 0.92,
                ),
            )
        }
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not configured."))
        }
        val bytes = readRecordedAudioBytes(filePath)
        if (bytes.isEmpty()) {
            return Result.failure(IllegalStateException("No audio captured."))
        }
        return analyzeVocalFromBytes(bytes, mimeType, currentTarget)
    }

    private suspend fun analyzeVocalFromBytes(
        audioBytes: ByteArray,
        mimeType: String,
        currentTarget: String,
    ): Result<AiVocalAnalysisResult> = try {
        val prompt = growlAnalysisPrompt(currentTarget)

        val base64Audio = audioBytes.encodeBase64()
        val body = buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            add(
                                buildJsonObject {
                                    put("text", prompt)
                                },
                            )
                            add(
                                buildJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", mimeType)
                                        put("data", base64Audio)
                                    }
                                },
                            )
                        }
                    },
                )
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
            }
        }

        logGeminiRequestUrl()
        val response: HttpResponse = httpClient.post(geminiGenerateContentUrl()) {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(
                jsonParser.encodeToString(
                    kotlinx.serialization.json.JsonElement.serializer(),
                    body as JsonElement,
                ),
            )
        }
        val raw = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Gemini HTTP ${response.status.value}: $raw")
        }

        val envelope = jsonParser.decodeFromString(GenerateContentResponse.serializer(), raw)
        envelope.error?.let { err ->
            error(err.message ?: "Gemini API error")
        }
        val text = envelope.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Empty Gemini response")

        Result.success(parseAnalysisJson(text))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    private fun parseAnalysisJson(text: String): AiVocalAnalysisResult {
        val cleaned = stripMarkdownCodeFence(text.trim())
        val raw = try {
            jsonParser.decodeFromString(AiVocalAnalysisJson.serializer(), cleaned)
        } catch (e: Exception) {
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start >= 0 && end > start) {
                jsonParser.decodeFromString(
                    AiVocalAnalysisJson.serializer(),
                    cleaned.substring(start, end + 1),
                )
            } else {
                throw e
            }
        }
        val technical = raw.technicalScore?.let { normalizeTechnicalScore(it) }
            ?: if (raw.isSuccessful) 0.88 else 0.35
        return AiVocalAnalysisResult(
            isSuccessful = raw.isSuccessful,
            techniqueDetected = raw.techniqueDetected.trim(),
            feedback = raw.feedback.trim(),
            technicalScore = technical,
        )
    }

    /**
     * Maps API output to 0.0–1.0 and derives success from growl delivery only (not lyric clarity).
     */
    private fun normalizeTechnicalScore(value: Double): Double = when {
        value.isNaN() -> 0.0
        value > 1.0 -> (value / 100.0).coerceIn(0.0, 1.0)
        else -> value.coerceIn(0.0, 1.0)
    }

    private fun growlAnalysisPrompt(currentTarget: String) = """
        You are a Metal Vocal Validator. Listen to the audio.

        Validation Criteria:
        - TECHNIQUE: Is the user using an extreme vocal technique? (Acceptable: Growl, Guttural, Scream, Screech, Shout, False Cord, Fry). If any distortion is present, pass the technique check.
        - LYRICS: Did the user pronounce the target words: $currentTarget? Accept approximate pronunciation. Do not penalize for slurring or lack of clarity common in metal.
        - QUALITY: Ignore volume, consistency, and 'brutality.'

        Set is_successful to true only if distortion is detected AND the target words are roughly recognizable.

        Return ONLY a JSON object:
        {
          "is_successful": <boolean>,
          "technique_detected": "<string (e.g., scream, growl)>",
          "feedback": "<A short, encouraging 1-sentence comment.>"
        }
    """.trimIndent()

    private fun legacyVersePrompt(currentTarget: String) =
        "I am a metal vocal coach. Listen to this audio. The user is attempting to growl the following lyrics: $currentTarget. " +
            "On a scale of 1-10, how accurate is their pronunciation and timing? " +
            "Return a JSON object with 'score' and 'feedback'."

    private fun parseLegacyVerseJson(text: String): AiVerseValidationResult {
        val cleaned = stripMarkdownCodeFence(text.trim())
        return try {
            jsonParser.decodeFromString(AiVerseValidationResult.serializer(), cleaned)
        } catch (e: Exception) {
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start >= 0 && end > start) {
                jsonParser.decodeFromString(
                    AiVerseValidationResult.serializer(),
                    cleaned.substring(start, end + 1),
                )
            } else {
                throw e
            }
        }
    }

    companion object {
        /** Base host for the Generative Language API (Gemini). */
        private const val GEMINI_API_HOST = "https://generativelanguage.googleapis.com"
        /**
         * REST version segment. `v1beta` matches the documented `models.generateContent` template.
         * (`v1` also works for many models; 404 here is usually an outdated [GEMINI_MODEL] id.)
         */
        private const val GEMINI_API_VERSION = "v1beta"
        /** Stable model id — see https://ai.google.dev/gemini-api/docs/models/gemini */
        private const val GEMINI_MODEL = "gemini-2.5-flash"

        private fun geminiGenerateContentUrl(): String =
            "$GEMINI_API_HOST/$GEMINI_API_VERSION/models/$GEMINI_MODEL:generateContent"

        /**
         * Logs the exact URL used for the request. The API key query value is redacted.
         */
        private fun logGeminiRequestUrl() {
            val url = "${geminiGenerateContentUrl()}?key=<redacted>"
            println("[AiVocalValidator] Gemini POST $url")
        }

        fun stripMarkdownCodeFence(text: String): String {
            var t = text.trim()
            if (!t.startsWith("```")) return t
            t = t.removePrefix("```json").removePrefix("```").trim()
            val fence = t.lastIndexOf("```")
            if (fence >= 0) {
                t = t.substring(0, fence).trim()
            }
            return t
        }
    }
}
