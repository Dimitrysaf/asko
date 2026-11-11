package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A missed day explained as sick or travel. Added in DB version 9. */
@Entity(tableName = "day_excuses")
data class DayExcuseEntity(
    /** The day, as LocalDate.toEpochDay(). */
    @PrimaryKey val epochDay: Long,
    /** [io.github.dimitrysaf.asko.data.model.DayExcuse] name. */
    val type: String,
)
