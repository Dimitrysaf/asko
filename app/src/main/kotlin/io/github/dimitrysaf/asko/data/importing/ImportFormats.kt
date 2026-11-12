package io.github.dimitrysaf.asko.data.importing

import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.lib.format.WeightUnit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** One set as another app exported it, in Asko's units (kg, metres, seconds). */
internal data class ImportedSet(
    val type: SetType,
    val reps: Int?,
    val weightKg: Double?,
    val seconds: Int?,
    val meters: Double?,
)

/** One exercise of an imported workout, by the other app's name for it. */
internal data class ImportedExercise(
    val name: String,
    val sets: List<ImportedSet>,
    /** Hevy: done as a superset with the exercise after it. */
    val supersetWithNext: Boolean = false,
)

internal data class ImportedWorkout(
    val name: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val note: String?,
    val exercises: List<ImportedExercise>,
)

/** Which app a CSV came from. */
enum class ImportSource { STRONG, HEVY }

/** A CSV export read into workouts, or null when it's neither app's. */
internal class ParsedImport(val source: ImportSource, val workouts: List<ImportedWorkout>)

/**
 * Reads a Strong or Hevy CSV export. Weights and distances without a unit column are taken in
 * [unit] (kg and km, or lb and mi), as those apps export them in yours.
 */
internal fun parseImport(text: String, unit: WeightUnit, zone: ZoneId = ZoneId.systemDefault()): ParsedImport? {
    val rows = parseCsv(text.removePrefix("﻿"), delimiterOf(text))
    val header = rows.firstOrNull()?.map { it.trim() } ?: return null
    val column = header.withIndex().associate { (i, name) -> name.lowercase() to i }
    val body = rows.drop(1).map { row -> Row(row, column) }
    return when {
        "exercise_title" in column && "start_time" in column -> ParsedImport(ImportSource.HEVY, hevy(body, zone))
        "exercise name" in column && "date" in column -> ParsedImport(ImportSource.STRONG, strong(body, unit, zone))
        else -> null
    }
}

/** A CSV row, its fields by column name (any case). */
internal class Row(private val fields: List<String>, private val column: Map<String, Int>) {
    operator fun get(name: String): String = column[name.lowercase()]?.let { fields.getOrNull(it) }?.trim().orEmpty()
    fun has(name: String) = name.lowercase() in column
    fun number(name: String): Double? = get(name).replace(',', '.').toDoubleOrNull()
}

// Strong: one row per set; "Date" and "Workout Name" tell the workouts apart, "Set Order" is the
// set's number (or W, D, F for warm-up, drop and failure sets; "Rest Timer" rows are skipped).

private fun strong(rows: List<Row>, unit: WeightUnit, zone: ZoneId): List<ImportedWorkout> =
    rows.groupConsecutive { it["Date"] + "\u0000" + it["Workout Name"] }.mapNotNull { sets ->
        val first = sets.first()
        val started = parseDate(first["Date"], STRONG_DATES, zone) ?: return@mapNotNull null
        val duration = parseDuration(first["Duration"].ifEmpty { first["Workout Duration"] })
        val exercises = sets
            .filterNot { it["Set Order"].contains("rest", ignoreCase = true) }
            .groupConsecutive { it["Exercise Name"] }
            .map { exerciseRows ->
                ImportedExercise(
                    name = exerciseRows.first()["Exercise Name"],
                    sets = exerciseRows.map { row ->
                        val weightUnit = when (row["Weight Unit"].lowercase()) {
                            "lbs", "lb" -> WeightUnit.LB
                            "kg", "kgs" -> WeightUnit.KG
                            else -> unit
                        }
                        val distanceMeters = row.number("Distance")?.takeIf { it > 0 }?.let { d ->
                            when (row["Distance Unit"].lowercase()) {
                                "m" -> d
                                "km" -> d * 1000
                                "mi", "miles" -> d * METERS_PER_MILE
                                else -> if (unit == WeightUnit.LB) d * METERS_PER_MILE else d * 1000
                            }
                        }
                        ImportedSet(
                            type = when (row["Set Order"].uppercase()) {
                                "W" -> SetType.WARMUP
                                "D" -> SetType.DROP
                                "F" -> SetType.FAILURE
                                else -> SetType.NORMAL
                            },
                            reps = row.number("Reps")?.toInt()?.takeIf { it > 0 },
                            weightKg = row.number("Weight")?.takeIf { it > 0 }?.let(weightUnit::toKg),
                            seconds = row.number("Seconds")?.toInt()?.takeIf { it > 0 },
                            meters = distanceMeters,
                        )
                    },
                )
            }
        ImportedWorkout(
            name = first["Workout Name"].ifEmpty { null },
            startedAt = started,
            endedAt = duration?.let { started.plusSeconds(it) },
            note = first["Workout Notes"].ifEmpty { null },
            exercises = exercises,
        )
    }

