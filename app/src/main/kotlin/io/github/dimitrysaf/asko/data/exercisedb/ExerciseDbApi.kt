package io.github.dimitrysaf.asko.data.exercisedb

import java.util.concurrent.atomic.AtomicInteger
import io.github.dimitrysaf.asko.data.model.Exercise
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads the exercise library from ExerciseDB's free API (oss.exercisedb.dev): 1,500 exercises,
 * each with an animated demo. No account or key needed.
 */
object ExerciseDbApi {
    /** The free API's animations are 180 × 180 px GIFs; larger ones are paid-only. */
    const val GIF_SIZE_PX = 180

    // 25 is the most the free API returns per page.
    private const val EXERCISES_URL = "https://oss.exercisedb.dev/api/v1/exercises?limit=25"
    private const val MAX_PAGES = 200 // ~4× today's library; stops a misbehaving cursor from looping forever
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Every exercise that has its animation, one per name, sorted by name. Throws [IOException]
     * when ExerciseDB can't be reached. [onProgress] gets how far it is: each exercise counts once
     * when listed and once when its animation is checked, out of twice the API's reported total.
     */
    suspend fun fetchExercises(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<Exercise> = withContext(Dispatchers.IO) {
        coroutineScope {
            val done = AtomicInteger(0)
            var total = EXPECTED_TOTAL * 2
            // Some listed GIFs don't exist on ExerciseDB's server. Each page's are checked while the
            // next page is fetched (mostly during the API's rate-limit waits), so this costs little time.
            val gate = Semaphore(PARALLEL_CHECKS)
            val checked = mutableListOf<Deferred<ExerciseDbExercise?>>()
            var cursor: String? = null
            var pages = 0
            do {
                if (++pages > MAX_PAGES) throw IOException("ExerciseDB paging did not end")
                val url = EXERCISES_URL + (cursor?.let { "&after=" + URLEncoder.encode(it, "UTF-8") } ?: "")
                val page = json.decodeFromString<ExerciseDbPage>(get(url))
                page.meta.total?.let { total = it * 2 }
                onProgress(done.addAndGet(page.data.size), total)
                page.data.forEach { exercise ->
                    checked += async {
                        gate.withPermit { exercise.takeIf { animationExists(it.gifUrl) } }
                            .also { onProgress(done.incrementAndGet(), total) }
                    }
                }
                cursor = page.meta.nextCursor.takeIf { page.meta.hasNextPage }
            } while (cursor != null)
            checked.awaitAll().filterNotNull()
                .map { it.toExercise() }
                .sortedBy { it.name.lowercase() }
                .distinctBy { it.name.lowercase() } // ExerciseDB lists some exercises twice
        }
    }

    /** False only when the server says the file is gone; any other trouble keeps the exercise. */
    private fun animationExists(url: String): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        return try {
            connection.responseCode !in listOf(HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_GONE)
        } catch (e: IOException) {
            true
        } finally {
            connection.disconnect()
        }
    }

    /**
     * The free API allows about 8 requests in a row, then answers 429 with "Retry-After: 10".
     * A full sync is 60 pages, so wait as asked and retry the same page. First sync takes
     * about a minute and a half; later loads read the saved copy.
     */
    private suspend fun get(url: String): String {
        repeat(MAX_RETRIES) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            try {
                when (val code = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> return connection.inputStream.bufferedReader().use { it.readText() }
                    HTTP_TOO_MANY_REQUESTS -> {
                        val waitSeconds = connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 10
                        delay(waitSeconds.coerceIn(1, 60) * 1_000)
                    }
                    else -> throw IOException("ExerciseDB answered $code for $url")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("ExerciseDB kept rate-limiting $url")
    }

    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val MAX_RETRIES = 6
    private const val PARALLEL_CHECKS = 8

    /** The library's size, for progress until the first page reports the real total. */
    const val EXPECTED_TOTAL = 1500
}

private val STEP_PREFIX = Regex("^Step:\\s*\\d+\\s*")

private fun ExerciseDbExercise.toExercise() = Exercise(
    id = exerciseId,
    name = name.titleCase(),
    category = bodyParts.firstOrNull()?.titleCase().orEmpty(),
    primaryMuscles = targetMuscles.map { it.titleCase() },
    secondaryMuscles = secondaryMuscles.map { it.titleCase() },
    equipment = equipments.map { it.titleCase() },
    animationUrl = gifUrl,
    // ExerciseDB writes "Step:1 Do this"; the page numbers the steps itself.
    instructions = instructions.map { it.replace(STEP_PREFIX, "").trim() }.filter { it.isNotEmpty() },
)

/** ExerciseDB names are lowercase: "barbell bench press" → "Barbell Bench Press". */
private fun String.titleCase() = split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }

// Only the fields Asko reads.

@Serializable
private class ExerciseDbPage(val meta: ExerciseDbMeta, val data: List<ExerciseDbExercise>)

@Serializable
private class ExerciseDbMeta(val total: Int? = null, val hasNextPage: Boolean = false, val nextCursor: String? = null)

@Serializable
private class ExerciseDbExercise(
    val exerciseId: String,
    val name: String,
    val gifUrl: String,
    val bodyParts: List<String> = emptyList(),
    val equipments: List<String> = emptyList(),
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
)
