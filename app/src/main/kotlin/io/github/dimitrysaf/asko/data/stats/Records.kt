package io.github.dimitrysaf.asko.data.stats

import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.Workout
import java.time.Instant

/** The kinds of personal record kept for an exercise. */
enum class RecordKind {
    /** The most weight in one set (added weight, for bodyweight exercises). */
    HEAVIEST,

    /** The most reps in one set. */
    MOST_REPS,

    /** The best estimated one-rep max (Epley: weight × (1 + reps / 30)), from sets of up to 12 reps. */
    ONE_REP_MAX,

    /** The longest time a timed set was held, in seconds. */
    LONGEST_HOLD,

    /** The longest distance in one set, in metres. */
    LONGEST_DISTANCE,

    /** The fastest distance set: its speed, in metres a second (shown as the time per km or mile). */
    FASTEST_PACE,
}

/** A record set in a workout: [value] beat the best before it, [previous]. */
data class PersonalRecord(val exerciseId: String, val kind: RecordKind, val value: Double, val previous: Double)

/** What you've done with one exercise, over all your workouts. Bests are null when there's none. */
data class ExerciseStats(
    val bests: Map<RecordKind, Double>,
    val sessions: Int,
    val sets: Int,
    val lastDone: Instant?,
)

/** One set's value for a record kind, or null when it doesn't count for it. */
fun PlannedSet.valueFor(kind: RecordKind): Double? = when (kind) {
    RecordKind.HEAVIEST -> weightKg?.takeIf { it > 0 && (reps ?: 0) > 0 }
    RecordKind.MOST_REPS -> reps?.takeIf { it > 0 }?.toDouble()
    RecordKind.ONE_REP_MAX -> {
        val kg = weightKg
        val r = reps
        if (kg == null || kg <= 0 || r == null || r <= 0 || r > MAX_REPS_FOR_ONE_REP_MAX) null else kg * (1 + r / 30.0)
    }
    // A distance set's time is how long the distance took, not a hold.
    RecordKind.LONGEST_HOLD -> durationSeconds?.takeIf { it > 0 && distanceMeters == null }?.toDouble()
    RecordKind.LONGEST_DISTANCE -> distanceMeters?.takeIf { it > 0 }
    RecordKind.FASTEST_PACE -> {
        val m = distanceMeters
        val s = durationSeconds
        if (m == null || s == null || m <= 0 || s <= 0) null else m / s
    }
}

private const val MAX_REPS_FOR_ONE_REP_MAX = 12

/** The best of each record kind across these sets. */
private fun List<PlannedSet>.bests(): Map<RecordKind, Double> =
    RecordKind.entries.mapNotNull { kind -> mapNotNull { it.valueFor(kind) }.maxOrNull()?.let { kind to it } }.toMap()

/** Every set done of [exerciseId] in these workouts. */
private fun List<Workout>.setsOf(exerciseId: String): List<PlannedSet> =
    flatMap { w -> w.exercises.filter { it.exerciseId == exerciseId }.flatMap { it.sets } }

/** Statistics for [exerciseId] across these workouts. */
fun List<Workout>.statsFor(exerciseId: String): ExerciseStats {
    val done = filter { w -> w.exercises.any { it.exerciseId == exerciseId } }
    val sets = done.setsOf(exerciseId)
    return ExerciseStats(
        bests = sets.bests(),
        sessions = done.size,
        sets = sets.size,
        lastDone = done.maxOfOrNull { it.startedAt },
    )
}

/**
 * The records [workout] set: each exercise's bests in it that beat its bests in the workouts done
 * before it ([history] may include later ones and the workout itself; they're left out). An
 * exercise done for the first time sets none: there's nothing to beat yet.
 */
fun recordsIn(workout: Workout, history: List<Workout>): List<PersonalRecord> {
    val before = history.filter { it.id != workout.id && it.startedAt < workout.startedAt }
    return workout.exercises.map { it.exerciseId }.distinct().flatMap { id ->
        val earlier = before.setsOf(id).bests()
        if (earlier.isEmpty()) return@flatMap emptyList()
        val now = listOf(workout).setsOf(id).bests()
        now.mapNotNull { (kind, value) ->
            val previous = earlier[kind] ?: return@mapNotNull null
            if (value > previous) PersonalRecord(id, kind, value, previous) else null
        }
    }
}

/** A record beaten: [kind] went from [previous] to [value] in the workout started [at]. */
data class RecordEvent(val kind: RecordKind, val value: Double, val previous: Double, val at: Instant)

/**
 * Every record [exerciseId] has set, newest first: each time one of its bests was beaten (the
 * first time it was done only sets the bar, like [recordsIn]).
 */
fun List<Workout>.recordHistory(exerciseId: String): List<RecordEvent> {
    val bests = mutableMapOf<RecordKind, Double>()
    val events = mutableListOf<RecordEvent>()
    sortedBy { it.startedAt }.forEach { w ->
        val now = listOf(w).setsOf(exerciseId).bests()
        now.forEach { (kind, value) ->
            val before = bests[kind]
            if (before != null && value > before) events += RecordEvent(kind, value, before, w.startedAt)
            if (before == null || value > before) bests[kind] = value
        }
    }
    return events.reversed()
}

/** The heaviest set of [reps] reps, and when. */
data class RepMax(val reps: Int, val weightKg: Double, val at: Instant)

/**
 * For each rep count from 1 to 12 that [exerciseId] has been done for with a weight, the heaviest
 * such set (the first time it was lifted, if it was matched later).
 */
fun List<Workout>.repMaxes(exerciseId: String): List<RepMax> {
    val best = sortedMapOf<Int, RepMax>()
    sortedBy { it.startedAt }.forEach { w ->
        w.exercises.filter { it.exerciseId == exerciseId }.flatMap { it.sets }.forEach { set ->
            val reps = set.reps ?: return@forEach
            val kg = set.weightKg ?: return@forEach
            if (reps !in 1..MAX_REPS_FOR_ONE_REP_MAX || kg <= 0) return@forEach
            if (kg > (best[reps]?.weightKg ?: 0.0)) best[reps] = RepMax(reps, kg, w.startedAt)
        }
    }
    return best.values.toList()
}
