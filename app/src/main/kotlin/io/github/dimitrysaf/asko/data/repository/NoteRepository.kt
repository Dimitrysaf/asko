package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.local.dao.NoteDao
import io.github.dimitrysaf.asko.data.local.entity.NoteEntity
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.model.NoteType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Notes on routines, exercises and workouts. */
class NoteRepository(private val dao: NoteDao) {
    /** A subject's notes, oldest first, following changes. */
    fun notes(subject: NoteSubject, id: String): Flow<List<Note>> =
        dao.observe(subject.name, id).map { list -> list.map { it.toModel() } }

    /** The notes of several subjects together: an exercise's versions in each library are one exercise. */
    fun notes(subject: NoteSubject, ids: List<String>): Flow<List<Note>> =
        dao.observeAny(subject.name, ids).map { list -> list.map { it.toModel() } }

    suspend fun notesNow(subject: NoteSubject, id: String): List<Note> = dao.all(subject.name, id).map { it.toModel() }

    /** Trainers' names and gyms written before, for suggestions while typing. */
    val trainers: Flow<List<String>> = dao.trainers()
    val gyms: Flow<List<String>> = dao.gyms()

    /** Saves a new note, as added now. */
    suspend fun add(subject: NoteSubject, id: String, note: Note) {
        dao.insert(note.copy(id = 0, createdAt = Instant.now(), editedAt = null).toEntity(subject, id))
    }

    /** Saves a changed note, as edited now (it stays with what it belongs to). */
    suspend fun edit(note: Note) {
        val it = note.copy(editedAt = Instant.now()).trimmedToType()
        dao.updateContent(it.id, it.type.name, it.text, it.editedAt?.toEpochMilli(), it.trainer, it.gym, it.bodyPart, it.painLevel)
    }

    suspend fun delete(noteId: Long) = dao.delete(noteId)

    /** Everything about a subject that's gone (a deleted routine or workout). */
    suspend fun deleteFor(subject: NoteSubject, id: String) = dao.deleteFor(subject.name, id)

    /** Puts [notes] in place of the subject's notes (a logged workout's, saved with it). */
    suspend fun replace(subject: NoteSubject, id: String, notes: List<Note>) =
        dao.replace(subject.name, id, notes.map { it.toEntity(subject, id).copy(id = 0) })

    /** Every pain note, on anything, oldest first (the pain timeline). */
    /** Every workout's notes, by the workout's id (History search looks through them). */
    val workoutNotes: Flow<Map<Long, List<Note>>> = dao.observeAllOf(NoteSubject.WORKOUT.name).map { list ->
        list.groupBy({ it.subjectId.toLongOrNull() ?: -1L }, { it.toModel() })
    }

    val painNotes: Flow<List<Note>> = dao.observeOfType(NoteType.PAIN.name).map { list -> list.map { it.toModel() } }

    /** Every exercise note, by exercise id (for backups). */
    suspend fun exerciseNotes(): Map<String, List<Note>> =
        dao.allOf(NoteSubject.EXERCISE.name).groupBy({ it.subjectId }, { it.toModel() })
}

private fun NoteEntity.toModel() = Note(
    id = id,
    // An unknown name (written by a newer app version) reads as your own note.
    type = NoteType.entries.firstOrNull { it.name == type } ?: NoteType.SELF,
    text = text,
    createdAt = Instant.ofEpochMilli(createdAtMs),
    editedAt = editedAtMs?.let(Instant::ofEpochMilli),
    trainer = trainer,
    gym = gym,
    bodyPart = bodyPart,
    painLevel = painLevel,
)

private fun Note.toEntity(subject: NoteSubject, id: String) = trimmedToType().let {
    NoteEntity(
        id = it.id,
        subject = subject.name,
        subjectId = id,
        type = it.type.name,
        text = it.text,
        createdAtMs = it.createdAt.toEpochMilli(),
        editedAtMs = it.editedAt?.toEpochMilli(),
        trainer = it.trainer,
        gym = it.gym,
        bodyPart = it.bodyPart,
        painLevel = it.painLevel,
    )
}
