package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.local.dao.BodyWeightDao
import io.github.dimitrysaf.asko.data.local.entity.BodyWeightEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Your body weight over time, as you log it (optional). */
class BodyWeightRepository(private val dao: BodyWeightDao, scope: CoroutineScope) {
    /** Each day logged → kilos, oldest first; null until first read. */
    val weights: StateFlow<List<Pair<LocalDate, Double>>?> =
        dao.observeAll().map { list -> list.map { LocalDate.ofEpochDay(it.epochDay) to it.kg } }
            .stateIn(scope, SharingStarted.Lazily, null)

    /** Logs [kg] for [day] (in place of what was logged that day). */
    suspend fun log(day: LocalDate, kg: Double) = dao.upsert(listOf(BodyWeightEntity(day.toEpochDay(), kg)))

    /** Every entry, as stored (for backups). */
    suspend fun allNow(): List<BodyWeightEntity> = dao.all()

    /** Puts these back (restoring a backup), over the same days. */
    suspend fun restore(weights: List<BodyWeightEntity>) = dao.upsert(weights)
}
