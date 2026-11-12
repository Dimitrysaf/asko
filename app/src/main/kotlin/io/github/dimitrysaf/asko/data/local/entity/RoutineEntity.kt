package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Place in the Routines list (set by dragging). Added in DB version 8. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
    /** The folder it's kept in; null for none. Added in DB version 12. */
    val folder: String? = null,
)

/** One exercise in a routine. Its planned sets are [RoutineSetEntity] rows. */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    /** ExerciseDB exercise id. */
    val exerciseId: String,
    /** Order within the routine. */
    val position: Int,
    /** Rest after each set; 0 = no timer. */
    val restSeconds: Int,
    /** Sets are held for a time instead of counted in reps. */
    val timed: Boolean,
    /** Reps are planned as a range; the sets' repsMax is the top. Added in DB version 4. */
    @ColumnInfo(defaultValue = "0") val repRange: Boolean = false,
    /** A superset with the exercise after it. Added in DB version 7. */
    @ColumnInfo(defaultValue = "0") val supersetWithNext: Boolean = false,
    /** Sets are a distance and a time. Added in DB version 11. */
    @ColumnInfo(defaultValue = "0") val distance: Boolean = false,
)

/** One planned set of a routine exercise. */
@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineExerciseId")],
)
data class RoutineSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineExerciseId: Long,
    val position: Int,
    /** [io.github.dimitrysaf.asko.data.model.SetType] name. */
    val type: String,
    val reps: Int?,
    /** Top of the rep range. Added in DB version 4. */
    val repsMax: Int? = null,
    val weightKg: Double?,
    val durationSeconds: Int?,
    /** Metres. Added in DB version 11. */
    val distanceMeters: Double? = null,
)

data class RoutineExerciseWithSets(
    @Embedded val exercise: RoutineExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "routineExerciseId")
    val sets: List<RoutineSetEntity>,
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseWithSets>,
)
