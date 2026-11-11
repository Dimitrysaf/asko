package io.github.dimitrysaf.asko.lib.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** The 7 days of the current week, starting on the locale's first day of week (Monday in Greece). */
fun currentWeek(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): List<LocalDate> {
    val first = LocalDate.now(zone).with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
    return List(7) { first.plusDays(it.toLong()) }
}

/** The calendar day this moment falls on, in the phone's time zone. */
fun Instant.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate = atZone(zone).toLocalDate()
