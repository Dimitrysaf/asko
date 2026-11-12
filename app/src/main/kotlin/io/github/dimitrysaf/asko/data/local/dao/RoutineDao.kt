package io.github.dimitrysaf.asko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.dimitrysaf.asko.data.local.entity.RoutineEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineSetEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    /** In the order they were dragged into; routines from before that (all at 0) by name. */
    @Transaction
    @Query("SELECT * FROM routines ORDER BY position, name COLLATE NOCASE")
    fun observeAll(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observe(id: Long): Flow<RoutineWithExercises?>

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity): Long

    /** Their sets go too (cascade). */
    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercises(routineId: Long)

    @Insert
    suspend fun insertExercise(exercise: RoutineExerciseEntity): Long

    @Insert
    suspend fun insertSets(sets: List<RoutineSetEntity>)

    /**
     * Saves the routine and replaces its exercises and sets, all or nothing. Returns its id.
     * (Upsert returns -1 when it updated, so an existing id is kept.) Ids and positions inside
     * [exercises] are filled in here.
     */
    @Transaction
    suspend fun save(routine: RoutineEntity, exercises: List<Pair<RoutineExerciseEntity, List<RoutineSetEntity>>>): Long {
        // An edited routine keeps its place and its folder; a new one goes to the end of the list.
        val position = if (routine.id != 0L) positionOf(routine.id) ?: nextPosition() else nextPosition()
        val folder = if (routine.id != 0L) folderOf(routine.id) else routine.folder
        val inserted = upsertRoutine(routine.copy(position = position, folder = folder))
        val routineId = if (routine.id != 0L) routine.id else inserted
        deleteExercises(routineId)
        exercises.forEachIndexed { i, (exercise, sets) ->
            val exerciseId = insertExercise(exercise.copy(id = 0, routineId = routineId, position = i))
            insertSets(sets.mapIndexed { j, set -> set.copy(id = 0, routineExerciseId = exerciseId, position = j) })
        }
        return routineId
    }

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT folder FROM routines WHERE id = :id")
    suspend fun folderOf(id: Long): String?

    /** Moves a routine into [folder] (null: out of any). */
    @Query("UPDATE routines SET folder = :folder WHERE id = :id")
    suspend fun setFolder(id: Long, folder: String?)

    @Query("SELECT position FROM routines WHERE id = :id")
    suspend fun positionOf(id: Long): Int?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routines")
    suspend fun nextPosition(): Int

    @Query("UPDATE routines SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    /** Puts the routines in this order (their ids, first to last). */
    @Transaction
    suspend fun setOrder(ids: List<Long>) {
        ids.forEachIndexed { i, id -> setPosition(id, i) }
    }

    /** Every routine with its exercises and sets (for restoring a backup). */
    @Transaction
    suspend fun deleteAll() {
        deleteAllSets()
        deleteAllExercises()
        deleteAllRoutines()
    }

    @Query("DELETE FROM routine_sets")
    suspend fun deleteAllSets()

    @Query("DELETE FROM routine_exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()
}
