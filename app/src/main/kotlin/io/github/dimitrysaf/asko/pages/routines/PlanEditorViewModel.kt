package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

/** One set being edited; [key] keeps its row (and the text being typed) stable. */
data class DraftSet(val key: Int, val set: PlannedSet)

/** One exercise being edited; [key] keeps it stable while dragging. */
data class DraftExercise(
    val key: Int,
    val exerciseId: String,
    val restSeconds: Int,
    val timed: Boolean,
    val repRange: Boolean,
    val sets: List<DraftSet>,
    /** A superset with the exercise after it. */
    val supersetWithNext: Boolean = false,
) {
    fun toModel() = RoutineExercise(exerciseId, restSeconds, timed, repRange, sets.map { it.set }, supersetWithNext)
}

/** The exercises as saved; the last one links to nothing. */
fun List<DraftExercise>.toModels(): List<RoutineExercise> = map { it.toModel() }.withoutSupersetGaps()

/**
 * Exercises and sets being edited, as in the routine editor: what a routine plans and what a
 * logged workout records. Subclasses add what they are ([RoutineEditorViewModel],
 * [io.github.dimitrysaf.asko.pages.workout.LogWorkoutViewModel]) and how they're stored.
 * [dirty] says whether there is anything to lose.
 */
abstract class PlanEditorViewModel(
    exercises: ExerciseRepository,
    private val settings: SettingsRepository,
    loaded: Boolean,
) : ViewModel() {
    val items = mutableStateListOf<DraftExercise>()

    /** The expanded (editable) exercise; others show a read-only preview. Tap toggles it. */
    var expandedKey by mutableStateOf<Int?>(null)
        private set

    /** The exercise marked by a long press (it is also what is being dragged). */
    var selectedKey by mutableStateOf<Int?>(null)
        private set

    /** Unsaved changes exist. */
    var dirty by mutableStateOf(false)
        protected set

    /** Each exercise id → the version to show (see ExerciseRepository.lookup); null while it loads. */
    var library by mutableStateOf<Map<String, Exercise>?>(null)
        private set

    /** What the picker offers: the libraries as Settings has them; null while it loads. */
    var browsable by mutableStateOf<List<Exercise>?>(null)
        private set

    /** False while existing exercises are read in. */
    var loaded by mutableStateOf(loaded)
        protected set

    private var nextKey = 0

    init {
        viewModelScope.launch {
            library = try {
                exercises.lookup()
            } catch (e: IOException) {
                emptyMap() // offline before the first sync: exercises show without names
            }
        }
        viewModelScope.launch {
            exercises.browsable
                .catch { e -> if (e is IOException) emit(emptyList()) else throw e }
                .collect { browsable = it }
        }
    }

    /** Puts stored exercises in for editing (they don't count as a change). */
    protected fun load(exercises: List<RoutineExercise>) {
        exercises.forEach { exercise ->
            items += DraftExercise(
                key = nextKey++,
                exerciseId = exercise.exerciseId,
                restSeconds = exercise.restSeconds,
                timed = exercise.timed,
                repRange = exercise.repRange,
                sets = exercise.sets.map { DraftSet(nextKey++, it) },
                supersetWithNext = exercise.supersetWithNext,
            )
        }
    }

    /** Tapping an exercise opens it, tapping it again closes it; either clears the selection. */
    fun toggleExpanded(key: Int) {
        expandedKey = if (expandedKey == key) null else key
        selectedKey = null
    }

    /** Long press: marks the exercise without opening it. */
    fun select(key: Int) {
        selectedKey = key
    }

    fun clearSelection() {
        selectedKey = null
    }

    /**
     * New exercises get one normal set with no targets yet (Add set copies it);
     * the last one added opens for editing.
     */
    fun addExercises(ids: List<String>) = changed {
        ids.forEach { id ->
            items += DraftExercise(
                key = nextKey++,
                exerciseId = id,
                restSeconds = settings.defaultRestSeconds.value,
                timed = false,
                repRange = false,
                sets = List(DEFAULT_SETS) { DraftSet(nextKey++, PlannedSet(SetType.NORMAL)) },
            )
        }
        items.lastOrNull()?.let { expandedKey = it.key }
    }

    fun removeExercise(key: Int) = changed {
        val index = items.indexOfFirst { it.key == key }
        if (index >= 0) takeOut(index)
        if (expandedKey == key) expandedKey = null
        if (selectedKey == key) selectedKey = null
    }

    /**
     * Drag to reorder: moves the exercise [fromKey] to where [toKey] is. Within its superset it
     * just changes places (the superset stays as it is); out of it, it leaves (the superset closes
     * up behind it), and dropped inside another superset, it joins that one.
     */
    fun move(fromKey: Int, toKey: Int) = changed {
        val from = items.indexOfFirst { it.key == fromKey }
        val to = items.indexOfFirst { it.key == toKey }
        if (from < 0 || to < 0) return@changed
        val run = items.supersetRuns { it.supersetWithNext }.first { from in it }
        if (to in run) {
            // The links belong to the places, not the exercises.
            val links = run.map { items[it].supersetWithNext }
            items.add(to, items.removeAt(from))
            run.forEachIndexed { i, index -> items[index] = items[index].copy(supersetWithNext = links[i]) }
            return@changed
        }
        val moved = takeOut(from)
        val before = items.getOrNull(to - 1)
        items.add(to, moved.copy(supersetWithNext = before?.supersetWithNext == true))
    }

    /** Removes the exercise at [index]; the one before it links on to whatever it linked to. */
    private fun takeOut(index: Int): DraftExercise {
        val removed = items.removeAt(index)
        val before = items.getOrNull(index - 1)
        if (before != null && before.supersetWithNext && !removed.supersetWithNext) {
            items[index - 1] = before.copy(supersetWithNext = false)
        }
        return removed
    }

    /**
     * A copy of the exercise (rest, sets and targets) right after it, opened for editing. In a
     * superset, the copy joins it.
     */
    fun duplicateExercise(key: Int) = changed {
        val index = items.indexOfFirst { it.key == key }
        if (index < 0) return@changed
        val source = items[index]
        val copy = source.copy(key = nextKey++, sets = source.sets.map { it.copy(key = nextKey++) })
        items.add(index + 1, copy)
        expandedKey = copy.key
        selectedKey = null
    }

    /** Joins the exercise to the one after it in a superset, or splits them. */
    fun setSuperset(key: Int, withNext: Boolean) = updateExercise(key) { it.copy(supersetWithNext = withNext) }

    fun setRest(key: Int, seconds: Int) = updateExercise(key) { it.copy(restSeconds = seconds) }

    fun setTimed(key: Int, timed: Boolean) = updateExercise(key) { it.copy(timed = timed) }

    /** Reps as one number or as a range; leaving range mode drops the top values. */
    fun setRepRange(key: Int, range: Boolean) = updateExercise(key) { exercise ->
        exercise.copy(
            repRange = range,
            sets = if (range) exercise.sets else exercise.sets.map { it.copy(set = it.set.copy(repsMax = null)) },
        )
    }

    /** A new set copies the last one's targets (that's what you'd type anyway), as a normal set. */
    fun addSet(key: Int) = updateExercise(key) { exercise ->
        val last = exercise.sets.lastOrNull()?.set ?: PlannedSet(SetType.NORMAL)
        exercise.copy(sets = exercise.sets + DraftSet(nextKey++, last.copy(type = SetType.NORMAL)))
    }

    /** An exercise keeps at least one set. */
    fun removeSet(key: Int, setKey: Int) = updateExercise(key) { exercise ->
        if (exercise.sets.size <= 1) exercise else exercise.copy(sets = exercise.sets.filterNot { it.key == setKey })
    }

    // Targets. Editing the first row also fills the rows below that are empty or still hold what
    // the first row had (so typing "1" then "12" ends with 12 everywhere, not 1). To-failure sets
    // have no reps or time target, so those skip them.
    fun setReps(key: Int, setKey: Int, reps: Int?) =
        setTarget(key, setKey, reps, { it.reps }, { set, v -> set.copy(reps = v) }, skip = { it.type == SetType.FAILURE })

    fun setRepsMax(key: Int, setKey: Int, reps: Int?) =
        setTarget(key, setKey, reps, { it.repsMax }, { set, v -> set.copy(repsMax = v) }, skip = { it.type == SetType.FAILURE })

    fun setDuration(key: Int, setKey: Int, seconds: Int?) =
        setTarget(key, setKey, seconds, { it.durationSeconds }, { set, v -> set.copy(durationSeconds = v) }, skip = { it.type == SetType.FAILURE })

    fun setWeight(key: Int, setKey: Int, kg: Double?) =
        setTarget(key, setKey, kg, { it.weightKg }, { set, v -> set.copy(weightKg = v) }, skip = { false })

    private fun <T> setTarget(
        key: Int,
        setKey: Int,
        value: T?,
        read: (PlannedSet) -> T?,
        write: (PlannedSet, T?) -> PlannedSet,
        skip: (PlannedSet) -> Boolean,
    ) = updateExercise(key) { exercise ->
        val first = exercise.sets.firstOrNull()
        val editingFirst = first?.key == setKey
        val firstBefore = first?.let { read(it.set) }
        exercise.copy(
            sets = exercise.sets.map { draft ->
                val current = read(draft.set)
                when {
                    draft.key == setKey -> draft.copy(set = write(draft.set, value))
                    editingFirst && !skip(draft.set) && (current == null || current == firstBefore) ->
                        draft.copy(set = write(draft.set, value))
                    else -> draft
                }
            },
        )
    }

    fun updateSet(key: Int, setKey: Int, transform: (PlannedSet) -> PlannedSet) = updateExercise(key) { exercise ->
        exercise.copy(sets = exercise.sets.map { if (it.key == setKey) it.copy(set = transform(it.set)) else it })
    }

    private fun updateExercise(key: Int, transform: (DraftExercise) -> DraftExercise) = changed {
        val index = items.indexOfFirst { it.key == key }
        if (index >= 0) items[index] = transform(items[index])
    }

    protected fun changed(edit: () -> Unit) {
        edit()
        dirty = true
    }

    private companion object {
        const val DEFAULT_SETS = 1
    }
}
