package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Draft of a new routine ([routineId] 0, named [initialName]) or an existing one. Nothing is
 * stored until [save].
 */
class RoutineEditorViewModel(
    private val routineId: Long,
    initialName: String,
    private val routines: RoutineRepository,
    exercises: ExerciseRepository,
    settings: SettingsRepository,
) : PlanEditorViewModel(exercises, settings, loaded = routineId == 0L) {
    var name by mutableStateOf(initialName)
        private set

    private var saving = false

    val canSave: Boolean get() = loaded && name.isNotBlank() && items.isNotEmpty()

    init {
        if (routineId != 0L) {
            viewModelScope.launch {
                routines.routine(routineId).first()?.let { routine ->
                    name = routine.name
                    load(routine.exercises)
                }
                loaded = true
            }
        }
    }

    fun rename(newName: String) = changed { name = newName }

    /** Saves once (a double tap can't create two routines), then reports the routine's id. */
    fun save(onSaved: (routineId: Long) -> Unit) {
        if (!canSave || saving) return
        saving = true
        viewModelScope.launch {
            val id = routines.save(Routine(routineId, name.trim(), items.toModels()))
            dirty = false
            onSaved(id)
        }
    }
}
