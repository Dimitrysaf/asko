package io.github.dimitrysaf.asko.data.settings

import android.content.Context
import androidx.core.content.edit
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
    }
}
