package io.github.dimitrysaf.asko.lib.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.data.backup.BackupRepository
import io.github.dimitrysaf.asko.lib.notify.WorkoutSession
import android.content.Context
import io.github.dimitrysaf.asko.data.catalog.ExerciseCatalog
import io.github.dimitrysaf.asko.data.exercisedb.ExerciseDbApi
import io.github.dimitrysaf.asko.data.freeexercisedb.FreeExerciseDbApi
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.local.AskoDatabase
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.NoteRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.repository.StreakRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import java.io.File

/** App-wide singletons, built once in [io.github.dimitrysaf.asko.AskoApplication]. */
class AppContainer(context: Context) {
    private val database = AskoDatabase.build(context)

    /** Lives as long as the app: what repositories keep cached for every page runs here. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(context)

    /** ExerciseDB and free-exercise-db, each saved on the device; merged as Settings says. */
    val exerciseRepository = ExerciseRepository(
        mapOf(
            ExerciseSource.EXERCISEDB to ExerciseCatalog(
                File(context.filesDir, "exercisedb.json"),
                expectedTotal = ExerciseDbApi.EXPECTED_TOTAL * 2,
            ) { onProgress -> ExerciseDbApi.fetchExercises(onProgress) },
            ExerciseSource.FREE_EXERCISE_DB to ExerciseCatalog(
                File(context.filesDir, "free-exercise-db.json"),
                expectedTotal = 1,
            ) { _ -> FreeExerciseDbApi.fetchExercises() },
        ),
        settings,
    )


    init {
        // shortcut: removes the unfiltered library saved by older builds (forcing one fresh sync); drop this once your phone has run it.
        File(context.filesDir, "exercises.json").delete()
    }
    /** Notes on routines, exercises and workouts. */
    val noteRepository = NoteRepository(database.noteDao())
    val workoutRepository = WorkoutRepository(database.workoutDao(), noteRepository, appScope)
    val routineRepository = RoutineRepository(database.routineDao(), noteRepository, appScope)

    /** The workout streak, with sick and travel days. */
    val streakRepository = StreakRepository(workoutRepository, database.dayExcuseDao(), settings, appScope)


    /** Backing up and restoring routines and history. */
    val backupRepository = BackupRepository(database, routineRepository, workoutRepository, noteRepository, streakRepository)

    /** The open workout and its ongoing notification. */
    val workoutSession = WorkoutSession()

    /** The workout in progress, kept on disk so closing the app doesn't lose it. */
    val activeWorkoutStore = ActiveWorkoutStore(File(context.filesDir, "active_workout.json"))
}
