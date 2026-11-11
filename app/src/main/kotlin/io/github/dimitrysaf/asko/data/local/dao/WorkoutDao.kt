package io.github.dimitrysaf.asko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.dimitrysaf.asko.data.local.entity.WorkoutEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutSetEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observe(id: Long): Flow<WorkoutWithExercises?>

    @Query("SELECT id FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeWorkoutId(): Long?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    /** The running workout's id, or a new workout's. One transaction, so two callers can't both insert. */
    @Transaction
    suspend fun activeOrNewWorkoutId(startedAt: Long): Long =
        activeWorkoutId() ?: insertWorkout(WorkoutEntity(startedAt = startedAt))

    @Query("UPDATE workouts SET endedAt = :endedAt WHERE id = :id")
    suspend fun finishWorkout(id: Long, endedAt: Long)

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    /** A finished workout with its exercises and sets, in order, as one transaction. */
    @Transaction
    suspend fun insertLogged(workout: WorkoutEntity, exercises: List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>): Long {
        val workoutId = insertWorkout(workout)
        insertExercisesOf(workoutId, exercises)
        return workoutId
    }

    /** An edited workout: its new times, and its exercises and sets replaced, as one transaction. */
    @Transaction
    suspend fun replace(workoutId: Long, startedAt: Long, endedAt: Long, exercises: List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>) {
        updateTimes(workoutId, startedAt, endedAt)
        deleteSetsOf(workoutId)
        deleteExercisesOf(workoutId)
        insertExercisesOf(workoutId, exercises)
    }

    /** A workout and everything in it. */
    @Transaction
    suspend fun delete(workoutId: Long) {
        deleteSetsOf(workoutId)
        deleteExercisesOf(workoutId)
        deleteWorkout(workoutId)
    }

    private suspend fun insertExercisesOf(workoutId: Long, exercises: List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>) {
        exercises.forEachIndexed { position, (exercise, sets) ->
            val exerciseId = insertExercise(exercise.copy(workoutId = workoutId, position = position))
            insertSets(sets.mapIndexed { i, set -> set.copy(workoutExerciseId = exerciseId, position = i) })
        }
    }

    @Query("UPDATE workouts SET startedAt = :startedAt, endedAt = :endedAt WHERE id = :workoutId")
    suspend fun updateTimes(workoutId: Long, startedAt: Long, endedAt: Long)

    @Query("DELETE FROM workout_sets WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)")
    suspend fun deleteSetsOf(workoutId: Long)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesOf(workoutId: Long)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    /** Every workout with its exercises and sets (for restoring a backup). */
    @Transaction
    suspend fun deleteAll() {
        deleteAllSets()
        deleteAllExercises()
        deleteAllWorkouts()
    }

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllSets()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}
