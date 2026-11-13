package io.github.dimitrysaf.asko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import io.github.dimitrysaf.asko.data.local.entity.CustomExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.ExerciseMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomExerciseDao {
    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomExerciseEntity>>

    @Query("SELECT * FROM custom_exercises")
    suspend fun all(): List<CustomExerciseEntity>

    @Upsert
    suspend fun upsert(exercise: CustomExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<CustomExerciseEntity>)

    @Query("UPDATE custom_exercises SET retired = 1 WHERE id = :id")
    suspend fun retire(id: String)

    @Query("UPDATE custom_exercises SET retired = 0 WHERE id = :id")
    suspend fun unretire(id: String)
}

@Dao
interface ExerciseMediaDao {
    @Query("SELECT * FROM exercise_media ORDER BY addedAtMs")
    fun observeAll(): Flow<List<ExerciseMediaEntity>>

    @Insert
    suspend fun insert(media: ExerciseMediaEntity): Long

    @Query("DELETE FROM exercise_media WHERE id = :id")
    suspend fun delete(id: Long)
}
