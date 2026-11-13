package io.github.dimitrysaf.asko.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import io.github.dimitrysaf.asko.data.local.dao.WorkoutDao
import io.github.dimitrysaf.asko.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.first
import io.github.dimitrysaf.asko.data.local.toModel
import io.github.dimitrysaf.asko.data.local.toWorkoutEntities
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class WorkoutRepository(private val dao: WorkoutDao, private val notes: NoteRepository, scope: CoroutineScope) {
    /**
     * Finished workouts, newest first, kept up to date and cached for as long as the app runs, so
     * a page opened again shows them at once; null until first read.
     */
    val historyState: StateFlow<List<Workout>?> =
        dao.observeFinished().map { list -> list.map { it.toModel() } }.stateIn(scope, SharingStarted.Lazily, null)

    /** Finished workouts, newest first (from the cache). */
    val history: Flow<List<Workout>> = historyState.filterNotNull()

    /**
     * The exercises done most recently, last done first, each once (up to [RECENT_EXERCISES]);
     * null until the history is first read.
     */
    val recentExerciseIdsState: StateFlow<List<String>?> =
        historyState.map { it?.recentExerciseIds() }.stateIn(scope, SharingStarted.Lazily, null)

    /** Saves an edited workout: when it started and ended, and what was done. */
    suspend fun update(id: Long, startedAt: Instant, endedAt: Instant, exercises: List<RoutineExercise>) =
        dao.replace(id, startedAt.toEpochMilli(), endedAt.toEpochMilli(), exercises.toWorkoutEntities())

    /**
     * Deletes the workout and its notes. Returns what puts it all back as it was (same id, times,
     * routine name, exercises and notes), for Undo.
     */
    suspend fun delete(id: Long): suspend () -> Unit {
        val workout = dao.observe(id).first()?.toModel()
        val saved = notes.notesNow(NoteSubject.WORKOUT, id.toString())
        dao.delete(id)
        notes.deleteFor(NoteSubject.WORKOUT, id.toString())
        return {
            if (workout != null) {
                dao.insertLogged(
                    WorkoutEntity(
                        id = workout.id,
                        startedAt = workout.startedAt.toEpochMilli(),
                        endedAt = workout.endedAt?.toEpochMilli(),
                        note = workout.note,
                        routineName = workout.routineName,
                    ),
                    workout.exercises.toWorkoutEntities(),
                )
                notes.replace(NoteSubject.WORKOUT, id.toString(), saved)
            }
        }
    }

    /** One workout; null once it no longer exists. */
    fun workout(id: Long): Flow<Workout?> = dao.observe(id).map { it?.toModel() }

    /** The workout in progress, if any. */
    val activeWorkout: Flow<Workout?> = dao.observeActive().map { it?.toModel() }

    /** Resumes the unfinished workout, or starts one at [now]. */
    suspend fun resumeOrStartWorkout(now: Instant = Instant.now()): Long =
        dao.activeOrNewWorkoutId(now.toEpochMilli())

    suspend fun finishWorkout(id: Long, now: Instant = Instant.now()) =
        dao.finishWorkout(id, now.toEpochMilli())

    /**
     * Stores a finished workout that started at [at] with these [exercises] as they were done.
     * Without [endedAt] (logged afterwards) it ends when it started: the duration shows as unknown.
     * [routineName] is the routine it was done from, if any.
     */
    suspend fun log(at: Instant, exercises: List<RoutineExercise>, endedAt: Instant = at, routineName: String? = null): Long =
        dao.insertLogged(
            WorkoutEntity(startedAt = at.toEpochMilli(), endedAt = endedAt.toEpochMilli(), routineName = routineName),
            exercises.toWorkoutEntities(),
        )
}

/** How many exercises "Recently used" lists. */
private const val RECENT_EXERCISES = 5

/** From the newest workout back ([this] is newest first), each exercise once. */
private fun List<Workout>.recentExerciseIds(): List<String> =
    asSequence().flatMap { w -> w.exercises.asSequence().map { it.exerciseId } }.distinct().take(RECENT_EXERCISES).toList()
