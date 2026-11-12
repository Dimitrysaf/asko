package io.github.dimitrysaf.asko.data.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * The workout in progress, saved to [file] after every change so it survives the app being closed
 * (Android ends apps in the background). Only one at a time; it's cleared when the workout is
 * finished or discarded.
 */
class ActiveWorkoutStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val summary = MutableStateFlow<ActiveWorkoutSummary?>(null)
    private var read = false

    /** Which workout is in progress, if any (for Resume on the Routines tab). */
    val current: Flow<ActiveWorkoutSummary?> = flow {
        mutex.withLock { if (!read) readLocked() }
        emitAll(summary)
    }

    /** The saved workout, or null when none is in progress. */
    suspend fun load(): SavedWorkout? = mutex.withLock { readLocked() }

    suspend fun save(workout: SavedWorkout) = mutex.withLock {
        withContext(Dispatchers.IO) {
            // A temp file first, so a crash mid-write never leaves a half-written workout.
            val temp = File(file.path + ".tmp")
            temp.writeText(json.encodeToString(workout))
            temp.renameTo(file)
        }
        summary.value = workout.summary()
        read = true
    }

    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) { file.delete() }
        summary.value = null
        read = true
    }

    private suspend fun readLocked(): SavedWorkout? {
        val saved = withContext(Dispatchers.IO) {
            if (!file.exists()) {
                null
            } else {
                try {
                    json.decodeFromString<SavedWorkout>(file.readText())
                } catch (e: SerializationException) {
                    null // written by an older app version: nothing to resume
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
        summary.value = saved?.summary()
        read = true
        return saved
    }
}

data class ActiveWorkoutSummary(val routineId: Long, val routineName: String, val startedAt: Instant)

private fun SavedWorkout.summary() = ActiveWorkoutSummary(routineId, routineName, Instant.ofEpochMilli(startedAtMs))

/**
 * Everything to carry on a workout. [elapsedMs] and the countdown's remaining time are as of
 * [savedAtMs] (wall clock), so the time since then can be added back when it's restored.
 */
@Serializable
data class SavedWorkout(
    val routineId: Long,
    val routineName: String,
    val startedAtMs: Long,
    val elapsedMs: Long,
    val paused: Boolean,
    val exerciseIndex: Int,
    val setIndex: Int,
    val restWithinExercise: Boolean,
    val countdown: SavedCountdown?,
    val exercises: List<SavedExercise>,
    val savedAtMs: Long,
)

@Serializable
data class SavedCountdown(val kind: String, val totalMs: Long, val remainingMs: Long)

@Serializable
data class SavedExercise(
    val exerciseId: String,
    val restSeconds: Int,
    val timed: Boolean,
    val repRange: Boolean,
    val sets: List<SavedSet>,
    val supersetWithNext: Boolean = false,
    val distance: Boolean = false,
)

@Serializable
data class SavedSet(val target: SavedValues, val actual: SavedValues, val status: String)

@Serializable
data class SavedValues(
    val type: String,
    val reps: Int? = null,
    val repsMax: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
)
