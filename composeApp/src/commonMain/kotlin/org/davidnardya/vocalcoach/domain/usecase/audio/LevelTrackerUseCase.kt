package org.davidnardya.vocalcoach.domain.usecase.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import org.davidnardya.vocalcoach.domain.repository.ScoreRepository

class LevelTrackerUseCase(private val repository: ScoreRepository) {
    private val _levelUpEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val levelUpEvents = _levelUpEvents.asSharedFlow()

    fun monitorLevelUps(): Flow<Int> {
        return repository.getPlayerStats()
            .map { it.level }
            .distinctUntilChanged()
            .drop(1) // Ignore initial level
            .map { newLevel ->
                _levelUpEvents.emit(newLevel)
                newLevel
            }
    }
}
