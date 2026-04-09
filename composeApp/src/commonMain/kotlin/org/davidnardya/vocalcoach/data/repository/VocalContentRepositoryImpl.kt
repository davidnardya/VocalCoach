package org.davidnardya.vocalcoach.data.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.repository.VocalContentRepository
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vocalcoach.composeapp.generated.resources.Res

private const val VOCAL_CONTENT_RESOURCE_PATH = "files/vocal_content.json"

class VocalContentRepositoryImpl : VocalContentRepository {
    private var cached: VocalContentPayload? = null
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getTargets(stage: TrainingStage): List<String> {
        val payload = cached ?: run {
            val bytes = Res.readBytes(VOCAL_CONTENT_RESOURCE_PATH)
            val parsed = json.decodeFromString(
                VocalContentPayload.serializer(),
                bytes.decodeToString(),
            )
            cached = parsed
            parsed
        }
        return when (stage) {
            TrainingStage.Stage3 -> payload.stage3Words
            TrainingStage.Stage4 -> payload.stage4Phrases
            TrainingStage.Stage5 -> payload.stage5Verses
            else -> emptyList()
        }
    }
}

@Serializable
private data class VocalContentPayload(
    val stage3Words: List<String> = emptyList(),
    val stage4Phrases: List<String> = emptyList(),
    val stage5Verses: List<String> = emptyList(),
)
