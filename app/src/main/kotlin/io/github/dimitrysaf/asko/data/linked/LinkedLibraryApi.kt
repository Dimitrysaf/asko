package io.github.dimitrysaf.asko.data.linked

import io.github.dimitrysaf.asko.data.freeexercisedb.bodyPart
import io.github.dimitrysaf.asko.data.freeexercisedb.equipmentName
import io.github.dimitrysaf.asko.data.freeexercisedb.muscle
import io.github.dimitrysaf.asko.data.importing.parseCsv
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.wger.WgerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException

/**
 * Reads a library you added from its link, whatever shape it's in, as long as it lists
 * exercises: a JSON list (free-exercise-db's, ExerciseDB's, or anything close: a name, muscles,
 * equipment, how-to, pictures), the same list inside an object ("exercises", "data", "items"…,
 * following its next pages), a wger exercise list (wger's API, any server), or a CSV file with a
 * header row. Field names are matched loosely (primaryMuscles, target, targetMuscles, muscles…).
 */
object LinkedLibraryApi {
    private val json = Json { ignoreUnknownKeys = true }

    /** How many pages of a paged list are followed at most. */
    private const val MAX_PAGES = 100

    /**
     * Every exercise at [url], sorted by name: as [source], ids prefixed with [idPrefix], relative
     * picture paths read against [imagesBase]. Throws [IOException] when it can't be reached or
     * has no exercises in a shape it knows.
     */
    suspend fun fetchExercises(url: String, imagesBase: String, idPrefix: String, source: ExerciseSource): List<Exercise> =
        withContext(Dispatchers.IO) {
            val text = WgerApi.get(url).trim()
            val exercises = when {
                text.startsWith("[") || text.startsWith("{") -> fromJson(text, imagesBase, idPrefix, source)
                else -> fromCsv(text, imagesBase, idPrefix, source)
            }
            if (exercises.isEmpty()) throw IOException("No exercises at $url")
            exercises.distinctBy { it.id }.sortedBy { it.name.lowercase() }
        }

