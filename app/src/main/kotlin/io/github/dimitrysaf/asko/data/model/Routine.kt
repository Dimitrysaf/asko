package io.github.dimitrysaf.asko.data.model

/** A saved workout plan: exercises in order, each with its planned sets and rest. */
data class Routine(
    /** 0 for a routine not saved yet. */
    val id: Long,
    val name: String,
    val exercises: List<RoutineExercise>,
    /** The folder it's kept in on the Routines tab (a split: "Push / Pull / Legs"); null for none. */
    val folder: String? = null,
)

data class RoutineExercise(
    /** ExerciseDB exercise id. */
    val exerciseId: String,
    /** Rest after each set; 0 = no timer. */
    val restSeconds: Int,
    /** Sets are held for a time (plank, cardio) instead of counted in reps. */
    val timed: Boolean,
    /** Reps are planned as a range (8–12): [PlannedSet.reps] is the low end, [PlannedSet.repsMax] the high. */
    val repRange: Boolean = false,
    val sets: List<PlannedSet>,
    /**
     * Done as a superset with the exercise after it: their sets alternate, and the rest comes after
     * each round (the last exercise of the superset's rest). Ignored on the last exercise.
     */
    val supersetWithNext: Boolean = false,
    /** Sets are covered over a distance (running, rowing), with the time it took: no reps or weight. */
    val distance: Boolean = false,
)

/**
 * The exercises in runs: each superset (exercises linked by [linked], "with the next") is one run,
 * every other exercise a run of its own. Runs are index ranges, in order.
 */
fun <T> List<T>.supersetRuns(linked: (T) -> Boolean): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    var start = 0
    for (i in indices) {
        if (i == lastIndex || !linked(this[i])) {
            runs += start..i
            start = i + 1
        }
    }
    return runs
}

/** Exercises with some taken out: a superset closes up around them, and the last links to nothing. */
fun List<RoutineExercise>.withoutSupersetGaps(): List<RoutineExercise> =
    mapIndexed { i, e -> if (i == lastIndex) e.copy(supersetWithNext = false) else e }

/** One planned set. Null targets mean "no target". */
data class PlannedSet(
    val type: SetType,
    val reps: Int? = null,
    /** Top of the rep range, when the exercise uses one. */
    val repsMax: Int? = null,
    /** Total weight moved, e.g. 8 for two 4 kg dumbbells; for bodyweight moves, the added weight. */
    val weightKg: Double? = null,
    /** Target time for timed exercises; for distance ones, the time the distance took. */
    val durationSeconds: Int? = null,
    /** Distance covered, in metres, for distance exercises. */
    val distanceMeters: Double? = null,
)

/** What a set is for. Marked by hand; only [NORMAL] sets are numbered. */
enum class SetType {
    NORMAL,
    WARMUP,
    DROP,
    FAILURE,

    /** An easy set to wind down, the counterpart of a warm-up. */
    FINISHER,
}
