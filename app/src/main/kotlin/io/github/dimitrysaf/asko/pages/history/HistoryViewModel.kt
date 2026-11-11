package io.github.dimitrysaf.asko.pages.history

import io.github.dimitrysaf.asko.data.model.Streak
import io.github.dimitrysaf.asko.data.repository.StreakRepository
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

class HistoryViewModel(workouts: WorkoutRepository, exercises: ExerciseRepository, streaks: StreakRepository) : ViewModel() {
    /** The streak and each day's mark for the calendar; null while loading. */
    val streak: StateFlow<Streak?> = streaks.streak

    /** Finished workouts, newest first; null while loading. */
    val history: StateFlow<List<Workout>?> = workouts.historyState

    /** The exercise library, for names and pictures; empty until loaded (or offline before a sync). */
    var library by mutableStateOf(exercises.lookupNow() ?: emptyMap())
        private set

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
