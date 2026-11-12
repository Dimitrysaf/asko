package io.github.dimitrysaf.asko.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.dimitrysaf.asko.data.local.entity.BodyWeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weights ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weights ORDER BY epochDay")
    suspend fun all(): List<BodyWeightEntity>

    @Upsert
    suspend fun upsert(weights: List<BodyWeightEntity>)
}
