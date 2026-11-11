package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.runtime.mutableStateListOf
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.repository.NoteRepository
import java.time.Instant
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.pages.routines.PlanEditorViewModel
import io.github.dimitrysaf.asko.pages.routines.toModels
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A workout being logged after the fact: when it was ([at], now by default) and what was done.
 * With a [workoutId], an existing workout being edited instead: it opens with its time and what
 * was done, and saving keeps how long it took.
 */
class LogWorkoutViewModel(
    private val workouts: WorkoutRepository,
    private val noteRepository: NoteRepository,
    exercises: ExerciseRepository,
    settings: SettingsRepository,
    private val workoutId: Long = 0,
) : PlanEditorViewModel(exercises, settings, loaded = workoutId == 0L) {
    var at: LocalDateTime by mutableStateOf(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
        private set

    val editing: Boolean get() = workoutId != 0L

    /** An edited workout's length, kept when its start time changes. */
    private var duration: Duration = Duration.ZERO

    private var logging = false

    /** The workout's notes, saved with it when it's logged (and lost if it's discarded). */
    val notes = mutableStateListOf<Note>()

    /** Ids for notes not saved yet: below zero, so they never meet a stored one. */
    private var nextNoteId = -1L

    fun addNote(note: Note) = changed { notes += note.copy(id = nextNoteId--, createdAt = Instant.now()).trimmedToType() }

    fun editNote(note: Note) = changed {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) notes[index] = note.copy(editedAt = Instant.now()).trimmedToType()
    }

    fun deleteNote(note: Note) = changed { notes.removeAll { it.id == note.id } }

    val canLog: Boolean get() = loaded && items.isNotEmpty()

    init {
        if (editing) {
            viewModelScope.launch {
                workouts.workout(workoutId).first()?.let { workout ->
                    at = LocalDateTime.ofInstant(workout.startedAt, ZoneId.systemDefault())
                    duration = workout.duration ?: Duration.ZERO
                    load(workout.exercises)
                }
                notes += noteRepository.notesNow(NoteSubject.WORKOUT, workoutId.toString())
                loaded = true
            }
        }
    }

    fun changeTime(value: LocalDateTime) = changed { at = value }

    /** Logs (or saves the edit) once, so a double tap can't do it twice, then reports the day. */
    fun log(onLogged: (day: LocalDate) -> Unit) {
        if (!canLog || logging) return
        logging = true
        viewModelScope.launch {
            val start = at.atZone(ZoneId.systemDefault()).toInstant()
            val exercises = items.toModels()
            val id = if (editing) {
                workouts.update(workoutId, start, start.plus(duration), exercises)
                workoutId
            } else {
                workouts.log(start, exercises)
            }
            noteRepository.replace(NoteSubject.WORKOUT, id.toString(), notes.toList())
            dirty = false
            onLogged(at.toLocalDate())
        }
    }
}
