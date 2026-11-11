package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis. */
    val startedAt: Long,
    /** Epoch millis; null while the workout is still in progress. */
    val endedAt: Long? = null,
    val note: String? = null,
    /** The routine it was done from, as named then; null when logged without one. Added in DB version 6. */
    val routineName: String? = null,
)

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithSets>,
)
