package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.model.Exercise

/**
 * Exercises in the chosen muscle group and equipment (null = any) whose name contains every word
 * of [query], in any order. Best matches first: name starts with the query, then a word in the
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
        !words.all { it in squashedName } -> null
        squashedName.startsWith(words.joinToString("")) -> 0
        name.lowercase().split(NON_WORD).any { it.startsWith(words.first()) } -> 1
        else -> 2
    }
}

/** Lowercase letters and digits only: "Push-Up" → "pushup". */
private fun String.squashed() = lowercase().replace(NON_WORD, "")

private val WHITESPACE = Regex("\\s+")
private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
