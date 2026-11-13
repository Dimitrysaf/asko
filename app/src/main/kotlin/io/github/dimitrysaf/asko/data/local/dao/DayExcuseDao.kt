package io.github.dimitrysaf.asko.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.dimitrysaf.asko.data.local.entity.DayExcuseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayExcuseDao {
    @Query("SELECT * FROM day_excuses")
    fun observeAll(): Flow<List<DayExcuseEntity>>

    @Query("SELECT * FROM day_excuses")
    suspend fun all(): List<DayExcuseEntity>

    @Upsert
    suspend fun upsert(days: List<DayExcuseEntity>)

    @Query("DELETE FROM day_excuses WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)

    @Query("DELETE FROM day_excuses")
    suspend fun deleteAll()
}
