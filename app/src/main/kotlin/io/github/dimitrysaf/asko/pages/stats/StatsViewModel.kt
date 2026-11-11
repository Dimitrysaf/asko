package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.stats.ExerciseStats
import io.github.dimitrysaf.asko.data.stats.PersonalRecord
import io.github.dimitrysaf.asko.data.stats.recordsIn
import io.github.dimitrysaf.asko.data.stats.statsFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

/** A record, with when it was set. */
data class DatedRecord(val record: PersonalRecord, val at: Instant)

/** One exercise you've done and what you've done with it. */
data class ExerciseRow(val exerciseId: String, val stats: ExerciseStats)

data class StatsState(
    /** Every finished workout, newest first (the charts work from it). */
    val history: List<Workout>,
    val latestRecords: List<DatedRecord>,
    /** Every exercise done, most sets first. */
    val mostUsed: List<ExerciseRow>,
)

/** Statistics over the whole history: the charts' data, the latest records, the most used exercises. */
class StatsViewModel(workouts: WorkoutRepository, exercises: ExerciseRepository) : ViewModel() {
    /** null until the history is read. */
    val state: StateFlow<StatsState?> = workouts.history
        .map(::stats)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var library by mutableStateOf<Map<String, Exercise>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            library = try {
                exercises.all().associateBy { it.id }
            } catch (e: IOException) {
                emptyMap()
            }
        }
    }

    private fun stats(history: List<Workout>): StatsState {
        // History is newest first, so the latest records come first.
        val latest = history.asSequence()
            .flatMap { w -> recordsIn(w, history).map { DatedRecord(it, w.startedAt) } }
            .take(LATEST_RECORDS)
            .toList()
        val rows = history.flatMap { w -> w.exercises.map { it.exerciseId } }.distinct()
            .map { ExerciseRow(it, history.statsFor(it)) }
            .sortedByDescending { it.stats.sets }
        return StatsState(history, latest, rows)
    }

    private companion object {
        const val LATEST_RECORDS = 10
    }
}
