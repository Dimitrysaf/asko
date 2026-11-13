package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.local.dao.CustomExerciseDao
import io.github.dimitrysaf.asko.data.local.entity.CustomExerciseEntity
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.Muscle
import io.github.dimitrysaf.asko.data.model.mainMuscles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

/** The exercises you made: the "Your exercises" library. */
class CustomExerciseRepository(private val dao: CustomExerciseDao, scope: CoroutineScope) {
    /** All of them (deleted ones too, marked retired), by name; null until first read. */
    val exercises: StateFlow<List<Exercise>?> =
        dao.observeAll().map { list -> list.map { it.toModel() } }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Creates ([Exercise.id] empty) or updates it; its muscle group comes from its first main
     * muscle. Returns its id.
     */
    suspend fun save(exercise: Exercise): String {
        val id = exercise.id.ifEmpty { ID_PREFIX + UUID.randomUUID() }
        val existing = dao.all().firstOrNull { it.id == id }
        dao.upsert(exercise.copy(id = id).toEntity(createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis()))
        return id
    }

    /** Deletes it from the library; routines and workouts that have it keep showing it. */
    suspend fun retire(id: String) = dao.retire(id)

    /** Brings back an exercise deleted (retired) by mistake, for Undo. */
    suspend fun unretire(id: String) = dao.unretire(id)

    /** Every one, as stored (for backups). */
    suspend fun allNow(): List<CustomExerciseEntity> = dao.all()

    /** Puts these back (restoring a backup), over any with the same id. */
    suspend fun restore(exercises: List<CustomExerciseEntity>) = dao.upsertAll(exercises)

    companion object {
        const val ID_PREFIX = "custom:"
    }
}

private fun CustomExerciseEntity.toModel() = Exercise(
    id = id,
    name = name,
    category = category,
    primaryMuscles = primaryMuscles.lines().filter { it.isNotBlank() },
    secondaryMuscles = secondaryMuscles.lines().filter { it.isNotBlank() },
    equipment = equipment.lines().filter { it.isNotBlank() },
    animationUrl = "",
    instructions = instructions.lines().map { it.trim() }.filter { it.isNotEmpty() },
    source = ExerciseSource.CUSTOM,
    retired = retired,
)

private fun Exercise.toEntity(createdAtMs: Long) = CustomExerciseEntity(
    id = id,
    name = name.trim(),
    category = Muscle.entries.firstOrNull { it in mainMuscles }?.bodyPart ?: category,
    primaryMuscles = primaryMuscles.joinToString("\n"),
    secondaryMuscles = secondaryMuscles.joinToString("\n"),
    equipment = equipment.joinToString("\n"),
    instructions = instructions.joinToString("\n"),
    retired = retired,
    createdAtMs = createdAtMs,
)
