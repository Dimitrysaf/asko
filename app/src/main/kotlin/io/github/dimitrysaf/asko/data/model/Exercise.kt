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
    /** One of yours that you deleted: not browsed, but still shown where it was done. */
    val retired: Boolean = false,
) {
    /** One picture for lists: the animation (its first frame is shown) or the first photo. */
    val stillUrl: String get() = animationUrl.ifEmpty { photos.firstOrNull().orEmpty() }
}

/**
 * Where exercises come from: a built-in library, your own exercises, or a library you added from
 * a link ([link]). Their order and which are shown are set in Settings. Saved as its [name]
 * ("EXERCISEDB", "LINK:3f2a…"), so libraries saved before this was open-ended still read.
 */
@Serializable
@JvmInline
value class ExerciseSource(val name: String) {
    /** A library you added from a link, rather than one Asko comes with. */
    val isLink: Boolean get() = name.startsWith(LINK_PREFIX)

    /** For a library you added: its id among your links. */
    val linkId: String? get() = if (isLink) name.removePrefix(LINK_PREFIX) else null

    companion object {
        /** ExerciseDB: animated demos. */
        val EXERCISEDB = ExerciseSource("EXERCISEDB")

        /** free-exercise-db: two photos each, start and end position. */
        val FREE_EXERCISE_DB = ExerciseSource("FREE_EXERCISE_DB")

        /** wger's community library: photos for some, and how-tos. Off until you switch it on. */
        val WGER = ExerciseSource("WGER")

        /** Exercises you made, drawn from the muscles they work. */
        val CUSTOM = ExerciseSource("CUSTOM")

        /** The libraries Asko comes with (yours included). */
        val BUILT_IN = listOf(CUSTOM, EXERCISEDB, FREE_EXERCISE_DB, WGER)

        private const val LINK_PREFIX = "LINK:"

        /** The library you added from a link, by its id. */
        fun link(id: String) = ExerciseSource(LINK_PREFIX + id)
    }
}
