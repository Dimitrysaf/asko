package io.github.dimitrysaf.asko.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import io.github.dimitrysaf.asko.data.local.dao.DayExcuseDao
import io.github.dimitrysaf.asko.data.local.entity.DayExcuseEntity
import io.github.dimitrysaf.asko.data.model.DayExcuse
import io.github.dimitrysaf.asko.data.model.Streak
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.lib.time.localDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The workout streak: workouts and rest days add to it, sick and travel days pause it, other
 * missed days end it. A lost streak can be explained (sick, travelling) or let go.
 */
class StreakRepository(
    workouts: WorkoutRepository,
    private val dao: DayExcuseDao,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
) {
    private val excuses: Flow<Map<LocalDate, DayExcuse>> = dao.observeAll().map { list ->
        list.mapNotNull { e -> DayExcuse.entries.firstOrNull { it.name == e.type }?.let { LocalDate.ofEpochDay(e.epochDay) to it } }.toMap()
    }

    /**
     * The streak as of today, following workouts, excuses and the rest-day settings; cached for as
     * long as the app runs, null until first worked out.
     */
    val streak: StateFlow<Streak?> = combine(
        workouts.history.map { history -> history.map { it.startedAt.localDate() }.toSet() },
        excuses,
        settings.restSchedule,
        settings.streakAskedThrough,
    ) { days, excused, rest, asked -> Streak.of(days, excused, rest, LocalDate.now(), asked) }
        .stateIn(scope, SharingStarted.Lazily, null)

    /** The missed [days] were spent sick or travelling: they pause the streak instead of ending it. */
    suspend fun excuse(days: List<LocalDate>, excuse: DayExcuse) {
        dao.upsert(days.map { DayExcuseEntity(it.toEpochDay(), excuse.name) })
        days.maxOrNull()?.let(settings::setStreakAskedThrough)
    }

    /** The streak is let go: these missed days aren't asked about again. */
    fun letGo(days: List<LocalDate>) {
        days.maxOrNull()?.let(settings::setStreakAskedThrough)
    }

    /** Every excused day (for backups). */
    suspend fun excusesNow(): Map<LocalDate, DayExcuse> =
        dao.all().mapNotNull { e -> DayExcuse.entries.firstOrNull { it.name == e.type }?.let { LocalDate.ofEpochDay(e.epochDay) to it } }.toMap()

    /** Puts [excuses] in place of every excused day (restoring a backup). */
    suspend fun replaceExcuses(excuses: Map<LocalDate, DayExcuse>) {
        dao.deleteAll()
        dao.upsert(excuses.map { (day, excuse) -> DayExcuseEntity(day.toEpochDay(), excuse.name) })
    }
}
