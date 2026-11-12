package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.stats.PersonalRecord
import io.github.dimitrysaf.asko.data.stats.recordsIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** One saved workout (just finished, or opened from History), and the exercise library for names and pictures. */
class WorkoutViewModel(
    private val workoutId: Long,
    private val workouts: WorkoutRepository,
    exercises: ExerciseRepository,
) : ViewModel() {
    val workout: StateFlow<Workout?> =
        workouts.workout(workoutId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The records this workout set (none while loading). */
    val records: StateFlow<List<PersonalRecord>> =
        combine(workout, workouts.historyState) { done, history ->
            if (done == null || history == null) emptyList() else recordsIn(done, history)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var library by mutableStateOf<Map<String, Exercise>>(emptyMap())
        private set

    /** Deletes the workout, then [then]. */
    fun delete(then: () -> Unit) {
        viewModelScope.launch {
            workouts.delete(workoutId)
            then()
        }
    }

    init {
        viewModelScope.launch {
            library = try {
                exercises.lookup()
            } catch (e: IOException) {
                emptyMap()
            }
        }
    }
}
