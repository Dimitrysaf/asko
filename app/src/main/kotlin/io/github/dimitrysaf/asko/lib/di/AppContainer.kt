package io.github.dimitrysaf.asko.lib.di

import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.data.backup.BackupRepository
import io.github.dimitrysaf.asko.lib.notify.WorkoutSession
import android.content.Context
import io.github.dimitrysaf.asko.data.catalog.ExerciseCatalog
import io.github.dimitrysaf.asko.data.local.AskoDatabase
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.NoteRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import java.io.File

/** App-wide singletons, built once in [io.github.dimitrysaf.asko.AskoApplication]. */
class AppContainer(context: Context) {
    private val database = AskoDatabase.build(context)

    val exerciseRepository = ExerciseRepository(
        ExerciseCatalog(File(context.filesDir, "exercisedb.json")),
    )

    init {
        // shortcut: removes the unfiltered library saved by older builds (forcing one fresh sync); drop this once your phone has run it.
        File(context.filesDir, "exercises.json").delete()
    }
    /** Notes on routines, exercises and workouts. */
    val noteRepository = NoteRepository(database.noteDao())
    val workoutRepository = WorkoutRepository(database.workoutDao(), noteRepository)
    val routineRepository = RoutineRepository(database.routineDao(), noteRepository)

    val settings = SettingsRepository(context)

    /** Backing up and restoring routines and history. */
    val backupRepository = BackupRepository(database, routineRepository, workoutRepository, noteRepository)

    /** The open workout and its ongoing notification. */
    val workoutSession = WorkoutSession()

    /** The workout in progress, kept on disk so closing the app doesn't lose it. */
    val activeWorkoutStore = ActiveWorkoutStore(File(context.filesDir, "active_workout.json"))
}
