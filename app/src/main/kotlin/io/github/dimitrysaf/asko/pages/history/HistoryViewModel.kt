package io.github.dimitrysaf.asko.pages.history

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

class HistoryViewModel(workouts: WorkoutRepository, exercises: ExerciseRepository) : ViewModel() {
    /** Finished workouts, newest first; null while loading. */
    val history: StateFlow<List<Workout>?> =
        workouts.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The exercise library, for names and pictures; empty until loaded (or offline before a sync). */
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
}
