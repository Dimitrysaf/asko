package io.github.dimitrysaf.asko.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
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

    /** Deletes the routine and its notes. */
    suspend fun delete(id: Long) {
        dao.delete(id)
        notes.deleteFor(NoteSubject.ROUTINE, id.toString())
    }

    /** Saves the list's order (routine ids, first to last). */
    suspend fun setOrder(ids: List<Long>) = dao.setOrder(ids)
}
