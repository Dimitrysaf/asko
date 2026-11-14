package io.github.dimitrysaf.asko.data.wger

import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.Muscle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads wger's exercise library (wger.de, community made, CC BY-SA): about 950 exercises, some with
 * photos, read page by page from its public API. No account or key. Only exercises with an
 * English name are kept; their how-to comes from the English description.
 */
object WgerApi {
    private const val FIRST_PAGE = "https://wger.de/api/v2/exerciseinfo/?limit=200&offset=0"

    /** Prefix of its exercises' ids in Asko, so they never meet another library's. */
    const val ID_PREFIX = "wger:"

    /** Roughly how many it has, for the download's progress. */
    const val EXPECTED_TOTAL = 970

    private const val ENGLISH = 2

    private val json = Json { ignoreUnknownKeys = true }

    /** Every exercise with an English name, sorted by name. Throws [IOException] when it can't be reached. */
    suspend fun fetchExercises(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): List<Exercise> = withContext(Dispatchers.IO) {
        readPages(get(FIRST_PAGE), ID_PREFIX, ExerciseSource.WGER, onProgress)
    }

    /**
     * A wger exercise list (wger's own, or another wger server's), from its first page's [text]
     * on through the rest: as [source], ids prefixed with [idPrefix]. Throws [IOException] when a
     * page can't be reached or isn't wger's.
     */
    fun readPages(
        text: String,
        idPrefix: String,
        source: ExerciseSource,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<Exercise> {
        val all = mutableListOf<WgerExercise>()
        var page = parse(text)
        while (true) {
            all += page.results
            onProgress(all.size, page.count)
            page = parse(get(page.next ?: break))
        }
        return all.mapNotNull { it.toExercise(idPrefix, source) }.sortedBy { it.name.lowercase() }
    }

    private fun parse(text: String): WgerPage = try {
        json.decodeFromString<WgerPage>(text)
    } catch (e: SerializationException) {
        throw IOException("Not a wger exercise list", e)
    }

    internal fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("wger answered ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun WgerExercise.toExercise(idPrefix: String, source: ExerciseSource): Exercise? {
        val english = translations.firstOrNull { it.language == ENGLISH } ?: return null
        val name = english.name.trim().ifEmpty { return null }
        return Exercise(
            id = idPrefix + id,
            name = name,
            category = bodyPart(category.name),
            primaryMuscles = muscles.map { it.label() },
            secondaryMuscles = muscles_secondary.map { it.label() },
            equipment = equipment.mapNotNull { equipmentName(it.name) },
            animationUrl = "",
            instructions = steps(english.description),
            source = source,
            // The main photo first.
            photos = images.sortedByDescending { it.is_main }.map { it.image },
        )
    }
}

// Its words, put in ExerciseDB's, so the Muscle group and Equipment filters and "similar
// exercises" work across every library.

/** A muscle by wger's id, as the other libraries name it; else its English (or Latin) name. */
private fun WgerMuscle.label(): String =
    Muscle.entries.firstOrNull { it.wgerId == id }?.label ?: name_en.ifEmpty { name }

/** Its categories as ExerciseDB's body parts. */
private fun bodyPart(category: String): String = when (category) {
    "Abs" -> "Waist"
    "Arms" -> "Upper Arms"
    "Calves" -> "Lower Legs"
    "Legs" -> "Upper Legs"
    else -> category // Back, Cardio, Chest, Shoulders
}

private fun equipmentName(name: String): String? = when (name) {
    "none (bodyweight exercise)" -> "Bodyweight"
    "SZ-Bar" -> "EZ Bar"
    "Cable machine" -> "Cable"
    "Gym mat" -> null
    else -> name
}

/** Its description (HTML) as how-to steps: one per list item or paragraph, tags and entities gone. */
private fun steps(html: String): List<String> =
    html.split(Regex("(?i)</li>|</p>|<br\\s*/?>|\\n"))
        .map { part ->
            part.replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">")
                .trim()
        }
        .filter { it.isNotEmpty() }

// Only the fields Asko reads (wger's names, hence the underscores).

@Serializable
private class WgerPage(val count: Int = 0, val next: String? = null, val results: List<WgerExercise> = emptyList())

@Serializable
private class WgerExercise(
    val id: Int,
    val category: WgerName,
    val muscles: List<WgerMuscle> = emptyList(),
    @Suppress("PropertyName") val muscles_secondary: List<WgerMuscle> = emptyList(),
    val equipment: List<WgerName> = emptyList(),
    val images: List<WgerImage> = emptyList(),
    val translations: List<WgerTranslation> = emptyList(),
)

@Serializable
private class WgerName(val name: String = "")

@Serializable
@Suppress("PropertyName")
private class WgerMuscle(val id: Int, val name: String = "", val name_en: String = "")

@Serializable
@Suppress("PropertyName")
private class WgerImage(val image: String, val is_main: Boolean = false)

@Serializable
private class WgerTranslation(val language: Int, val name: String = "", val description: String = "")