    private fun fromJson(text: String, imagesBase: String, idPrefix: String, source: ExerciseSource): List<Exercise> {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw IOException("Not JSON", e)
        }
        // A wger list: pages of exercises with their names in translations.
        if (root is JsonObject && (root["results"] as? JsonArray)?.firstOrNull()?.let { it is JsonObject && "translations" in it } == true) {
            return WgerApi.readPages(text, idPrefix, source)
        }
        val items = mutableListOf<JsonObject>()
        var page: JsonElement? = root
        var pages = 0
        while (page != null && pages < MAX_PAGES) {
            items += listIn(page)
            pages++
            page = nextPage(page)?.let { next ->
                try {
                    json.parseToJsonElement(WgerApi.get(next))
                } catch (e: SerializationException) {
                    null
                }
            }
        }
        return items.mapNotNull { it.toExercise(imagesBase, idPrefix, source) }
    }

    /** The exercises in a page: the list itself, or the first list of objects inside it. */
    private fun listIn(page: JsonElement): List<JsonObject> = when (page) {
        is JsonArray -> page.filterIsInstance<JsonObject>()
        is JsonObject -> LIST_KEYS.firstNotNullOfOrNull { (page[it] as? JsonArray)?.filterIsInstance<JsonObject>()?.takeIf(List<JsonObject>::isNotEmpty) }
            ?: page.values.firstNotNullOfOrNull { (it as? JsonArray)?.filterIsInstance<JsonObject>()?.takeIf(List<JsonObject>::isNotEmpty) }
            ?: emptyList()
        else -> emptyList()
    }

    /** The next page's link, if the page names one ("next", "nextPage", "metadata.nextPage"…). */
    private fun nextPage(page: JsonElement): String? {
        if (page !is JsonObject) return null
        val holders = listOf(page) + listOf("metadata", "meta", "pagination", "links").mapNotNull { page[it] as? JsonObject }
        return holders.firstNotNullOfOrNull { holder ->
            listOf("next", "nextPage", "next_page", "nextUrl").firstNotNullOfOrNull { (holder[it] as? JsonPrimitive)?.contentOrNull }
        }?.takeIf { it.startsWith("http") }
    }

    private fun fromCsv(text: String, imagesBase: String, idPrefix: String, source: ExerciseSource): List<Exercise> {
        val delimiter = if (text.lineSequence().first().count { it == ';' } > text.lineSequence().first().count { it == ',' }) ';' else ','
        val rows = parseCsv(text, delimiter)
        val header = rows.firstOrNull()?.map { it.trim() } ?: return emptyList()
        return rows.drop(1).mapNotNull { row ->
            // Each row as an object of strings, so CSV and JSON read the same way.
            val fields = header.zip(row).associate { (key, value) -> key to JsonPrimitive(value) as JsonElement }
            JsonObject(fields).toExercise(imagesBase, idPrefix, source)
        }
    }

    private fun JsonObject.toExercise(imagesBase: String, idPrefix: String, source: ExerciseSource): Exercise? {
        val name = text(NAME_KEYS)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val primary = words(PRIMARY_KEYS)
        val pictures = words(IMAGE_KEYS).map { resolve(it, imagesBase) }
        val gif = text(ANIMATION_KEYS)?.let { resolve(it, imagesBase) } ?: pictures.firstOrNull { it.endsWith(".gif", ignoreCase = true) }
        val category = text(CATEGORY_KEYS)?.let(::bodyPartName)?.takeIf { it in BODY_PARTS } ?: bodyPart(primary.firstOrNull()?.lowercase())
        return Exercise(
            id = idPrefix + (text(ID_KEYS)?.takeIf { it.isNotBlank() } ?: name.lowercase().replace(Regex("[^a-z0-9]+"), "-")),
            name = name,
            category = category,
            primaryMuscles = primary.map { muscle(it.lowercase()) },
            secondaryMuscles = words(SECONDARY_KEYS).map { muscle(it.lowercase()) },
            equipment = words(EQUIPMENT_KEYS).map { equipmentName(it.lowercase()) },
            animationUrl = gif.orEmpty(),
            instructions = steps(INSTRUCTION_KEYS),
            source = source,
            photos = if (gif != null) emptyList() else pictures,
        )
    }

    /** The first of [keys] holding text (or a number). */
    private fun JsonObject.text(keys: List<String>): String? = keys.firstNotNullOfOrNull { key ->
        when (val value = this[key]) {
            is JsonPrimitive -> value.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonArray -> (value.firstOrNull() as? JsonPrimitive)?.contentOrNull
            else -> null
        }
    }

    /**
     * The first of [keys] holding a list of words: an array of strings or of objects (their
     * "name_en", "name", "url" or "image"), or one string split at commas, semicolons or bars.
     */
    private fun JsonObject.words(keys: List<String>): List<String> {
        for (key in keys) {
            val found = when (val value = this[key]) {
                is JsonArray -> value.mapNotNull { item ->
                    when (item) {
                        is JsonPrimitive -> item.contentOrNull
                        is JsonObject -> listOf("name_en", "name", "url", "image").firstNotNullOfOrNull { (item[it] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
                        else -> null
                    }
                }
                is JsonPrimitive -> value.contentOrNull?.split(',', ';', '|').orEmpty()
                else -> emptyList()
            }.map { it.trim() }.filter { it.isNotEmpty() }
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    /** How-to steps: an array, or text split into lines (HTML tags dropped). */
    private fun JsonObject.steps(keys: List<String>): List<String> {
        for (key in keys) {
            val value = this[key] ?: continue
            val lines = when (value) {
                is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                is JsonPrimitive -> value.contentOrNull.orEmpty().split(Regex("(?i)</li>|</p>|<br\\s*/?>|\\n"))
                else -> emptyList()
            }.map { it.replace(Regex("<[^>]+>"), "").trim() }.filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) return lines
        }
        return emptyList()
    }

    private fun resolve(path: String, imagesBase: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path else imagesBase + path.removePrefix("/")

    /** A category in ExerciseDB's words where it's one of them ("upper legs" → "Upper Legs"). */
    private fun bodyPartName(category: String): String =
        category.trim().split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }

    private val BODY_PARTS = setOf("Back", "Cardio", "Chest", "Lower Arms", "Lower Legs", "Neck", "Shoulders", "Upper Arms", "Upper Legs", "Waist")

    private val LIST_KEYS = listOf("exercises", "data", "items", "results", "records")
    private val NAME_KEYS = listOf("name", "title", "exercise", "exercise_name", "exerciseName")
    private val ID_KEYS = listOf("id", "exerciseId", "exercise_id", "uuid", "_id", "slug")
    private val PRIMARY_KEYS = listOf("primaryMuscles", "primary_muscles", "targetMuscles", "target_muscles", "target", "muscles", "muscle", "primaryMuscle", "muscle_group", "muscleGroup")
    private val SECONDARY_KEYS = listOf("secondaryMuscles", "secondary_muscles", "muscles_secondary", "secondary", "synergists")
    private val EQUIPMENT_KEYS = listOf("equipment", "equipments", "equipment_required", "equipmentRequired")
    private val CATEGORY_KEYS = listOf("bodyPart", "bodyParts", "body_part", "category")
    private val INSTRUCTION_KEYS = listOf("instructions", "steps", "how_to", "howTo", "description")
    private val IMAGE_KEYS = listOf("images", "photos", "image", "imageUrl", "image_url", "thumbnail", "picture")
    private val ANIMATION_KEYS = listOf("gifUrl", "gif_url", "gif", "animationUrl", "animation")
}
