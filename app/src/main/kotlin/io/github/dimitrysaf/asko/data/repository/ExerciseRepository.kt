package io.github.dimitrysaf.asko.data.repository

import kotlinx.coroutines.flow.StateFlow
import io.github.dimitrysaf.asko.data.model.DownloadProgress
import io.github.dimitrysaf.asko.data.catalog.ExerciseCatalog
import io.github.dimitrysaf.asko.data.model.Exercise

class ExerciseRepository(private val catalog: ExerciseCatalog) {
    /** How far the library's download is while one runs; null otherwise. */
    val downloadProgress: StateFlow<DownloadProgress?> = catalog.progress

    suspend fun all(): List<Exercise> = catalog.load()

    suspend fun byId(id: String): Exercise? = catalog.load().firstOrNull { it.id == id }

    suspend fun similarTo(exercise: Exercise, limit: Int = SIMILAR_LIMIT): List<Exercise> =
        catalog.load().similarTo(exercise, limit)

    private companion object {
        const val SIMILAR_LIMIT = 6
    }
}

/**
 * Exercises that train at least one of [exercise]'s target muscles, best first: more shared
 * target muscles, then more shared secondary muscles, then different equipment (an alternative
 * when the machine is taken). Ties keep the library's alphabetical order.
 */
fun List<Exercise>.similarTo(exercise: Exercise, limit: Int): List<Exercise> {
    val targets = exercise.primaryMuscles.toSet()
    val secondary = exercise.secondaryMuscles.toSet()
    val equipment = exercise.equipment.toSet()
    return asSequence()
        .filter { it.id != exercise.id }
        .map { candidate ->
            val sharedTargets = candidate.primaryMuscles.count { it in targets }
            val score = sharedTargets * 10 +
                candidate.secondaryMuscles.count { it in secondary } +
                (if (candidate.equipment.none { it in equipment }) 1 else 0)
            candidate to (if (sharedTargets == 0) 0 else score)
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(limit)
        .map { it.first }
        .toList()
}
