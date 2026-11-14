package io.github.dimitrysaf.asko.data.settings

import io.github.dimitrysaf.asko.lib.format.DistanceUnit

import android.content.Context
import androidx.core.content.edit
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.github.dimitrysaf.asko.data.model.RestSchedule
import io.github.dimitrysaf.asko.lib.format.WeightUnit
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

    private val _restSound = MutableStateFlow(prefs.getBoolean(REST_SOUND, false))

    /** Play the notification sound when a rest (or a timed set) runs out while the workout is on screen. */
    val restSound: StateFlow<Boolean> = _restSound.asStateFlow()

    fun setRestSound(on: Boolean) {
        prefs.edit { putBoolean(REST_SOUND, on) }
        _restSound.value = on
    }

    private val _weightUnit = MutableStateFlow(
        WeightUnit.entries.firstOrNull { it.name == prefs.getString(WEIGHT_UNIT, null) } ?: WeightUnit.KG,
    )

    /** The unit weights are shown and typed in (they're stored in kilos either way). */
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit { putString(WEIGHT_UNIT, unit.name) }
        _weightUnit.value = unit
    }

    private val _distanceUnit = MutableStateFlow(
        DistanceUnit.entries.firstOrNull { it.name == prefs.getString(DISTANCE_UNIT, null) } ?: DistanceUnit.KM,
    )

    /** The unit distances are shown and typed in (they're stored in metres either way). */
    val distanceUnit: StateFlow<DistanceUnit> = _distanceUnit.asStateFlow()

    fun setDistanceUnit(unit: DistanceUnit) {
        prefs.edit { putString(DISTANCE_UNIT, unit.name) }
        _distanceUnit.value = unit
    }

    private val _showMuscles = MutableStateFlow(prefs.getBoolean(SHOW_MUSCLES, true))

    /** Each exercise's pictures end with the muscles it works, on wger's body. */
    val showMuscles: StateFlow<Boolean> = _showMuscles.asStateFlow()

    fun setShowMuscles(on: Boolean) {
        prefs.edit { putBoolean(SHOW_MUSCLES, on) }
        _showMuscles.value = on
    }

    private val _repCounter = MutableStateFlow(
        RepCounterMode.entries.firstOrNull { it.name == prefs.getString(REP_COUNTER, null) } ?: RepCounterMode.OFF,
    )

    /** How reps are counted during a set: not at all, aloud at a steady pace, or by the camera. */
    val repCounter: StateFlow<RepCounterMode> = _repCounter.asStateFlow()

    fun setRepCounter(mode: RepCounterMode) {
        prefs.edit { putString(REP_COUNTER, mode.name) }
        _repCounter.value = mode
    }

    private val _sayReps = MutableStateFlow(prefs.getBoolean(SAY_REPS, true))

    /** The rep counter says each rep aloud; off, it counts silently. */
    val sayReps: StateFlow<Boolean> = _sayReps.asStateFlow()

    fun setSayReps(on: Boolean) {
        prefs.edit { putBoolean(SAY_REPS, on) }
        _sayReps.value = on
    }

    private val _pauseOutOfView = MutableStateFlow(prefs.getBoolean(PAUSE_OUT_OF_VIEW, true))

    /** With the camera counter, the workout pauses while the camera can't see you. */
    val pauseOutOfView: StateFlow<Boolean> = _pauseOutOfView.asStateFlow()

    fun setPauseOutOfView(on: Boolean) {
        prefs.edit { putBoolean(PAUSE_OUT_OF_VIEW, on) }
        _pauseOutOfView.value = on
    }

    /** The metronome's time per rep for an exercise, in milliseconds (2 s until changed). */
    fun repPaceMs(exerciseId: String): Int = prefs.getInt(REP_PACE + exerciseId, DEFAULT_REP_PACE_MS)

    fun setRepPaceMs(exerciseId: String, ms: Int) = prefs.edit { putInt(REP_PACE + exerciseId, ms) }

    private val _healthConnect = MutableStateFlow(prefs.getBoolean(HEALTH_CONNECT, false))

    /** Finished workouts and body weight are written to Health Connect (once its permissions are given). */
    val healthConnect: StateFlow<Boolean> = _healthConnect.asStateFlow()

    fun setHealthConnect(on: Boolean) {
        prefs.edit { putBoolean(HEALTH_CONNECT, on) }
        _healthConnect.value = on
    }

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(KEEP_SCREEN_ON, true))

    /** Keep the screen on while a workout is open. */
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _userLibraries = MutableStateFlow(readUserLibraries())

    /** The libraries you added from a link, in the order you added them. */
    val userLibraries: StateFlow<List<UserLibrary>> = _userLibraries.asStateFlow()

    private val _sourceOrder = MutableStateFlow(readSourceOrder())

    /** The exercise libraries, first to last: an exercise in more than one shows from the first. */
    val sourceOrder: StateFlow<List<ExerciseSource>> = _sourceOrder.asStateFlow()

    private val _hiddenSources = MutableStateFlow(readHiddenSources())

    /** Libraries whose exercises aren't browsed or searched (ones already in routines still show). */
    val hiddenSources: StateFlow<Set<ExerciseSource>> = _hiddenSources.asStateFlow()

    fun setSourceOrder(order: List<ExerciseSource>) {
        prefs.edit { putString(SOURCE_ORDER, order.joinToString(",") { it.name }) }
        _sourceOrder.value = order
    }

    /** Hides or shows a library; the last one shown can't be hidden. */
    fun setSourceHidden(source: ExerciseSource, hidden: Boolean) {
        val next = if (hidden) _hiddenSources.value + source else _hiddenSources.value - source
        if (_sourceOrder.value.all { it in next }) return
        saveHidden(next)
    }

    private fun saveHidden(hidden: Set<ExerciseSource>) {
        prefs.edit { putStringSet(HIDDEN_SOURCES, hidden.map { it.name }.toSet()) }
        _hiddenSources.value = hidden
    }

    /** Adds a library from a link: last in the order, shown. */
    fun addUserLibrary(library: UserLibrary) {
        val all = _userLibraries.value + library
        saveUserLibraries(all)
        setSourceOrder(_sourceOrder.value + library.source)
    }

    /** Removes a library you added (its downloaded copy is the repository's to delete). */
    fun removeUserLibrary(library: UserLibrary) {
        saveUserLibraries(_userLibraries.value - library)
        setSourceOrder(_sourceOrder.value - library.source)
        saveHidden(_hiddenSources.value - library.source)
    }

    private fun saveUserLibraries(all: List<UserLibrary>) {
        prefs.edit { putString(USER_LIBRARIES, Json.encodeToString(all)) }
        _userLibraries.value = all
    }

    private fun readUserLibraries(): List<UserLibrary> =
        prefs.getString(USER_LIBRARIES, null)?.let { runCatching { Json.decodeFromString<List<UserLibrary>>(it) }.getOrNull() }.orEmpty()

    /** Every library there is: the built-in ones and the ones you added. */
    private fun allSources(): List<ExerciseSource> = ExerciseSource.BUILT_IN + _userLibraries.value.map { it.source }

    /**
     * As saved; libraries added since then go at the end, except your own exercises, which go
     * first (yours win over a library's exercise of the same name).
     */
    private fun readSourceOrder(): List<ExerciseSource> {
        val known = allSources()
        val saved = prefs.getString(SOURCE_ORDER, null).orEmpty().split(',').filter { it.isNotEmpty() }
            .map(::ExerciseSource).filter { it in known }
        val added = known.filterNot { it in saved }
        return added.filter { it == ExerciseSource.CUSTOM } + saved + added.filterNot { it == ExerciseSource.CUSTOM }
    }

    /**
     * As saved. wger starts hidden: on a new phone, and the first time an older version's
     * settings are read (it wasn't in their order yet).
     */
    private fun readHiddenSources(): Set<ExerciseSource> {
        val saved = prefs.getStringSet(HIDDEN_SOURCES, null)?.map(::ExerciseSource)?.toSet()
        val savedOrder = prefs.getString(SOURCE_ORDER, null).orEmpty()
        val wgerIsNew = saved == null || ExerciseSource.WGER.name !in savedOrder.split(',')
        val hidden = saved.orEmpty() + (if (wgerIsNew) setOf(ExerciseSource.WGER) else emptySet())
        if (hidden != saved) {
            prefs.edit {
                putStringSet(HIDDEN_SOURCES, hidden.map { it.name }.toSet())
                // Remember wger as seen, so switching it on sticks.
                putString(SOURCE_ORDER, _sourceOrder.value.joinToString(",") { it.name })
            }
        }
        return hidden.filter { it in _sourceOrder.value }.toSet()
    }

    private val _hideDuplicates = MutableStateFlow(prefs.getBoolean(HIDE_DUPLICATES, true))

    /** An exercise in more than one library shows once, from the first; off, every library's copy shows. */
    val hideDuplicates: StateFlow<Boolean> = _hideDuplicates.asStateFlow()

    fun setHideDuplicates(on: Boolean) {
        prefs.edit { putBoolean(HIDE_DUPLICATES, on) }
        _hideDuplicates.value = on
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

    private val _editableWorkoutRows = MutableStateFlow(prefs.getBoolean(EDITABLE_WORKOUT_ROWS, false))

    /** Now Exercising's set rows are fields to type into (off: read-only, changed through each set's sheet). */
    val editableWorkoutRows: StateFlow<Boolean> = _editableWorkoutRows.asStateFlow()

    fun setEditableWorkoutRows(on: Boolean) {
        prefs.edit { putBoolean(EDITABLE_WORKOUT_ROWS, on) }
        _editableWorkoutRows.value = on
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
        const val REST_SOUND = "rest_sound"
        const val WEIGHT_UNIT = "weight_unit"
        const val DISTANCE_UNIT = "distance_unit"
        const val HEALTH_CONNECT = "health_connect"
        const val REP_COUNTER = "rep_counter"
        const val SAY_REPS = "say_reps"
        const val PAUSE_OUT_OF_VIEW = "pause_out_of_view"
        const val REP_PACE = "rep_pace_"
        const val DEFAULT_REP_PACE_MS = 2_000
        const val SHOW_MUSCLES = "show_muscles"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val EDITABLE_WORKOUT_ROWS = "editable_workout_rows"
        const val SOURCE_ORDER = "exercise_source_order"
        const val REST_SCHEDULE = "rest_schedule"
        const val STREAK_ASKED_THROUGH = "streak_asked_through"
        const val HIDDEN_SOURCES = "exercise_sources_hidden"
        const val USER_LIBRARIES = "user_libraries"
        const val HIDE_DUPLICATES = "hide_duplicates"
    }
}

/** A library you added: its [name] as you called it, and the [url] of its JSON file. */
@Serializable
data class UserLibrary(val id: String, val name: String, val url: String) {
    val source: ExerciseSource get() = ExerciseSource.link(id)
}
