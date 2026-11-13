package io.github.dimitrysaf.asko.pages.workout

import io.github.dimitrysaf.asko.pages.routines.isBodyweight

import io.github.dimitrysaf.asko.pages.routines.setNumbers

import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.lib.notify.RoutineUpdate
import io.github.dimitrysaf.asko.lib.notify.WorkoutSession
import io.github.dimitrysaf.asko.lib.notify.WorkoutNotice
import io.github.dimitrysaf.asko.lib.notify.WorkoutMode
import io.github.dimitrysaf.asko.lib.notify.WorkoutCommand
import kotlinx.coroutines.flow.collectLatest
import io.github.dimitrysaf.asko.data.session.SavedWorkout
import io.github.dimitrysaf.asko.data.session.SavedValues
import io.github.dimitrysaf.asko.data.session.SavedSet
import io.github.dimitrysaf.asko.data.session.SavedExercise
import io.github.dimitrysaf.asko.data.session.SavedCountdown
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import androidx.compose.runtime.snapshotFlow
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.data.model.supersetRuns
import io.github.dimitrysaf.asko.data.model.withoutSupersetGaps
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

/** [FAILED]: tried but couldn't finish it; logged with the reps you got. */
enum class SetStatus { PENDING, DONE, FAILED, SKIPPED }

/** A set of the workout: what the routine planned ([target]) and what is logged ([actual]). */
data class PlayerSet(val key: Int, val target: PlannedSet, val actual: PlannedSet, val status: SetStatus)

data class PlayerExercise(
    val key: Int,
    val exerciseId: String,
    val restSeconds: Int,
    val timed: Boolean,
    val repRange: Boolean,
    val sets: List<PlayerSet>,
    /** A superset with the exercise after it. */
    val supersetWithNext: Boolean = false,
    /** Sets are a distance and a time. */
    val distance: Boolean = false,
)

enum class CountdownKind { REST, SET }

/** A running countdown: the rest after a set, or a timed set being held. */
data class Countdown(val kind: CountdownKind, val totalMs: Long, val remainingMs: Long) {
    val progress: Float get() = if (totalMs <= 0) 0f else remainingMs.toFloat() / totalMs
}

/**
 * A routine being done, set by set. The current set (a cursor over exercises and sets) starts at
 * the first. Next logs it as done (planned values, unless changed), starts the exercise's rest and
 * moves to the following set. A superset's exercises take turns, round by round (first sets, then
 * second sets…), with no rest inside a round and the superset's last exercise's rest after it; a timed set's clock starts by itself after the rest (or at once
 * without one), and when it runs out Next happens on its own. Pausing stops the workout clock and
 * any countdown. It's saved after every change, so closing the app doesn't lose it; Finish stores
 * the logged sets as a workout, Discard throws it away.
 */
