package io.github.dimitrysaf.asko.data.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** How far back a chart looks. */
enum class StatsRange { MONTH, QUARTER, YEAR, ALL }

/** How long one bar or point of a chart is. */
enum class BucketSize { DAY, WEEK, MONTH }

/**
 * The time a chart covers, cut into equal buckets (days, weeks or months) starting at [starts].
 * [indexOf] says which bucket a day falls in.
 */
class Timeline(val starts: List<LocalDate>, val size: BucketSize, private val firstDayOfWeek: DayOfWeek) {
    fun indexOf(day: LocalDate): Int? {
        val start = bucketStart(day, size, firstDayOfWeek)
        return starts.binarySearch(start).takeIf { it >= 0 }
    }
}

/** The start of the bucket [day] is in. */
fun bucketStart(day: LocalDate, size: BucketSize, firstDayOfWeek: DayOfWeek): LocalDate = when (size) {
    BucketSize.DAY -> day
    BucketSize.WEEK -> day.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    BucketSize.MONTH -> day.withDayOfMonth(1)
}

/**
 * The timeline for [range] up to [today]: the last 30 days by day, 13 weeks by week, 12 months by
 * month, or (all of it) by month from [earliest], at least two months.
 */
fun timeline(range: StatsRange, today: LocalDate, earliest: LocalDate?, firstDayOfWeek: DayOfWeek): Timeline {
    val (size, count) = when (range) {
        StatsRange.MONTH -> BucketSize.DAY to 30
        StatsRange.QUARTER -> BucketSize.WEEK to 13
        StatsRange.YEAR -> BucketSize.MONTH to 12
        StatsRange.ALL -> {
            val first = (earliest ?: today).withDayOfMonth(1)
            // At least two months: a chart needs two points to draw a line.
            BucketSize.MONTH to (ChronoUnit.MONTHS.between(first, today.withDayOfMonth(1)).toInt() + 1).coerceAtLeast(2)
        }
    }
    return weeksOrDaysBack(size, count, today, firstDayOfWeek)
}

/** The last [count] buckets of [size] ending with the one [today] is in. */
fun weeksOrDaysBack(size: BucketSize, count: Int, today: LocalDate, firstDayOfWeek: DayOfWeek): Timeline {
    val last = bucketStart(today, size, firstDayOfWeek)
    val starts = (count - 1 downTo 0).map { back ->
        when (size) {
            BucketSize.DAY -> last.minusDays(back.toLong())
            BucketSize.WEEK -> last.minusWeeks(back.toLong())
            BucketSize.MONTH -> last.minusMonths(back.toLong())
        }
    }
    return Timeline(starts, size, firstDayOfWeek)
}
