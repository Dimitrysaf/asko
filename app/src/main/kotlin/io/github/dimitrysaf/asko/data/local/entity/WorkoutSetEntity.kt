package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// A workout's exercises and sets, shaped like a routine's (RoutineEntity.kt). Since DB version 5.

/** One exercise in a workout. Its sets are [WorkoutSetEntity] rows. */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    /** ExerciseDB exercise id. */
    val exerciseId: String,
    /** Order within the workout. */
    val position: Int,
    /** Rest after each set; 0 = no timer. */
    val restSeconds: Int,
    /** Sets were held for a time instead of counted in reps. */
    val timed: Boolean,
    /** Reps are a range; the sets' repsMax is the top. */
    val repRange: Boolean,
    /** Done as a superset with the exercise after it. Added in DB version 7. */
    @ColumnInfo(defaultValue = "0") val supersetWithNext: Boolean = false,
    /** Sets were a distance and a time. Added in DB version 11. */
    @ColumnInfo(defaultValue = "0") val distance: Boolean = false,
)

/** One set of a workout exercise. */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    /** [io.github.dimitrysaf.asko.data.model.SetType] name. */
    val type: String,
    val reps: Int?,
    val repsMax: Int?,
    val weightKg: Double?,
    val durationSeconds: Int?,
    /** Metres. Added in DB version 11. */
    val distanceMeters: Double? = null,
)

data class WorkoutExerciseWithSets(
    @Embedded val exercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSetEntity>,
)
