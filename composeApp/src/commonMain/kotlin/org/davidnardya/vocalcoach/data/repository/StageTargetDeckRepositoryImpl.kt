package org.davidnardya.vocalcoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import org.davidnardya.vocalcoach.data.local.producePath
import org.davidnardya.vocalcoach.domain.models.TrainingStage
import org.davidnardya.vocalcoach.domain.repository.StageTargetDeckRepository

class StageTargetDeckRepositoryImpl : StageTargetDeckRepository {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath("target_deck.preferences_pb").toPath() },
    )

    override suspend fun getUsedIndices(stage: TrainingStage): Set<Int> {
        val key = usedIndicesKey(stage)
        val raw = dataStore.data.first()[key].orEmpty()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    override suspend fun saveUsedIndices(stage: TrainingStage, usedIndices: Set<Int>) {
        val key = usedIndicesKey(stage)
        dataStore.edit { preferences ->
            preferences[key] = usedIndices.map { it.toString() }.toSet()
        }
    }

    private fun usedIndicesKey(stage: TrainingStage) =
        stringSetPreferencesKey("used_indices_${stage.name.lowercase()}")
}
