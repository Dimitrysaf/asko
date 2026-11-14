package io.github.dimitrysaf.asko.lib.di

import io.github.dimitrysaf.asko.data.linked.LinkedLibraryApi
import io.github.dimitrysaf.asko.data.wger.WgerApi
import io.github.dimitrysaf.asko.data.importing.WorkoutImporter
import io.github.dimitrysaf.asko.lib.health.HealthConnectSync

import io.github.dimitrysaf.asko.data.repository.BodyWeightRepository
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
import io.github.dimitrysaf.asko.data.repository.CustomExerciseRepository
import io.github.dimitrysaf.asko.data.repository.MediaRepository
import io.github.dimitrysaf.asko.data.repository.FavouritesRepository
import io.github.dimitrysaf.asko.data.repository.NoteRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.repository.StreakRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import io.github.dimitrysaf.asko.lib.shortcuts.AppShortcuts
import java.io.File

/** App-wide singletons, built once in [io.github.dimitrysaf.asko.AskoApplication]. */
class AppContainer(context: Context) {
    private val database = AskoDatabase.build(context)

    /** Lives as long as the app: what repositories keep cached for every page runs here. */
    /** Work that outlives any page (Undo after a deleted page has closed, syncing). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(context)

    /** The exercises you made, and your photos and videos on exercises. */
    val customExerciseRepository = CustomExerciseRepository(database.customExerciseDao(), appScope)
    val mediaRepository = MediaRepository(context, database.exerciseMediaDao(), appScope)

    /** ExerciseDB, free-exercise-db, wger and the libraries you add, each saved on the device; merged as Settings says. */
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
            ExerciseSource.WGER to ExerciseCatalog(
                File(context.filesDir, "wger.json"),
                expectedTotal = WgerApi.EXPECTED_TOTAL,
            ) { onProgress -> WgerApi.fetchExercises(onProgress) },
        ),
        // A library you added: read from its link, whichever format it's in.
        { library ->
            ExerciseCatalog(File(context.filesDir, "library-${library.id}.json"), expectedTotal = 1) { _ ->
                LinkedLibraryApi.fetchExercises(
                    url = library.url,
                    imagesBase = FreeExerciseDbApi.imagesBaseFor(library.url),
                    idPrefix = "link-${library.id}:",
                    source = library.source,
                )
            }
        },
        settings,
        customExerciseRepository.exercises,
    )


    init {
        // shortcut: removes the unfiltered library saved by older builds (forcing one fresh sync); drop this once your phone has run it.
        File(context.filesDir, "exercises.json").delete()
    }
    /** Notes on routines, exercises and workouts. */
    val noteRepository = NoteRepository(database.noteDao())
    val workoutRepository = WorkoutRepository(database.workoutDao(), noteRepository, appScope)
    val routineRepository = RoutineRepository(database.routineDao(), noteRepository, appScope)

    /** Your body weight over time. */
    val bodyWeightRepository = BodyWeightRepository(database.bodyWeightDao(), appScope)

    /** Exercises starred as favourites. */
    val favouritesRepository = FavouritesRepository(context)

    /** The workout streak, with sick and travel days. */
    val streakRepository = StreakRepository(workoutRepository, database.dayExcuseDao(), settings, appScope)


    /** Backing up and restoring routines and history. */
    val backupRepository = BackupRepository(database, routineRepository, workoutRepository, noteRepository, streakRepository, favouritesRepository, customExerciseRepository, bodyWeightRepository)

    /** Workouts brought in from a Strong or Hevy CSV export. */
    val workoutImporter = WorkoutImporter(workoutRepository, exerciseRepository, customExerciseRepository, noteRepository, settings)

    /** Workouts and body weight written to Health Connect, when Settings has it on. */
    val healthConnect = HealthConnectSync(context, settings, workoutRepository, bodyWeightRepository, appScope)

    /** The open workout and its ongoing notification. */
    val workoutSession = WorkoutSession()

    /** The workout in progress, kept on disk so closing the app doesn't lose it. */
    val activeWorkoutStore = ActiveWorkoutStore(File(context.filesDir, "active_workout.json"))

    /** The launcher's shortcuts, kept in step with the routines and the workout in progress. */
    private val appShortcuts = AppShortcuts(context, routineRepository, activeWorkoutStore, appScope)
}
