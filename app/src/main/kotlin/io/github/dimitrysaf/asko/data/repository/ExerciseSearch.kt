package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.model.Exercise

/**
 * Exercises in the chosen muscle group and equipment (null = any) whose name contains every word
 * of [query], in any order. A word ending in "-ing" also counts without it, so everyday names find
 * the library's: "jumping squats" finds "Jump Squat". Best matches first: name starts with the query, then a word in the
 * name starts with it, then the rest.
 * Alphabetical within each group (the list arrives sorted and the sort is stable).
 */
fun List<Exercise>.matching(query: String, muscleGroup: String?, equipment: String?): List<Exercise> {
    // "push ups", "pushups" and "push-ups" all find "Push-Up": compare without spaces, hyphens or a plural "s".
    val words = query.split(WHITESPACE).map { it.squashed().removeSuffix("s") }.filter { it.isNotEmpty() }
    return mapNotNull { exercise ->
        if (muscleGroup != null && exercise.category != muscleGroup) return@mapNotNull null
        if (equipment != null && equipment !in exercise.equipment) return@mapNotNull null
        exercise.searchRank(words)?.let { exercise to it }
    }.sortedBy { it.second }.map { it.first }
}

/** Lower is better; null = no match. */
private fun Exercise.searchRank(words: List<String>): Int? {
    if (words.isEmpty()) return 0
    val squashedName = name.squashed()
    return when {
        !words.all { word -> word.forms().any { it in squashedName } } -> null
        squashedName.startsWith(words.joinToString("")) -> 0
        name.lowercase().split(NON_WORD).any { part -> words.first().forms().any { part.startsWith(it) } } -> 1
        else -> 2
    }
}

/**
 * A search word as typed and without an "-ing" ending: "jumping" → "jump", "running" → "run" (a
 * doubled last letter goes too). Short words like "swing" or "lying" stay as they are.
 */
private fun String.forms(): List<String> {
    if (length < 6 || !endsWith("ing")) return listOf(this)
    val stem = dropLast(3)
    val undoubled = stem.dropLast(1).takeIf { stem.length >= 4 && stem.last() == stem[stem.length - 2] }
    return listOfNotNull(this, stem, undoubled)
}

/** Lowercase letters and digits only: "Push-Up" → "pushup". */
private fun String.squashed() = lowercase().replace(NON_WORD, "")

private val WHITESPACE = Regex("\\s+")
private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
