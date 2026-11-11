package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.data.session.ActiveWorkoutSummary
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface RoutineState {
    data object Loading : RoutineState
    data object NotFound : RoutineState
    data class Loaded(val routine: Routine, val library: Map<String, Exercise>) : RoutineState
}

class RoutineViewModel(
    private val routineId: Long,
    private val routines: RoutineRepository,
    exercises: ExerciseRepository,
    private val activeWorkouts: ActiveWorkoutStore,
) : ViewModel() {
    /** A workout in progress, if any: Start resumes it, or asks first when it's another routine's. */
    val activeWorkout: StateFlow<ActiveWorkoutSummary?> =
        activeWorkouts.current.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Throws away the workout in progress, then [then]. */
    fun discardActiveWorkout(then: () -> Unit) {
        viewModelScope.launch {
            activeWorkouts.clear()
            then()
        }
    }

    private val library = flow {
        emit(
            try {
                exercises.lookup()
            } catch (e: IOException) {
                emptyMap()
            },
        )
    }

    /** Follows the routine, so edits show up when returning from the editor. */
    val state: StateFlow<RoutineState> =
        combine(routines.routine(routineId), library) { routine, lib ->
            if (routine == null) RoutineState.NotFound else RoutineState.Loaded(routine, lib)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineState.Loading)

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            routines.delete(routineId)
            onDeleted()
        }
    }
}
