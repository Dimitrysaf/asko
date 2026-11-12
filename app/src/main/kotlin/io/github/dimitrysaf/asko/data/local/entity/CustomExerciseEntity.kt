package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An exercise you made (the "Your exercises" library). Lists are stored one item per line.
 * Added in DB version 10.
 */
@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    /** "custom:" and a random id; stable, so routines and workouts can point at it. */
    @PrimaryKey val id: String,
    val name: String,
    /** Its muscle group (a library category), from its first main muscle. */
    val category: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val equipment: String,
    val instructions: String,
    /** Deleted: no longer browsed, but still known to the routines and workouts that have it. */
    val retired: Boolean = false,
    val createdAtMs: Long,
)

/** A photo, GIF or video you added to an exercise (any library's). Added in DB version 10. */
@Entity(tableName = "exercise_media", indices = [Index("exerciseId")])
data class ExerciseMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    /** The copy in the app's files ("media/…"). */
    val file: String,
    /** [io.github.dimitrysaf.asko.data.model.MediaType] name. */
    val type: String,
    /** A video's first frame, for lists ("media/…"); null for images. */
    val poster: String?,
    val addedAtMs: Long,
)
