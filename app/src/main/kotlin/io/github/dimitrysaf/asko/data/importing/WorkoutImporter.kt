package io.github.dimitrysaf.asko.data.importing

import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.model.NoteType
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.repository.CustomExerciseRepository
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.NoteRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import java.io.IOException
import java.io.InputStream
import java.time.temporal.ChronoUnit

/** What an import did: from which app, workouts added, ones already there, and exercises made for names not found. */
data class ImportCounts(val source: ImportSource, val workouts: Int, val skipped: Int, val newExercises: Int)

/**
 * Brings workouts in from a Strong or Hevy CSV export. Each exercise is matched to one in your
 * libraries by name ("Bench Press (Barbell)" is ExerciseDB's "barbell bench press"); names not
 * found become exercises of yours, once each. Workouts already in History (same start, to the
 * minute) are left out, so importing the same file twice adds nothing.
 */
class WorkoutImporter(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val customExercises: CustomExerciseRepository,
    private val notes: NoteRepository,
    private val settings: SettingsRepository,
) {
    /** Throws [IOException] when the file is neither app's export. */
    suspend fun import(input: InputStream): ImportCounts {
        val text = input.bufferedReader().readText()
        val parsed = parseImport(text, settings.weightUnit.value, settings.distanceUnit.value) ?: throw IOException("Not a Strong or Hevy export")
        val library = try {
            exercises.lookup()
        } catch (e: IOException) {
            exercises.lookupNow().orEmpty()
        }
        // Yours first (an earlier import's made ones are found again), then the libraries'.
        val mine = customExercises.exercises.filterNotNull().first().filterNot { it.retired }
        val byKey = HashMap<String, String>()
        (mine + library.values.filter { it.source != ExerciseSource.CUSTOM }).forEach { byKey.putIfAbsent(nameKey(it.name), it.id) }
        val existing = workouts.history.first().map { it.startedAt.truncatedTo(ChronoUnit.MINUTES) }.toSet()
        val rest = settings.defaultRestSeconds.value

        var added = 0
        var skipped = 0
        var made = 0
        for (workout in parsed.workouts.sortedBy { it.startedAt }) {
            if (workout.exercises.isEmpty()) continue
            if (workout.startedAt.truncatedTo(ChronoUnit.MINUTES) in existing) {
                skipped++
                continue
            }
            val done = workout.exercises.map { exercise ->
                val key = nameKey(exercise.name)
                val id = byKey[key] ?: customExercises.save(newExercise(exercise.name)).also {
                    byKey[key] = it
                    made++
                }
                val distance = exercise.sets.any { (it.meters ?: 0.0) > 0 }
                val timed = !distance && exercise.sets.any { it.seconds != null } && exercise.sets.none { it.reps != null }
                RoutineExercise(
                    exerciseId = id,
                    restSeconds = rest,
                    timed = timed,
                    distance = distance,
                    supersetWithNext = exercise.supersetWithNext,
                    sets = exercise.sets.map { set ->
                        PlannedSet(
                            type = set.type,
                            reps = set.reps.takeUnless { timed || distance },
                            weightKg = set.weightKg.takeUnless { distance },
                            durationSeconds = set.seconds,
                            distanceMeters = set.meters.takeIf { distance },
                        )
                    },
                )
            }
            val id = workouts.log(workout.startedAt, done, endedAt = workout.endedAt ?: workout.startedAt, routineName = workout.name)
            workout.note?.let { notes.add(NoteSubject.WORKOUT, id.toString(), Note(0, NoteType.SELF, it, workout.startedAt)) }
            added++
        }
        return ImportCounts(parsed.source, added, skipped, made)
    }

    /** An exercise of yours for a name no library has: the name only; muscles can be added later. */
    private fun newExercise(name: String) = Exercise(
        id = "",
        name = name.trim(),
        category = "",
        primaryMuscles = emptyList(),
        secondaryMuscles = emptyList(),
        equipment = emptyList(),
        animationUrl = "",
        instructions = emptyList(),
        source = ExerciseSource.CUSTOM,
    )
}

/**
 * A name reduced to its words, in any order and case, without plurals or punctuation, so
 * "Bench Press (Barbell)", "barbell bench press" and "Bench-Press, Barbell" are the same.
 */
internal fun nameKey(name: String): String =
    name.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotEmpty() }
        .map { if (it.length > 3 && it.endsWith("s") && !it.endsWith("ss")) it.dropLast(1) else it }
        .sorted()
        .joinToString(" ")
