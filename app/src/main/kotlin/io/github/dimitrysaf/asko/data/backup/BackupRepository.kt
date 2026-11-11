package io.github.dimitrysaf.asko.data.backup

import androidx.room.withTransaction
import io.github.dimitrysaf.asko.data.local.AskoDatabase
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.model.NoteType
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.data.repository.NoteRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * Backs up the routines and the workout history to a JSON file, and restores them from one. A
 * restore replaces what's there, all or nothing. (The exercise library isn't included: it comes
 * from ExerciseDB again.)
 */
class BackupRepository(
    private val database: AskoDatabase,
    private val routines: RoutineRepository,
    private val workouts: WorkoutRepository,
    private val notes: NoteRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** Writes everything to [output]; returns how many routines and workouts it holds. */
    suspend fun export(output: OutputStream): BackupCounts = withContext(Dispatchers.IO) {
        val backup = BackupFile(
            version = VERSION,
            exportedAtMs = System.currentTimeMillis(),
            routines = routines.routines.first().map {
                BackupRoutine(it.name, it.exercises.map(::backupExercise), backupNotes(NoteSubject.ROUTINE, it.id))
            },
            workouts = workouts.history.first().map { w ->
                BackupWorkout(
                    startedAtMs = w.startedAt.toEpochMilli(),
                    endedAtMs = w.endedAt?.toEpochMilli(),
                    routineName = w.routineName,
                    exercises = w.exercises.map(::backupExercise),
                    notes = backupNotes(NoteSubject.WORKOUT, w.id),
                )
            },
            exerciseNotes = notes.exerciseNotes().mapValues { (_, list) -> list.map(::backupNote) },
        )
        output.bufferedWriter().use { it.write(json.encodeToString(backup)) }
        BackupCounts(backup.routines.size, backup.workouts.size)
    }

    /**
     * Replaces all routines and workouts with the backup in [input]. Throws [IOException] when it
     * isn't an Asko backup (nothing is changed then).
     */
    suspend fun restore(input: InputStream): BackupCounts = withContext(Dispatchers.IO) {
        val backup = try {
            json.decodeFromString<BackupFile>(input.bufferedReader().use { it.readText() })
        } catch (e: SerializationException) {
            throw IOException("Not an Asko backup", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Not an Asko backup", e)
        }
        if (backup.version > VERSION) throw IOException("Made by a newer Asko")
        database.withTransaction {
            database.workoutDao().deleteAll()
            database.routineDao().deleteAll()
            database.noteDao().deleteAll()
            backup.routines.forEach {
                val id = routines.save(Routine(0, it.name, it.exercises.map(::routineExercise)))
                notes.replace(NoteSubject.ROUTINE, id.toString(), it.notes.map(::note))
            }
            backup.workouts.forEach { w ->
                val start = Instant.ofEpochMilli(w.startedAtMs)
                val id = workouts.log(start, w.exercises.map(::routineExercise), Instant.ofEpochMilli(w.endedAtMs ?: w.startedAtMs), w.routineName)
                notes.replace(NoteSubject.WORKOUT, id.toString(), w.notes.map(::note))
            }
            backup.exerciseNotes.forEach { (exerciseId, list) -> notes.replace(NoteSubject.EXERCISE, exerciseId, list.map(::note)) }
        }
        BackupCounts(backup.routines.size, backup.workouts.size)
    }

    private suspend fun backupNotes(subject: NoteSubject, id: Long) = notes.notesNow(subject, id.toString()).map(::backupNote)

    private companion object {
        const val VERSION = 1
    }
}

data class BackupCounts(val routines: Int, val workouts: Int)

private fun backupNote(n: Note) = BackupNote(n.type.name, n.text, n.createdAt.toEpochMilli(), n.editedAt?.toEpochMilli(), n.trainer, n.gym, n.bodyPart, n.painLevel)

private fun note(n: BackupNote) = Note(
    id = 0,
    type = NoteType.entries.firstOrNull { it.name == n.type } ?: NoteType.SELF,
    text = n.text,
    createdAt = Instant.ofEpochMilli(n.createdAtMs),
    editedAt = n.editedAtMs?.let(Instant::ofEpochMilli),
    trainer = n.trainer,
    gym = n.gym,
    bodyPart = n.bodyPart,
    painLevel = n.painLevel,
)

private fun backupExercise(e: RoutineExercise) = BackupExercise(
    exerciseId = e.exerciseId,
    restSeconds = e.restSeconds,
    timed = e.timed,
    repRange = e.repRange,
    supersetWithNext = e.supersetWithNext,
    sets = e.sets.map { BackupSet(it.type.name, it.reps, it.repsMax, it.weightKg, it.durationSeconds) },
)

private fun routineExercise(e: BackupExercise) = RoutineExercise(
    exerciseId = e.exerciseId,
    restSeconds = e.restSeconds,
    timed = e.timed,
    repRange = e.repRange,
    supersetWithNext = e.supersetWithNext,
    sets = e.sets.map {
        PlannedSet(
            type = SetType.entries.firstOrNull { type -> type.name == it.type } ?: SetType.NORMAL,
            reps = it.reps,
            repsMax = it.repsMax,
            weightKg = it.weightKg,
            durationSeconds = it.durationSeconds,
        )
    },
)

// The file's shape. Fields only get added (with defaults), so older backups keep restoring.

@Serializable
private class BackupFile(
    val version: Int,
    val exportedAtMs: Long,
    val routines: List<BackupRoutine> = emptyList(),
    val workouts: List<BackupWorkout> = emptyList(),
    /** Notes on exercises, by exercise id. */
    val exerciseNotes: Map<String, List<BackupNote>> = emptyMap(),
)

@Serializable
private class BackupRoutine(val name: String, val exercises: List<BackupExercise> = emptyList(), val notes: List<BackupNote> = emptyList())

@Serializable
private class BackupWorkout(
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val routineName: String? = null,
    val exercises: List<BackupExercise> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
)

@Serializable
private class BackupNote(
    val type: String = "SELF",
    val text: String = "",
    val createdAtMs: Long = 0,
    val editedAtMs: Long? = null,
    val trainer: String? = null,
    val gym: String? = null,
    val bodyPart: String? = null,
    val painLevel: Int? = null,
)

@Serializable
private class BackupExercise(
    val exerciseId: String,
    val restSeconds: Int = 0,
    val timed: Boolean = false,
    val repRange: Boolean = false,
    val sets: List<BackupSet> = emptyList(),
    val supersetWithNext: Boolean = false,
)

@Serializable
private class BackupSet(
    val type: String = "NORMAL",
    val reps: Int? = null,
    val repsMax: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
)
