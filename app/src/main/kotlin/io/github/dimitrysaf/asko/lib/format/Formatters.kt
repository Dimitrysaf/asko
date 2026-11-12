package io.github.dimitrysaf.asko.lib.format

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun Instant.formatDate(zone: ZoneId = ZoneId.systemDefault()): String =
    dateFormatter.format(atZone(zone))

/** 0:42, 12:05, 1:03:09 */
fun Duration.formatClock(): String {
    val h = toHours()
    val m = toMinutesPart()
    val s = toSecondsPart()
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
