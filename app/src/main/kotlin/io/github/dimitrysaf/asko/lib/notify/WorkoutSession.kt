package io.github.dimitrysaf.asko.lib.notify

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Connects the workout screen with its ongoing notification: the workout publishes what the
 * notification shows ([notice]), the notification's buttons send [commands] back. Lives as long
 * as the app (see [io.github.dimitrysaf.asko.lib.di.AppContainer]).
 */
class WorkoutSession {
    private val _notice = MutableStateFlow<WorkoutNotice?>(null)

    /** What the notification shows; null when no workout is open (the notification goes away). */
    val notice: StateFlow<WorkoutNotice?> = _notice.asStateFlow()

    private val _commands = MutableSharedFlow<WorkoutCommand>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Taps on the notification's buttons. */
    val commands: SharedFlow<WorkoutCommand> = _commands.asSharedFlow()

    /** The app is on screen: then the workout screen buzzes itself and no "rest over" notification is needed. */
    @Volatile
    var appVisible: Boolean = true

    fun publish(notice: WorkoutNotice) {
        _notice.value = notice
    }

    fun end() {
        _notice.value = null
    }

    fun send(command: WorkoutCommand) {
        _commands.tryEmit(command)
    }
}

/**
 * What the ongoing notification shows. While [mode] is resting or holding, [countdownEndsAt] (wall
 * clock) shows a countdown; otherwise [startedAt] (wall clock, shifted by pauses) shows the
 * workout's time counting up. [paused] shows neither.
 */
data class WorkoutNotice(
    val routineName: String,
    val exerciseName: String?,
    val setNumber: Int,
    val setCount: Int,
    val mode: WorkoutMode,
    val countdownEndsAt: Long?,
    val startedAt: Long,
    val paused: Boolean,
)

enum class WorkoutMode { WORKING, RESTING, HOLDING }

enum class WorkoutCommand { LESS_REST, MORE_REST, SKIP_REST }