class ActiveWorkoutViewModel(
    private val routineId: Long,
    private val routines: RoutineRepository,
    private val workouts: WorkoutRepository,
    exercises: ExerciseRepository,
    private val store: ActiveWorkoutStore,
    private val session: WorkoutSession,
    private val settings: SettingsRepository,
    /** What an empty workout is called while it's done ("Workout"). */
    private val emptyName: String,
) : ViewModel() {
    /** Started empty (no routine): exercises are added as you go. */
    val isEmptyWorkout: Boolean get() = routineId == EMPTY_WORKOUT

    var routineName by mutableStateOf("")
        private set
    val items = mutableStateListOf<PlayerExercise>()
    var library by mutableStateOf<Map<String, Exercise>>(emptyMap())
        private set
    var loaded by mutableStateOf(false)
        private set

    /** The exercises to pick from when adding or swapping (as Discovery shows them); null while loading. */
    var browsable by mutableStateOf<List<Exercise>?>(null)
        private set

    var exerciseIndex by mutableIntStateOf(0)
        private set
    var setIndex by mutableIntStateOf(0)
        private set

    private var startedAt = Instant.now()

    /** Time worked out, without pauses. */
    var elapsedMs by mutableLongStateOf(0L)
        private set
    var paused by mutableStateOf(false)
        private set
    var countdown by mutableStateOf<Countdown?>(null)
        private set

    /** The rest is between two sets of the same exercise (else it leads into a new exercise). */
    var restWithinExercise by mutableStateOf(false)
        private set

    private val alertChannel = Channel<CountdownKind>(Channel.CONFLATED)

    /** A countdown ran out (rest over, timed set held): the page vibrates. */
    val alerts = alertChannel.receiveAsFlow()

    private var nextKey = 0
    private var finishing = false

    val currentExercise: PlayerExercise? get() = items.getOrNull(exerciseIndex)
    val currentSet: PlayerSet? get() = currentExercise?.sets?.getOrNull(setIndex)
    val doneSets: Int get() = items.sumOf { e -> e.sets.count { it.status == SetStatus.DONE || it.status == SetStatus.FAILED } }
    val totalSets: Int get() = items.sumOf { it.sets.size }
    val allDone: Boolean get() = loaded && items.isNotEmpty() && items.all { e -> e.sets.none { it.status == SetStatus.PENDING } }

    init {
        viewModelScope.launch {
            // Carry on the saved workout of this routine (the app was closed mid-workout), or start it.
            val saved = store.load()?.takeIf { it.routineId == routineId }
            if (saved != null) {
                restore(saved)
            } else if (isEmptyWorkout) {
                routineName = emptyName
            } else {
                routines.routine(routineId).first()?.let { routine ->
                    routineName = routine.name
                    routine.exercises.forEach { exercise ->
                        items += PlayerExercise(
                            key = nextKey++,
                            exerciseId = exercise.exerciseId,
                            restSeconds = exercise.restSeconds,
                            timed = exercise.timed,
                            repRange = exercise.repRange,
                            sets = exercise.sets.map { PlayerSet(nextKey++, it, it.startingActual(), SetStatus.PENDING) },
                            supersetWithNext = exercise.supersetWithNext,
                            distance = exercise.distance,
                        )
                    }
                }
            }
            loaded = true
            // The ongoing notification: published once now (so it exists when the screen starts its
            // service), then whenever what it shows changes; its buttons act on the rest.
            session.publish(notice())
            launch { snapshotFlow { notice() }.collect { if (!finishing) session.publish(it) } }
            launch {
                session.commands.collect { command ->
                    when (command) {
                        WorkoutCommand.LESS_REST -> adjustRest(-REST_STEP_SECONDS)
                        WorkoutCommand.MORE_REST -> adjustRest(REST_STEP_SECONDS)
                        WorkoutCommand.SKIP_REST -> skipCountdown()
                    }
                }
            }
            // Saved after every change (not every tick: the clocks are rebuilt from when it was saved).
            snapshotFlow { SaveKey(items.toList(), exerciseIndex, setIndex, paused, countdown?.kind, countdown?.totalMs, restWithinExercise) }
                .collectLatest { if (!finishing) store.save(snapshot()) }
        }
        viewModelScope.launch {
            library = try {
                exercises.lookup()
            } catch (e: IOException) {
                emptyMap()
            }
        }
        viewModelScope.launch {
            browsable = try {
                exercises.browsable.first()
            } catch (e: IOException) {
                emptyList()
            }
        }
        // One clock for everything; elapsedRealtime keeps counting while the screen is off.
        viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (true) {
                delay(TICK_MS)
                val now = SystemClock.elapsedRealtime()
                tick(now - last)
                last = now
            }
        }
    }

    private fun tick(deltaMs: Long) {
        // Every set logged: the workout's time stops (and runs again if one is put back).
        if (paused || !loaded || allDone) return
        elapsedMs += deltaMs
        val running = countdown ?: return
        val remaining = running.remainingMs - deltaMs
        if (remaining > 0) {
            countdown = running.copy(remainingMs = remaining)
            return
        }
        countdown = null
        alertChannel.trySend(running.kind)
        when (running.kind) {
            // Rest over: a timed set starts by itself.
            CountdownKind.REST -> startHoldIfTimed()
            // Held long enough: logged, and on to the rest / next set as with Next.
            CountdownKind.SET -> next()
        }
    }

    /** A timed set is current and its clock hasn't started: the Pause button starts it. */
    val holdWaiting: Boolean
        get() {
            val exercise = currentExercise ?: return false
            val set = currentSet ?: return false
            return exercise.timed && set.status == SetStatus.PENDING && set.target.type != SetType.FAILURE &&
                (set.actual.durationSeconds ?: 0) > 0 && countdown == null
        }

    /** Pause / Resume; for a timed set still waiting, it starts its clock. */
    /**
     * Pauses or resumes, and nothing else (unlike [togglePause], which starts a timed set's clock
     * that's waiting): for the camera counter, while it can't see you.
     */
    fun holdPaused(on: Boolean) {
        paused = on
    }

    fun togglePause() {
        if (!paused && holdWaiting) {
            startHoldIfTimed()
        } else {
            paused = !paused
        }
    }

    /**
     * Next: the current set is logged as done (unless it already is logged or skipped), then the
     * exercise's rest starts. Without a rest, a timed next set starts its clock right away. Moving
     * from a set already done to another already done (going forward again after going back)
     * just moves: no rest.
     */
    fun next(justLogged: Boolean = false) {
        val exercise = currentExercise ?: return
        val set = currentSet ?: return
        val running = countdown
        // Already logged before this Next (you went back to it with Previous); [justLogged]: logged
        // a moment ago (the camera counter moving on), so the rest still comes.
        val wasLogged = !justLogged && set.status != SetStatus.PENDING
        if (set.status == SetStatus.PENDING) {
            val held = if (running?.kind == CountdownKind.SET) ((running.totalMs - running.remainingMs) / 1000).toInt() else null
            updateSet(exercise.key, set.key) {
                it.copy(
                    status = SetStatus.DONE,
                    actual = if (held != null) it.actual.copy(durationSeconds = held) else it.actual,
                )
            }
        }
        countdown = null
        val from = exerciseIndex
        val run = runOf(from)
        if (!advance()) return
        restWithinExercise = exerciseIndex == from
        paused = false
        // Going forward again over sets already done (after going back): just move, no rest.
        if (wasLogged && currentSet?.status != SetStatus.PENDING) return
        // Within a superset's round, straight on to the next exercise; after it, the round's rest.
        val roundGoesOn = exerciseIndex in run && exerciseIndex > from
        val rest = if (roundGoesOn) 0 else items[run.last].restSeconds
        if (rest > 0) {
            countdown = Countdown(CountdownKind.REST, rest * 1000L, rest * 1000L)
        } else {
            startHoldIfTimed()
        }
    }

    /** The exercises in runs: a superset is one, every other exercise one of its own. */
    private fun runs(): List<IntRange> = items.supersetRuns { it.supersetWithNext }

    private fun runOf(exerciseIndex: Int): IntRange = runs().firstOrNull { exerciseIndex in it } ?: exerciseIndex..exerciseIndex

    /** The current exercise is part of a superset. */
    val inSuperset: Boolean get() = runOf(exerciseIndex).let { it.first != it.last }

    /**
     * Every set, in the order they're done: exercise by exercise, except that a superset's
     * exercises take turns, round by round (an exercise with fewer sets drops out when it runs out).
     */
    private fun order(): List<Pair<Int, Int>> = buildList {
        runs().forEach { run ->
            val rounds = run.maxOf { items[it].sets.size }
            for (round in 0 until rounds) {
                for (e in run) if (round < items[e].sets.size) add(e to round)
            }
        }
    }

    /** Where the current set is in [order]. */
    private fun position(order: List<Pair<Int, Int>>) = order.indexOf(exerciseIndex to setIndex)

    /** Starts the current set's clock, if it is a timed set waiting. */
    private fun startHoldIfTimed() {
        if (!holdWaiting) return
        val ms = (currentSet?.actual?.durationSeconds ?: return) * 1000L
        paused = false
        countdown = Countdown(CountdownKind.SET, ms, ms)
    }

    /**
     * Done / Undone: marks the current set done (a running clock stops and logs the time held; a
     * rest ends), or puts a logged one back to do (a timed set's time back to the full planned
     * time). It doesn't move on; Next does.
     */
    fun toggleCurrent() {
        val exercise = currentExercise ?: return
        val set = currentSet ?: return
        // During a rest, Done also ends it.
        if (countdown?.kind == CountdownKind.REST) countdown = null
        val running = countdown
        when (set.status) {
            // Undone: a timed set's clock goes back to its full planned time, ready to start again.
            SetStatus.DONE, SetStatus.FAILED -> updateSet(exercise.key, set.key) {
                it.copy(
                    status = SetStatus.PENDING,
                    actual = if (exercise.timed) it.actual.copy(durationSeconds = it.target.durationSeconds) else it.actual,
                )
            }
            else -> {
                val held = if (running?.kind == CountdownKind.SET) ((running.totalMs - running.remainingMs) / 1000).toInt() else null
                if (held != null) countdown = null
                updateSet(exercise.key, set.key) {
                    it.copy(
                        status = SetStatus.DONE,
                        actual = if (held != null) it.actual.copy(durationSeconds = held) else it.actual,
                    )
                }
            }
        }
    }

    /** Couldn't finish the set: logs it as failed with the [reps] you got. */
    fun logFailed(exerciseKey: Int, setKey: Int, reps: Int) =
        updateSet(exerciseKey, setKey) { it.copy(actual = it.actual.copy(reps = reps), status = SetStatus.FAILED) }

    /** Done as planned: logs a set with the values shown. */
    fun markDone(exerciseKey: Int, setKey: Int) = updateSet(exerciseKey, setKey) { it.copy(status = SetStatus.DONE) }

    /** Doesn't do this set; it is left out of the log. */
    fun skip(exerciseKey: Int, setKey: Int) {
        updateSet(exerciseKey, setKey) { it.copy(status = SetStatus.SKIPPED) }
    }

    /** Logs [reps] (fewer than planned, or more) for a set and marks it done. */
    fun logReps(exerciseKey: Int, setKey: Int, reps: Int) =
        updateSet(exerciseKey, setKey) { it.copy(actual = it.actual.copy(reps = reps), status = SetStatus.DONE) }

    /** Logs how far a distance set went, and marks it done. */
    fun logDistance(exerciseKey: Int, setKey: Int, meters: Double) =
        updateSet(exerciseKey, setKey) { it.copy(actual = it.actual.copy(distanceMeters = meters), status = SetStatus.DONE) }

    /** Logs how long a distance set took, and marks it done. */
    fun logTime(exerciseKey: Int, setKey: Int, seconds: Int) =
        updateSet(exerciseKey, setKey) { it.copy(actual = it.actual.copy(durationSeconds = seconds), status = SetStatus.DONE) }

    fun setReps(exerciseKey: Int, setKey: Int, reps: Int?) = setActual(exerciseKey, setKey) { it.copy(reps = reps) }

    fun setDuration(exerciseKey: Int, setKey: Int, seconds: Int?) = setActual(exerciseKey, setKey) { it.copy(durationSeconds = seconds) }

    fun setDistance(exerciseKey: Int, setKey: Int, meters: Double?) = setActual(exerciseKey, setKey) { it.copy(distanceMeters = meters) }

    /**
     * A new weight for a set. The sets after it still to do that had the same weight change too:
     * moving up (or down) a weight usually holds for the rest of the exercise.
     */
    fun setWeight(exerciseKey: Int, setKey: Int, kg: Double?) {
        val exercise = items.firstOrNull { it.key == exerciseKey } ?: return
        val index = exercise.sets.indexOfFirst { it.key == setKey }
        if (index < 0) return
        val before = exercise.sets[index].actual.weightKg
        replaceExercise(exerciseKey) { e ->
            e.copy(sets = e.sets.mapIndexed { i, s ->
                when {
                    i == index -> s.copy(actual = s.actual.copy(weightKg = kg))
                    i > index && s.status == SetStatus.PENDING && s.actual.weightKg == before -> s.copy(actual = s.actual.copy(weightKg = kg))
                    else -> s
                }
            })
        }
    }

    /** Another set like this one, right after it, to do. */
    fun addSetAfter(exerciseKey: Int, setKey: Int) = replaceExercise(exerciseKey) { e ->
        val index = e.sets.indexOfFirst { it.key == setKey }
        val source = e.sets[index]
        val copy = PlayerSet(nextKey++, source.target, source.actual.copy(type = source.target.type), SetStatus.PENDING)
        e.copy(sets = e.sets.toMutableList().apply { add(index + 1, copy) })
    }

    /** Removes a set (an exercise keeps at least one). */
    fun removeSet(exerciseKey: Int, setKey: Int) {
        val current = currentSet?.key
        replaceExercise(exerciseKey) { e -> if (e.sets.size <= 1) e else e.copy(sets = e.sets.filterNot { it.key == setKey }) }
        val e = currentExercise ?: return
        setIndex = e.sets.indexOfFirst { it.key == current }.takeIf { it >= 0 } ?: setIndex.coerceAtMost(e.sets.lastIndex)
    }

    /** Makes a set the current one (tapping its row). */
    fun goTo(exerciseKey: Int, setKey: Int) {
        val e = items.indexOfFirst { it.key == exerciseKey }
        if (e < 0) return
        val s = items[e].sets.indexOfFirst { it.key == setKey }
        if (s < 0) return
        if ((exerciseIndex != e || setIndex != s) && countdown?.kind == CountdownKind.SET) countdown = null
        exerciseIndex = e
        setIndex = s
    }

    /**
     * Back to the set before, across exercises, without logging anything. Any running clock stops:
     * a rest is dropped (back to the set just done), a timed set's hold ends.
     */
    fun previousSet() {
        countdown = null
        val order = order()
        val (e, s) = order.getOrNull(position(order) - 1) ?: return
        exerciseIndex = e
        setIndex = s
    }

    /** Moves to the set after the current one (see [order]); false at the very end. */
    private fun advance(): Boolean {
        val order = order()
        val at = position(order)
        if (at < 0 || at >= order.lastIndex) return false
        val (e, s) = order[at + 1]
        exerciseIndex = e
        setIndex = s
        return true
    }

    val hasPrevious: Boolean get() = position(order()) > 0
    val hasNext: Boolean get() = order().let { val at = position(it); at >= 0 && at < it.lastIndex }

    fun adjustRest(deltaSeconds: Int) {
        val running = countdown ?: return
        val remaining = (running.remainingMs + deltaSeconds * 1000L).coerceAtLeast(0)
        if (remaining == 0L) {
            skipCountdown()
        } else {
            countdown = running.copy(remainingMs = remaining, totalMs = maxOf(running.totalMs, remaining))
        }
    }

    /** Ends the rest now; a timed set then starts its clock, as when the rest runs out. */
    fun skipCountdown() {
        val wasRest = countdown?.kind == CountdownKind.REST
        countdown = null
        if (wasRest) startHoldIfTimed()
    }

    /** Nothing of the exercise is logged yet: it can be swapped or removed. */
    fun canChange(exercise: PlayerExercise): Boolean =
        exercise.sets.none { it.status == SetStatus.DONE || it.status == SetStatus.FAILED }

    /**
     * Adds exercises at the end, each with the default rest and one empty set (as in the routine
     * editor). In an empty workout the first one added becomes the current one.
     */
    fun addExercises(ids: List<String>) {
        val wasEmpty = items.isEmpty()
        ids.forEach { id ->
            // A bodyweight exercise's weight is BW from the start.
            val weight = if (library[id]?.isBodyweight == true) 0.0 else null
            items += PlayerExercise(
                key = nextKey++,
                exerciseId = id,
                restSeconds = settings.defaultRestSeconds.value,
                timed = false,
                repRange = false,
                sets = listOf(PlannedSet(SetType.NORMAL, weightKg = weight).let { PlayerSet(nextKey++, it, it, SetStatus.PENDING) }),
            )
        }
        if (wasEmpty && items.isNotEmpty()) {
            exerciseIndex = 0
            setIndex = 0
        }
    }

    /** Puts [exerciseId] in the place of an exercise not started yet, with its sets and rest. */
    fun swapExercise(exerciseKey: Int, exerciseId: String) {
        val exercise = items.firstOrNull { it.key == exerciseKey } ?: return
        if (!canChange(exercise)) return
        replaceExercise(exerciseKey) { it.copy(exerciseId = exerciseId) }
    }

    /** Takes out an exercise not started yet; a superset closes up around it. */
    fun removeExercise(exerciseKey: Int) {
        val index = items.indexOfFirst { it.key == exerciseKey }
        if (index < 0 || !canChange(items[index])) return
        keepingCurrent { takeOut(index) }
    }

    /**
     * Drag to reorder, as in the routine editor: within its superset it just changes places; out
     * of it, it leaves (the superset closes up behind it), and dropped inside another, it joins it.
     */
    fun moveExercise(fromKey: Int, toKey: Int) = keepingCurrent {
        val from = items.indexOfFirst { it.key == fromKey }
        val to = items.indexOfFirst { it.key == toKey }
        if (from < 0 || to < 0 || from == to) return@keepingCurrent
        val run = runOf(from)
        if (to in run) {
            // The links belong to the places, not the exercises.
            val links = run.map { items[it].supersetWithNext }
            items.add(to, items.removeAt(from))
            run.forEachIndexed { i, index -> items[index] = items[index].copy(supersetWithNext = links[i]) }
            return@keepingCurrent
        }
        val moved = takeOut(from)
        val before = items.getOrNull(to - 1)
        items.add(to, moved.copy(supersetWithNext = before?.supersetWithNext == true))
        items.lastOrNull()?.let { if (it.supersetWithNext) items[items.lastIndex] = it.copy(supersetWithNext = false) }
    }

    /** Opens an exercise: its first set still to do (or its first set). */
    fun openExercise(exerciseKey: Int) {
        val e = items.indexOfFirst { it.key == exerciseKey }
        if (e < 0) return
        if (countdown?.kind == CountdownKind.SET) countdown = null
        exerciseIndex = e
        setIndex = items[e].sets.indexOfFirst { it.status == SetStatus.PENDING }.coerceAtLeast(0)
    }

    /** Removes the exercise at [index]; the one before it links on to whatever it linked to. */
    private fun takeOut(index: Int): PlayerExercise {
        val removed = items.removeAt(index)
        val before = items.getOrNull(index - 1)
        if (before != null && before.supersetWithNext && !removed.supersetWithNext) {
            items[index - 1] = before.copy(supersetWithNext = false)
        }
        return removed
    }

    /**
     * Changes the exercises in [change], staying on the same set; if its exercise went, on the
     * first set still to do (a timed set's clock stops).
     */
    private inline fun keepingCurrent(change: () -> Unit) {
        val exerciseKey = currentExercise?.key
        val setKey = currentSet?.key
        change()
        val e = items.indexOfFirst { it.key == exerciseKey }
        if (e >= 0) {
            exerciseIndex = e
            setIndex = items[e].sets.indexOfFirst { it.key == setKey }.coerceAtLeast(0)
        } else {
            if (countdown?.kind == CountdownKind.SET) countdown = null
            val (pe, ps) = order().firstOrNull { (x, y) -> items[x].sets[y].status == SetStatus.PENDING } ?: (0 to 0)
            exerciseIndex = pe
            setIndex = ps
        }
    }

    /** Throws the workout away: nothing is logged, and there's nothing to resume. */
    fun discard(onDiscarded: () -> Unit) {
        if (finishing) return
        finishing = true
        session.end()
        viewModelScope.launch {
            store.clear()
            onDiscarded()
        }
    }

    /** Stores the logged sets as a finished workout, then reports its id (null: nothing was logged). */
    fun finish(onFinished: (workoutId: Long?) -> Unit) {
        if (finishing) return
        finishing = true
        session.end()
        viewModelScope.launch {
            store.clear()
            // Exercises with nothing logged are left out; a superset closes up around them.
            val done = mutableListOf<RoutineExercise>()
            items.forEach { e ->
                val sets = e.sets.filter { it.status == SetStatus.DONE || it.status == SetStatus.FAILED }.map { it.actual }
                if (sets.isNotEmpty()) {
                    done += RoutineExercise(e.exerciseId, e.restSeconds, e.timed, repRange = false, sets = sets, supersetWithNext = e.supersetWithNext, distance = e.distance)
                } else if (!e.supersetWithNext) {
                    done.lastOrNull()?.let { done[done.lastIndex] = it.copy(supersetWithNext = false) }
                }
            }
            val exercises = done.withoutSupersetGaps()
            val name = if (isEmptyWorkout) null else routineName.ifBlank { null }
            val id = if (exercises.isNotEmpty()) workouts.log(startedAt, exercises, endedAt = startedAt.plusMillis(elapsedMs), routineName = name) else null
            // The routine as this workout went, offered on the finish screen when it differs.
            session.routineUpdate = null
            if (id != null && !isEmptyWorkout) {
                routines.routine(routineId).first()?.let { routine ->
                    val plan = plannedFromWorkout()
                    if (plan != routine.exercises) session.routineUpdate = RoutineUpdate(id, routine, routine.copy(exercises = plan))
                }
            }
            onFinished(id)
        }
    }

    /**
     * The routine as this workout went: its exercises in their order now (added, swapped, removed,
     * moved), each set as done: the weight used, and the reps done (a rep range stays a range).
     * Sets not done, skipped or failed keep their plan; a timed set keeps its planned time.
     */
    private fun plannedFromWorkout(): List<RoutineExercise> = items.map { e ->
        RoutineExercise(
            exerciseId = e.exerciseId,
            restSeconds = e.restSeconds,
            timed = e.timed,
            repRange = e.repRange,
            sets = e.sets.map { s ->
                when (s.status) {
                    // A distance: how far, and how long it took.
                    SetStatus.DONE if e.distance -> s.target.copy(distanceMeters = s.actual.distanceMeters, durationSeconds = s.actual.durationSeconds)
                    SetStatus.DONE -> s.target.copy(
                        weightKg = s.actual.weightKg,
                        // A range stays a range, a to-failure set stays without a target.
                        reps = if (e.repRange || e.timed || s.target.type == SetType.FAILURE) s.target.reps else s.actual.reps,
                    )
                    SetStatus.FAILED -> s.target.copy(weightKg = s.actual.weightKg)
                    else -> s.target
                }
            },
            supersetWithNext = e.supersetWithNext,
            distance = e.distance,
        )
    }.withoutSupersetGaps()

    private fun setActual(exerciseKey: Int, setKey: Int, transform: (PlannedSet) -> PlannedSet) =
        updateSet(exerciseKey, setKey) { it.copy(actual = transform(it.actual)) }

    private fun updateSet(exerciseKey: Int, setKey: Int, transform: (PlayerSet) -> PlayerSet) = replaceExercise(exerciseKey) { e ->
        e.copy(sets = e.sets.map { if (it.key == setKey) transform(it) else it })
    }

    private fun replaceExercise(exerciseKey: Int, transform: (PlayerExercise) -> PlayerExercise) {
        val index = items.indexOfFirst { it.key == exerciseKey }
        if (index >= 0) items[index] = transform(items[index])
    }

    /** The app is on screen or not: off screen, a rest running out buzzes as a notification instead. */
    fun setAppVisible(visible: Boolean) {
        session.appVisible = visible
    }

    /**
     * What the notification shows, as of now. Times are wall clock, rounded to the second, so they
     * stay the same from tick to tick and only a real change updates the notification.
     */
    private fun notice(): WorkoutNotice {
        val now = System.currentTimeMillis()
        val running = countdown
        val exercise = currentExercise
        // Numbered as the set badges are: only working sets count; the others go by their type.
        val numbers = exercise?.sets?.map { it.target }?.setNumbers().orEmpty()
        return WorkoutNotice(
            routineName = routineName,
            exerciseName = exercise?.let { library[it.exerciseId]?.name },
            setNumber = numbers.getOrNull(setIndex) ?: 0,
            setCount = numbers.count { it != null },
            setType = currentSet?.target?.type ?: SetType.NORMAL,
            mode = when (running?.kind) {
                CountdownKind.REST -> WorkoutMode.RESTING
                CountdownKind.SET -> WorkoutMode.HOLDING
                null -> WorkoutMode.WORKING
            },
            countdownEndsAt = running?.let { (now + it.remainingMs).roundToSecond() },
            startedAt = (now - elapsedMs).roundToSecond(),
            // Every set logged: the clock has stopped, as when paused.
            paused = paused || allDone,
            exerciseSetCounts = items.map { it.sets.size },
            setsDone = items.sumOf { e -> e.sets.count { it.status != SetStatus.PENDING } },
        )
    }

    private fun Long.roundToSecond() = (this + 500) / 1000 * 1000

    /** What's saved, as of now. */
    private fun snapshot() = SavedWorkout(
        routineId = routineId,
        routineName = routineName,
        startedAtMs = startedAt.toEpochMilli(),
        elapsedMs = elapsedMs,
        paused = paused,
        exerciseIndex = exerciseIndex,
        setIndex = setIndex,
        restWithinExercise = restWithinExercise,
        countdown = countdown?.let { SavedCountdown(it.kind.name, it.totalMs, it.remainingMs) },
        exercises = items.map { e ->
            SavedExercise(e.exerciseId, e.restSeconds, e.timed, e.repRange, e.sets.map { SavedSet(it.target.saved(), it.actual.saved(), it.status.name) }, e.supersetWithNext, e.distance)
        },
        savedAtMs = System.currentTimeMillis(),
    )

    /**
     * Back to where the saved workout was. Unless it was paused, the time since it was saved
     * counts: the workout clock moves on and a rest (or timed set) runs down by that much.
     */
    private fun restore(saved: SavedWorkout) {
        routineName = saved.routineName
        startedAt = Instant.ofEpochMilli(saved.startedAtMs)
        val away = if (saved.paused) 0L else (System.currentTimeMillis() - saved.savedAtMs).coerceAtLeast(0)
        elapsedMs = saved.elapsedMs + away
        paused = saved.paused
        saved.exercises.forEach { e ->
            items += PlayerExercise(
                key = nextKey++,
                exerciseId = e.exerciseId,
                restSeconds = e.restSeconds,
                timed = e.timed,
                repRange = e.repRange,
                sets = e.sets.map { PlayerSet(nextKey++, it.target.planned(), it.actual.planned(), SetStatus.entries.firstOrNull { s -> s.name == it.status } ?: SetStatus.PENDING) },
                supersetWithNext = e.supersetWithNext,
                distance = e.distance,
            )
        }
        exerciseIndex = saved.exerciseIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        setIndex = saved.setIndex.coerceIn(0, ((items.getOrNull(exerciseIndex)?.sets?.size ?: 1) - 1).coerceAtLeast(0))
        restWithinExercise = saved.restWithinExercise
        countdown = saved.countdown?.let { c ->
            val kind = CountdownKind.entries.firstOrNull { it.name == c.kind } ?: return@let null
            val remaining = c.remainingMs - away
            if (remaining > 0) Countdown(kind, c.totalMs, remaining) else null // ran out while away
        }
    }

    /** What [snapshotFlow] watches: a change to any of these saves the workout. */
    private data class SaveKey(
        val items: List<PlayerExercise>,
        val exerciseIndex: Int,
        val setIndex: Int,
        val paused: Boolean,
        val countdownKind: CountdownKind?,
        val countdownTotal: Long?,
        val restWithinExercise: Boolean,
    )

    companion object {
        /** The routine id of a workout started empty (routines' ids start at 1). */
        const val EMPTY_WORKOUT = 0L
        private const val TICK_MS = 200L
        private const val REST_STEP_SECONDS = 15
    }
}

/**
 * What a set is logged as unless changed: the plan, with a rep range's top as the reps to aim for.
 * To-failure sets have no target, so their reps start empty.
 */
private fun PlannedSet.startingActual() = copy(reps = repsMax ?: reps, repsMax = null)

private fun PlannedSet.saved() = SavedValues(type.name, reps, repsMax, weightKg, durationSeconds, distanceMeters)

private fun SavedValues.planned() = PlannedSet(
    type = SetType.entries.firstOrNull { it.name == type } ?: SetType.NORMAL,
    reps = reps,
    repsMax = repsMax,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    distanceMeters = distanceMeters,
)
