package io.github.dimitrysaf.asko.data.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What a day was for the streak. [WORKOUT] (a workout done) and [REST] (one of your rest days)
 * add to it; [SICK] and [TRAVEL] pause it; any other day that's over ends it.
 */
enum class DayMark { WORKOUT, REST, SICK, TRAVEL }

/** A missed day explained afterwards, so it pauses the streak instead of ending it. */
enum class DayExcuse { SICK, TRAVEL }

/**
 * Your rest days over time: each entry's days hold from its date until the next entry. Changing
 * them adds an entry from today, so days before keep the rest days they had.
 */
data class RestSchedule(val entries: List<Pair<LocalDate, Set<DayOfWeek>>>) {
    /** The rest days set now. */
    val current: Set<DayOfWeek> get() = entries.lastOrNull()?.second.orEmpty()

    fun isRest(day: LocalDate): Boolean =
        entries.lastOrNull { (from, _) -> !from.isAfter(day) }?.second?.contains(day.dayOfWeek) == true
}

/** A streak that ended: how long it was, and the days missed since (to explain them). */
data class LostStreak(val length: Int, val missed: List<LocalDate>)

/**
 * The streak as of [today]: how long it is now, each day's mark, and a streak lost to missed days
 * (only when those days haven't been asked about yet).
 */
class Streak(
    val current: Int,
    val lost: LostStreak?,
    private val workoutDays: Set<LocalDate>,
    private val excuses: Map<LocalDate, DayExcuse>,
    private val rest: RestSchedule,
) {
    private val firstWorkout: LocalDate? = workoutDays.minOrNull()

    /**
     * The day counts towards a streak: a workout, or a rest day since the first workout, up to
     * today. These are filled in the calendar.
     */
    fun counts(day: LocalDate, today: LocalDate = LocalDate.now()): Boolean = when {
        day in workoutDays -> true
        firstWorkout == null || day.isBefore(firstWorkout) || day.isAfter(today) -> false
        else -> day !in excuses && rest.isRest(day)
    }

    /** The day is one of your rest days (as they were set then). */
    fun isRestDay(day: LocalDate): Boolean = rest.isRest(day)

    /** The day's icon: a workout, a sick or travel day, a rest day, or nothing. */
    fun markOf(day: LocalDate): DayMark? = when {
        day in workoutDays -> DayMark.WORKOUT
        excuses[day] == DayExcuse.SICK -> DayMark.SICK
        excuses[day] == DayExcuse.TRAVEL -> DayMark.TRAVEL
        rest.isRest(day) -> DayMark.REST
        else -> null
    }

    companion object {
        /**
         * From the first workout to [today]: workout and rest days add one, sick and travel days
         * pause, any other day that's over ends the streak. Today only adds (it isn't over).
         * A streak ended by missed days up to yesterday, with no workout since, is [lost] unless
         * it was already asked about ([askedThrough]).
         */
        fun of(
            workoutDays: Set<LocalDate>,
            excuses: Map<LocalDate, DayExcuse>,
            rest: RestSchedule,
            today: LocalDate,
            askedThrough: LocalDate?,
        ): Streak {
            var count = 0
            var lostLength = 0
            val missed = mutableListOf<LocalDate>()
            var day = workoutDays.minOrNull()
            while (day != null && !day.isAfter(today)) {
                when {
                    day in workoutDays -> {
                        count++
                        missed.clear()
                        lostLength = 0
                    }
                    day in excuses -> Unit
                    rest.isRest(day) -> count++
                    day == today -> Unit
                    else -> {
                        if (missed.isEmpty()) lostLength = count
                        missed += day
                        count = 0
                    }
                }
                day = day.plusDays(1)
            }
            val unasked = missed.filter { askedThrough == null || it.isAfter(askedThrough) }
            val lost = if (lostLength > 0 && unasked.isNotEmpty()) LostStreak(lostLength, unasked) else null
            return Streak(count, lost, workoutDays, excuses, rest)
        }
    }
}
