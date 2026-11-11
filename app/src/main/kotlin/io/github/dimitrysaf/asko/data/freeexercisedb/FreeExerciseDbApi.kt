package io.github.dimitrysaf.asko.data.freeexercisedb

import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads free-exercise-db (github.com/yuhonas/free-exercise-db, public domain): about 870
 * exercises, each with two photos (start and end position), as one JSON file. No account or key.
 */
object FreeExerciseDbApi {
    private const val BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main"
    private const val EXERCISES_URL = "$BASE/dist/exercises.json"
    private const val IMAGES_URL = "$BASE/exercises/"

    /** Prefix of its exercises' ids in Asko, so they never meet ExerciseDB's. */
    const val ID_PREFIX = "fedb:"

    private val json = Json { ignoreUnknownKeys = true }

    /** Every exercise, sorted by name. Throws [IOException] when it can't be reached. */
    suspend fun fetchExercises(): List<Exercise> = withContext(Dispatchers.IO) {
        val connection = URL(EXERCISES_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        val text = try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("free-exercise-db answered ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val list = try {
            json.decodeFromString<List<FreeExercise>>(text)
        } catch (e: SerializationException) {
            throw IOException("free-exercise-db sent something unexpected", e)
        }
        list.map { it.toExercise() }.sortedBy { it.name.lowercase() }
    }

    private fun FreeExercise.toExercise() = Exercise(
        id = ID_PREFIX + id,
        name = name.trim(),
        category = if (category == "cardio") "Cardio" else bodyPart(primaryMuscles.firstOrNull()),
        primaryMuscles = primaryMuscles.map(::muscle),
        secondaryMuscles = secondaryMuscles.map(::muscle),
        equipment = listOfNotNull(equipment?.let(::equipmentName)),
        animationUrl = "",
        instructions = instructions.map { it.trim() }.filter { it.isNotEmpty() },
        source = ExerciseSource.FREE_EXERCISE_DB,
        photos = images.map { IMAGES_URL + it },
    )
}

// Its words, put in ExerciseDB's, so the Muscle group and Equipment filters and "similar
// exercises" work across both libraries.

/** The muscle group (ExerciseDB's body part) a muscle belongs to. */
private fun bodyPart(muscle: String?): String = when (muscle) {
    "abdominals" -> "Waist"
    "abductors", "adductors", "glutes", "hamstrings", "quadriceps" -> "Upper Legs"
    "biceps", "triceps" -> "Upper Arms"
    "forearms" -> "Lower Arms"
    "calves" -> "Lower Legs"
    "chest" -> "Chest"
    "lats", "middle back", "lower back", "traps" -> "Back"
    "shoulders" -> "Shoulders"
    "neck" -> "Neck"
    else -> ""
}

private fun muscle(name: String): String = when (name) {
    "chest" -> "Pectorals"
    "lats" -> "Latissimus Dorsi"
    "lower back" -> "Erector Spinae"
    "middle back" -> "Upper Back"
    "shoulders" -> "Deltoids"
    "traps" -> "Trapezius"
    else -> name.split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
}

private fun equipmentName(name: String): String = when (name) {
    "body only" -> "Bodyweight"
    "machine" -> "Leverage Machine"
    "kettlebells" -> "Kettlebell"
    "bands" -> "Resistance Band"
    "exercise ball" -> "Stability Ball"
    "foam roll" -> "Roller"
    "e-z curl bar" -> "EZ Bar"
    else -> name.split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
}

// Only the fields Asko reads.

@Serializable
private class FreeExercise(
    val id: String,
    val name: String,
    val category: String = "",
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val images: List<String> = emptyList(),
)
