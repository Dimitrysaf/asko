package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.data.model.DayExcuse
import io.github.dimitrysaf.asko.data.model.Streak
import io.github.dimitrysaf.asko.data.repository.StreakRepository
import kotlinx.coroutines.launch
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutSummary
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.model.Workout
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
    private val streaks: StreakRepository,
) : ViewModel() {
    /** The streak, each day's mark, and a lost streak to ask about; null while loading. */
    val streak: StateFlow<Streak?> = streaks.streak

    /** The missed [days] were spent sick or travelling: the streak goes on. */
    fun excuse(days: List<LocalDate>, excuse: DayExcuse) {
        viewModelScope.launch { streaks.excuse(days, excuse) }
    }

    /** The streak is gone; these days aren't asked about again. */
    fun letGo(days: List<LocalDate>) = streaks.letGo(days)

    /** A workout left in progress (the app was closed mid-workout); null when there's none. */
    val activeWorkout: StateFlow<ActiveWorkoutSummary?> =
        activeWorkouts.current.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Days with at least one finished workout; null until the database answers. */
    val workoutDays: StateFlow<Set<LocalDate>?> =
        workouts.history
            .map(::days)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), workouts.historyState.value?.let(::days))

    private fun days(history: List<Workout>) = history.map { it.startedAt.localDate() }.toSet()

    // What's cached first (or empty), so routines show at once; names and thumbnails fill in once
    // the library is read.
    private val library = flow {
        emit(exercises.lookupNow() ?: emptyMap())
        emit(
            try {
                exercises.lookup()
            } catch (e: IOException) {
                emptyMap() // offline before the first sync: routines still show, without exercise names
            },
        )
    }

    /** Saved routines, in their order; null until the database answers. */
    val list: StateFlow<List<RoutineSummary>?> =
        combine(routines.routines, library, ::summaries)
            // Opened again: straight from the cache, no loading.
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                routines.routinesState.value?.let { summaries(it, exercises.lookupNow() ?: emptyMap()) },
            )

    private fun summaries(list: List<Routine>, lib: Map<String, Exercise>) =
        list.map { routine -> RoutineSummary(routine, routine.exercises.mapNotNull { lib[it.exerciseId] }) }

    /** The routines were dragged into a new order (their ids, first to last). */
    fun setOrder(ids: List<Long>) {
        viewModelScope.launch { routines.setOrder(ids) }
    }
}
