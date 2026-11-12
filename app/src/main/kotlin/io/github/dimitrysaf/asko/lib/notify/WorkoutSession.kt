package io.github.dimitrysaf.asko.lib.notify

import io.github.dimitrysaf.asko.data.model.SetType

import io.github.dimitrysaf.asko.data.model.Routine
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

    /** The routine changes the last finished workout suggests, for its finish screen; null when none. */
    @Volatile
    var routineUpdate: RoutineUpdate? = null

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
    /** The set's number among the working sets (as its badge shows it); 0 for a warm-up and the like. */
    val setNumber: Int,
    /** How many working sets the exercise has. */
    val setCount: Int,
    /** What the set is: a warm-up, drop set… goes by that rather than a number. */
    val setType: SetType = SetType.NORMAL,
    val mode: WorkoutMode,
    val countdownEndsAt: Long?,
    val startedAt: Long,
    val paused: Boolean,
    /** Each exercise's number of sets, in order: the progress bar's segments. */
    val exerciseSetCounts: List<Int> = emptyList(),
    /** Sets logged or skipped so far, across the workout. */
    val setsDone: Int = 0,
)

enum class WorkoutMode { WORKING, RESTING, HOLDING }

enum class WorkoutCommand { LESS_REST, MORE_REST, SKIP_REST }

/**
 * The workout [workoutId] went differently from its routine: [before] is the routine as saved,
 * [after] the routine as the workout went (see the finish screen).
 */
data class RoutineUpdate(val workoutId: Long, val before: Routine, val after: Routine)
