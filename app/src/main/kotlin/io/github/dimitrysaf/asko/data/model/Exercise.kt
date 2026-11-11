package io.github.dimitrysaf.asko.data.model

import kotlinx.serialization.Serializable

/**
 * An exercise from ExerciseDB, saved as JSON on the device.
 *
 * Fields have no defaults on purpose: a library saved by an older version (or from wger) fails
 * to read and is downloaded again.
 */
@Serializable
data class Exercise(
    /** ExerciseDB's id: stable across syncs, so workout logs can point at it. */
    val id: String,
    val name: String,
    /** Body part, e.g. "Upper Legs". The Muscle group filter. */
    val category: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    /** Animated GIF of the full movement. */
    val animationUrl: String,
    /** Form cues, in order. */
    val instructions: List<String>,
)