// Hevy: one row per set; "start_time" and "title" tell the workouts apart, "set_type" says what
// the set was, and exercises sharing a "superset_id" one after another are a superset.

private fun hevy(rows: List<Row>, zone: ZoneId): List<ImportedWorkout> =
    rows.groupConsecutive { it["start_time"] + "\u0000" + it["title"] }.mapNotNull { sets ->
        val first = sets.first()
        val started = parseDate(first["start_time"], HEVY_DATES, zone) ?: return@mapNotNull null
        val ended = parseDate(first["end_time"], HEVY_DATES, zone)
        val groups = sets.groupConsecutive { it["exercise_title"] + "\u0000" + it["superset_id"] }
        val exercises = groups.mapIndexed { i, exerciseRows ->
            val superset = exerciseRows.first()["superset_id"]
            ImportedExercise(
                name = exerciseRows.first()["exercise_title"],
                supersetWithNext = superset.isNotEmpty() && groups.getOrNull(i + 1)?.first()?.get("superset_id") == superset,
                sets = exerciseRows.map { row ->
                    val kg = row.number("weight_kg") ?: row.number("weight_lbs")?.let(WeightUnit.LB::toKg)
                    val meters = row.number("distance_km")?.times(1000)
                        ?: row.number("distance_miles")?.times(METERS_PER_MILE)
                        ?: row.number("distance_meters")
                    ImportedSet(
                        type = when (row["set_type"].lowercase()) {
                            "warmup" -> SetType.WARMUP
                            "dropset" -> SetType.DROP
                            "failure" -> SetType.FAILURE
                            else -> SetType.NORMAL
                        },
                        reps = row.number("reps")?.toInt()?.takeIf { it > 0 },
                        weightKg = kg?.takeIf { it > 0 },
                        seconds = row.number("duration_seconds")?.toInt()?.takeIf { it > 0 },
                        meters = meters?.takeIf { it > 0 },
                    )
                },
            )
        }
        ImportedWorkout(
            name = first["title"].ifEmpty { null },
            startedAt = started,
            endedAt = ended,
            note = first["description"].ifEmpty { null },
            exercises = exercises,
        )
    }

/** Rows next to each other with the same [key], as runs, in order. */
private fun <T> List<T>.groupConsecutive(key: (T) -> String): List<List<T>> {
    val runs = mutableListOf<MutableList<T>>()
    var last: String? = null
    for (item in this) {
        val k = key(item)
        if (k != last || runs.isEmpty()) runs += mutableListOf(item) else runs.last() += item
        last = k
    }
    return runs
}

private val STRONG_DATES = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss")
private val HEVY_DATES = listOf("d MMM yyyy, HH:mm", "d MMM yyyy HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")

/** A date and time in the phone's time zone, in any of [patterns]; null if none fits. */
private fun parseDate(text: String, patterns: List<String>, zone: ZoneId): Instant? {
    if (text.isBlank()) return null
    for (pattern in patterns) {
        try {
            return LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)).atZone(zone).toInstant()
        } catch (_: DateTimeParseException) {
        }
    }
    return null
}

/** Seconds in Strong's "1h 5m" (or "45m", "30s", or plain seconds); null if empty. */
private fun parseDuration(text: String): Long? {
    if (text.isBlank()) return null
    text.trim().toLongOrNull()?.let { return it }
    val parts = Regex("(\\d+)\\s*([hms])").findAll(text.lowercase()).toList()
    if (parts.isEmpty()) return null
    return parts.sumOf { m ->
        val n = m.groupValues[1].toLong()
        when (m.groupValues[2]) {
            "h" -> n * 3600
            "m" -> n * 60
            else -> n
        }
    }
}

private const val METERS_PER_MILE = 1609.344
