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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** One exercise's stats: the history its charts work from, and the exercise for its name. */
class ExerciseStatsViewModel(
    exerciseId: String,
    workouts: WorkoutRepository,
    exercises: ExerciseRepository,
) : ViewModel() {
    /** Every finished workout, newest first; null until read. */
    val history: StateFlow<List<Workout>?> = workouts.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var exercise by mutableStateOf<Exercise?>(null)
        private set

    init {
        viewModelScope.launch {
            exercise = try {
                exercises.byId(exerciseId)
            } catch (e: IOException) {
                null
            }
        }
    }
}
