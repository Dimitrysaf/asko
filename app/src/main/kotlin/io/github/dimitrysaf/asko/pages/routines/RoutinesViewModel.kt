package io.github.dimitrysaf.asko.pages.routines

import kotlinx.coroutines.launch
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutSummary
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.lib.time.localDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.time.LocalDate

/** A routine with its exercises looked up in the library, for display. */
data class RoutineSummary(val routine: Routine, val exercises: List<Exercise>)

class RoutinesViewModel(
    workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    exercises: ExerciseRepository,
    private val activeWorkouts: ActiveWorkoutStore,
) : ViewModel() {
    /** A workout left in progress (the app was closed mid-workout); null when there's none. */
    val activeWorkout: StateFlow<ActiveWorkoutSummary?> =
        activeWorkouts.current.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Days with at least one finished workout; null until the database answers. */
    val workoutDays: StateFlow<Set<LocalDate>?> =
        workouts.history
            .map { history -> history.map { it.startedAt.localDate() }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Empty first, so routines show at once; names and thumbnails fill in once the library is read.
    private val library = flow {
        emit(emptyMap())
        emit(
            try {
                exercises.all().associateBy { it.id }
            } catch (e: IOException) {
                emptyMap() // offline before the first sync: routines still show, without exercise names
            },
        )
    }

    /** Saved routines, in their order; null until the database answers. */
    val list: StateFlow<List<RoutineSummary>?> =
        combine(routines.routines, library) { list, lib ->
            list.map { routine -> RoutineSummary(routine, routine.exercises.mapNotNull { lib[it.exerciseId] }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The routines were dragged into a new order (their ids, first to last). */
    fun setOrder(ids: List<Long>) {
        viewModelScope.launch { routines.setOrder(ids) }
    }
}
