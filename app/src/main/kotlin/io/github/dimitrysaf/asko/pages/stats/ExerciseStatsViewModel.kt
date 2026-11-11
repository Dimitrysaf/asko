package io.github.dimitrysaf.asko.pages.stats

import kotlinx.coroutines.flow.combine
import io.github.dimitrysaf.asko.data.repository.asShown
import io.github.dimitrysaf.asko.data.repository.lookupsOrEmpty
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
    /**
     * Every finished workout, newest first, each exercise under the id of its version shown (so
     * its versions in each library count as one); null until read.
     */
    val history: StateFlow<List<Workout>?> = combine(workouts.history, exercises.lookupsOrEmpty()) { history, lookup -> history.asShown(lookup) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
