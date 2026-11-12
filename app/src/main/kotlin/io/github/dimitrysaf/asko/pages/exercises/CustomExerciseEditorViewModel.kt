package io.github.dimitrysaf.asko.pages.exercises

import android.net.Uri
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import io.github.dimitrysaf.asko.data.repository.MediaRepository
import java.util.UUID
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import java.io.IOException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.Muscle
import io.github.dimitrysaf.asko.data.repository.CustomExerciseRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Making or changing an exercise of yours ([exerciseId] empty: a new one, called [initialName]):
 * its name, its photos and videos, the muscles it mainly works and the others it also works (which
 * draw it with wger's images), its equipment and how it's done.
 */
class CustomExerciseEditorViewModel(
    exerciseId: String,
    initialName: String,
    private val repository: CustomExerciseRepository,
    exercises: ExerciseRepository,
    private val media: MediaRepository,
) : ViewModel() {
    val isNew: Boolean = exerciseId.isEmpty()

    /** Its id; a new one gets its id now, so photos and videos can be added before it's saved. */
    val id: String = exerciseId.ifEmpty { CustomExerciseRepository.ID_PREFIX + UUID.randomUUID() }

    /** Photos or videos were added or removed here (Discard asks first, and takes a new one's away). */
    private var mediaChanged by mutableStateOf(false)

    /** The library, for the equipment's pictures; empty until read. */
    var library by mutableStateOf(exercises.browsableNow().orEmpty())
        private set

    var name by mutableStateOf(initialName)
    var main by mutableStateOf(emptySet<Muscle>())
        private set
    var other by mutableStateOf(emptySet<Muscle>())
        private set
    var equipment by mutableStateOf(emptySet<String>())
        private set
    var instructions by mutableStateOf("")

    var loaded by mutableStateOf(isNew)
        private set

    /** As it was opened, to tell whether anything changed. */
    private var original: Exercise? = if (isNew) draft() else null

    init {
        viewModelScope.launch {
            library = try {
                exercises.browsable.first()
            } catch (e: IOException) {
                emptyList()
            }
        }
        if (!isNew) {
            viewModelScope.launch {
                repository.exercises.filterNotNull().first().firstOrNull { it.id == id }?.let { e ->
                    name = e.name
                    main = e.primaryMuscles.mapNotNull(Muscle::of).toSet()
                    other = e.secondaryMuscles.mapNotNull(Muscle::of).toSet() - main
                    equipment = e.equipment.toSet()
                    instructions = e.instructions.joinToString("\n")
                }
                original = draft()
                loaded = true
            }
        }
    }

    /** Something changed since it was opened. */
    val dirty: Boolean get() = loaded && (draft() != original || mediaChanged)

    /** It has a name and at least one main muscle (they draw it and file it under a muscle group). */
    val canSave: Boolean get() = name.isNotBlank() && main.isNotEmpty()

    /** A muscle it mainly works, or not; a main muscle isn't also one of the others. */
    fun toggleMain(muscle: Muscle) {
        main = if (muscle in main) main - muscle else main + muscle
        other = other - muscle
    }

    fun toggleOther(muscle: Muscle) {
        other = if (muscle in other) other - muscle else other + muscle
        main = main - muscle
    }

    fun toggleEquipment(item: String) {
        equipment = if (item in equipment) equipment - item else equipment + item
    }

    /** It as it is now, for the drawing and for saving. */
    fun draft() = Exercise(
        id = id,
        name = name.trim(),
        category = "",
        primaryMuscles = Muscle.entries.filter { it in main }.map { it.label },
        secondaryMuscles = Muscle.entries.filter { it in other }.map { it.label },
        equipment = EQUIPMENT.filter { it in equipment } + equipment.filterNot { it in EQUIPMENT }.sorted(),
        animationUrl = "",
        instructions = instructions.lines().map { it.trim() }.filter { it.isNotEmpty() },
        source = ExerciseSource.CUSTOM,
    )

    /** Adds a photo or video from the picker. */
    fun addMedia(uri: Uri) {
        viewModelScope.launch {
            try {
                media.add(id, uri)
                mediaChanged = true
            } catch (e: IOException) {
                // The file couldn't be read: nothing is added.
            }
        }
    }

    fun removeMedia(item: ExerciseMedia) {
        viewModelScope.launch {
            media.remove(item)
            mediaChanged = true
        }
    }

    /** Leaves without saving; a new one's photos and videos go with it. */
    fun discard(then: () -> Unit) {
        viewModelScope.launch {
            if (isNew) media.removeAll(id)
            then()
        }
    }

    fun save(onSaved: (id: String) -> Unit) {
        if (!canSave) return
        viewModelScope.launch { onSaved(repository.save(draft())) }
    }

    companion object {
        /** The equipment to choose from, as the libraries name it, most used first. */
        val EQUIPMENT = listOf(
            "Bodyweight", "Dumbbell", "Barbell", "Kettlebell", "Cable", "Leverage Machine", "Smith Machine",
            "Resistance Band", "EZ Bar", "Medicine Ball", "Stability Ball", "Roller", "Other",
        )
    }
}
