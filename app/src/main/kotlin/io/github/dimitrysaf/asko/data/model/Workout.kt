package io.github.dimitrysaf.asko.data.model

import java.time.Duration
import java.time.Instant

/**
 * A workout that was done: when, and its exercises with the sets as they were done. Exercises and
 * sets have the same shape as a routine's ([RoutineExercise], [PlannedSet]), so logging is the
 * routine editor filled in with what you did.
 */
data class Workout(
    val id: Long,
    val startedAt: Instant,
    /** Null while the workout is still in progress. */
    val endedAt: Instant?,
    val note: String?,
    /** The routine it was done from, as named then; null when logged without one. */
    val routineName: String?,
    val exercises: List<RoutineExercise>,
) {
    /** How long it took; null when unknown (in progress, or logged afterwards without a duration). */
    val duration: Duration? get() = endedAt?.let { Duration.between(startedAt, it) }?.takeUnless { it.isZero }

    val setCount: Int get() = exercises.sumOf { it.sets.size }
}

/** Logged with nothing in it: no reps (no time held, for a timed set; no distance, for a distance set). */
fun PlannedSet.isEmptyLog(timed: Boolean, distance: Boolean = false): Boolean = when {
    distance -> (distanceMeters ?: 0.0) <= 0
    timed -> (durationSeconds ?: 0) <= 0
    else -> (reps ?: 0) <= 0
}

/** Every set of it was logged empty (see [isEmptyLog]): nothing was done. */
val Workout.nothingDone: Boolean get() = exercises.all { e -> e.sets.all { it.isEmptyLog(e.timed, e.distance) } }
