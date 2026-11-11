package io.github.dimitrysaf.asko.data.settings

import android.content.Context
import androidx.core.content.edit
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.RestSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The app's settings, kept in shared preferences. Each is a [StateFlow], so screens follow changes. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _defaultRestSeconds = MutableStateFlow(prefs.getInt(DEFAULT_REST, 90))

    /** The rest an exercise starts with when it's added to a routine or a log. */
    val defaultRestSeconds: StateFlow<Int> = _defaultRestSeconds.asStateFlow()

    private val _restBuzz = MutableStateFlow(prefs.getBoolean(REST_BUZZ, true))

    /** Buzz when a rest (or a timed set) runs out while the workout is on screen. */
    val restBuzz: StateFlow<Boolean> = _restBuzz.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEEP_SCREEN_ON, true))

    /** Keep the screen on while a workout is open. */
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _sourceOrder = MutableStateFlow(readSourceOrder())

    /** The exercise libraries, first to last: an exercise in more than one shows from the first. */
    val sourceOrder: StateFlow<List<ExerciseSource>> = _sourceOrder.asStateFlow()

    private val _hiddenSources = MutableStateFlow(
        prefs.getStringSet(HIDDEN_SOURCES, emptySet()).orEmpty().mapNotNull { name -> ExerciseSource.entries.firstOrNull { it.name == name } }.toSet(),
    )

    /** Libraries whose exercises aren't browsed or searched (ones already in routines still show). */
    val hiddenSources: StateFlow<Set<ExerciseSource>> = _hiddenSources.asStateFlow()

    fun setSourceOrder(order: List<ExerciseSource>) {
        prefs.edit { putString(SOURCE_ORDER, order.joinToString(",") { it.name }) }
        _sourceOrder.value = order
    }

    /** Hides or shows a library; the last one shown can't be hidden. */
    fun setSourceHidden(source: ExerciseSource, hidden: Boolean) {
        val next = if (hidden) _hiddenSources.value + source else _hiddenSources.value - source
        if (next.size >= ExerciseSource.entries.size) return
        prefs.edit { putStringSet(HIDDEN_SOURCES, next.map { it.name }.toSet()) }
        _hiddenSources.value = next
    }

    /** As saved; libraries added since then go at the end. */
    private fun readSourceOrder(): List<ExerciseSource> {
        val saved = prefs.getString(SOURCE_ORDER, null).orEmpty().split(',')
            .mapNotNull { name -> ExerciseSource.entries.firstOrNull { it.name == name } }
        return saved + ExerciseSource.entries.filterNot { it in saved }
    }

    private val _restSchedule = MutableStateFlow(readRestSchedule())

    /** Your rest days over time (see [RestSchedule]). */
    val restSchedule: StateFlow<RestSchedule> = _restSchedule.asStateFlow()

    /** Sets the rest days from today on; the days before keep theirs. */
    fun setRestDays(days: Set<DayOfWeek>) {
        val today = LocalDate.now()
        val entries = _restSchedule.value.entries.filterNot { it.first == today } + (today to days)
        prefs.edit {
            putString(REST_SCHEDULE, entries.joinToString(";") { (from, set) -> "${from.toEpochDay()}:" + set.joinToString(",") { it.value.toString() } })
        }
        _restSchedule.value = RestSchedule(entries)
    }

    private fun readRestSchedule(): RestSchedule = RestSchedule(
        prefs.getString(REST_SCHEDULE, null).orEmpty().split(';').filter { it.isNotBlank() }.mapNotNull { entry ->
            val (from, days) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val date = from.toLongOrNull()?.let(LocalDate::ofEpochDay) ?: return@mapNotNull null
            date to days.split(',').mapNotNull { it.toIntOrNull()?.takeIf { n -> n in 1..7 }?.let(DayOfWeek::of) }.toSet()
        }.sortedBy { it.first },
    )

    private val _streakAskedThrough = MutableStateFlow(
        prefs.getLong(STREAK_ASKED_THROUGH, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay),
    )

    /** Missed days up to this one were already asked about (a lost streak isn't asked about twice). */
    val streakAskedThrough: StateFlow<LocalDate?> = _streakAskedThrough.asStateFlow()

    fun setStreakAskedThrough(day: LocalDate) {
        prefs.edit { putLong(STREAK_ASKED_THROUGH, day.toEpochDay()) }
        _streakAskedThrough.value = day
    }

    fun setDefaultRestSeconds(seconds: Int) {
        prefs.edit { putInt(DEFAULT_REST, seconds) }
        _defaultRestSeconds.value = seconds
    }

    fun setRestBuzz(on: Boolean) {
        prefs.edit { putBoolean(REST_BUZZ, on) }
        _restBuzz.value = on
    }

    fun setKeepScreenOn(on: Boolean) {
        prefs.edit { putBoolean(KEEP_SCREEN_ON, on) }
        _keepScreenOn.value = on
    }

    private companion object {
        const val DEFAULT_REST = "default_rest_seconds"
        const val REST_BUZZ = "rest_buzz"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val SOURCE_ORDER = "exercise_source_order"
        const val REST_SCHEDULE = "rest_schedule"
        const val STREAK_ASKED_THROUGH = "streak_asked_through"
        const val HIDDEN_SOURCES = "exercise_sources_hidden"
    }
}
