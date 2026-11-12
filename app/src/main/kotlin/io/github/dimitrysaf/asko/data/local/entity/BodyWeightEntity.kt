package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Your body weight on a day (the last one logged that day). Added in DB version 11. */
@Entity(tableName = "body_weights")
data class BodyWeightEntity(
    /** The day, as LocalDate.toEpochDay(). */
    @PrimaryKey val epochDay: Long,
    val kg: Double,
)
