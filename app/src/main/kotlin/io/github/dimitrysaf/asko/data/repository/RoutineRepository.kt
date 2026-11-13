package io.github.dimitrysaf.asko.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import io.github.dimitrysaf.asko.data.local.dao.RoutineDao
import io.github.dimitrysaf.asko.data.local.toEntities
import io.github.dimitrysaf.asko.data.local.toModel
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepository(private val dao: RoutineDao, private val notes: NoteRepository, scope: CoroutineScope) {
    /** All routines in their order, cached for as long as the app runs; null until first read. */
    val routinesState: StateFlow<List<Routine>?> =
        dao.observeAll().map { list -> list.map { it.toModel() } }.stateIn(scope, SharingStarted.Lazily, null)

    /** All routines, in the order they're listed (from the cache). */
    val routines: Flow<List<Routine>> = routinesState.filterNotNull()

    /** One routine; emits null if it doesn't exist (or was deleted). */
    fun routine(id: Long): Flow<Routine?> = dao.observe(id).map { it?.toModel() }

    /** Creates (id 0) or updates the routine. Returns its id. */
    suspend fun save(routine: Routine): Long {
        val (entity, exercises) = routine.toEntities()
        return dao.save(entity, exercises)
    }

    /**
     * A copy of routine [id] called [name], with its exercises, sets and notes, at the end of the
     * list. Returns the copy's id, or null when the routine is gone.
     */
    suspend fun duplicate(id: Long, name: String): Long? {
        val routine = dao.observe(id).first()?.toModel() ?: return null
        val copyId = save(routine.copy(id = 0, name = name))
        notes.replace(NoteSubject.ROUTINE, copyId.toString(), notes.notesNow(NoteSubject.ROUTINE, id.toString()))
        return copyId
    }

    /**
     * Deletes the routine and its notes. Returns what puts it all back as it was (same id, place in
     * the list and folder, its notes), for Undo.
     */
    suspend fun delete(id: Long): suspend () -> Unit {
        val routine = dao.observe(id).first()?.toModel()
        val position = dao.positionOf(id)
        val folder = dao.folderOf(id)
        val saved = notes.notesNow(NoteSubject.ROUTINE, id.toString())
        dao.delete(id)
        notes.deleteFor(NoteSubject.ROUTINE, id.toString())
        return {
            if (routine != null) {
                save(routine)
                position?.let { dao.setPosition(id, it) }
                dao.setFolder(id, folder)
                notes.replace(NoteSubject.ROUTINE, id.toString(), saved)
            }
        }
    }

    /** Moves routine [id] into [folder] (blank or null: out of any folder). */
    suspend fun setFolder(id: Long, folder: String?) = dao.setFolder(id, folder?.trim()?.ifEmpty { null })

    /** Saves the list's order (routine ids, first to last). */
    suspend fun setOrder(ids: List<Long>) = dao.setOrder(ids)
}
