package io.github.dimitrysaf.asko.data.model

import kotlinx.serialization.Serializable

/**
 * An exercise from one of the libraries ([source]), saved as JSON on the device.
 *
 * Most fields have no defaults on purpose: a library saved by an older version (or from wger)
 * fails to read and is downloaded again. [source] and [photos] came later and have defaults, so
 * a saved ExerciseDB library still reads.
 */
@Serializable
data class Exercise(
    /** The library's id (free-exercise-db's start "fedb:"): stable across syncs, so workout logs can point at it. */
    val id: String,
    val name: String,
    /** Body part, e.g. "Upper Legs". The Muscle group filter. */
    val category: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    /** Animated GIF of the full movement; empty when the library has [photos] instead. */
    val animationUrl: String,
    /** Form cues, in order. */
    val instructions: List<String>,
    val source: ExerciseSource = ExerciseSource.EXERCISEDB,
    /** Photos of the movement in order (free-exercise-db: start and end position). */
    val photos: List<String> = emptyList(),
) {
    /** One picture for lists: the animation (its first frame is shown) or the first photo. */
    val stillUrl: String get() = animationUrl.ifEmpty { photos.firstOrNull().orEmpty() }
}

/** Where exercises come from. Their order and which are shown are set in Settings. */
@Serializable
enum class ExerciseSource {
    /** ExerciseDB: animated demos. */
    EXERCISEDB,

    /** free-exercise-db: two photos each, start and end position. */
    FREE_EXERCISE_DB,
}
