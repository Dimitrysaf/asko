package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Test

class MatchingTest {
    private fun exercise(name: String, category: String = "Chest", equipment: List<String> = emptyList()) =
        Exercise(
            id = name, name = name, category = category, primaryMuscles = emptyList(), secondaryMuscles = emptyList(),
            equipment = equipment, animationUrl = "", instructions = emptyList(),
        )

    // Sorted by name, as the catalog delivers them.
    private val library = listOf(
        exercise("Barbell Bench Press", equipment = listOf("Barbell")),
        exercise("Bench Dips On Floor", category = "Arms"),
        exercise("Bench Press", equipment = listOf("Barbell")),
        exercise("Jump Squat", category = "Legs"),
        exercise("Lying Triceps Extension", category = "Arms"),
        exercise("Push-Up"),
        exercise("Squat", category = "Legs"),
    )

    private fun names(query: String, group: String? = null, equipment: String? = null) =
        library.matching(query, group, equipment).map { it.name }

    @Test
    fun `best match first, then word starts`() {
        assertEquals(listOf("Bench Press", "Barbell Bench Press"), names("bench press"))
        assertEquals(listOf("Bench Dips On Floor", "Bench Press", "Barbell Bench Press"), names("bench"))
    }

    @Test
    fun `words match in any order`() {
        assertEquals(listOf("Barbell Bench Press", "Bench Press"), names("press bench"))
        assertEquals(listOf("Lying Triceps Extension"), names("triceps lying"))
    }

    @Test
    fun `hyphens, spaces and plurals do not matter`() {
        listOf("push ups", "pushups", "push-ups", "Push up", "pushup").forEach {
            assertEquals(it, listOf("Push-Up"), names(it))
        }
    }

    @Test
    fun `words ending in -ing also match without it`() {
        assertEquals(listOf("Jump Squat"), names("jumping squats"))
        assertEquals(listOf("Jump Squat"), names("Squat jumping"))
        assertEquals(listOf("Lying Triceps Extension"), names("lying"))
    }

    @Test
    fun `filters combine with search and blank query returns everything in order`() {
        assertEquals(library.map { it.name }, names("  "))
        assertEquals(listOf("Bench Dips On Floor"), names("bench", group = "Arms"))
        assertEquals(listOf("Bench Press", "Barbell Bench Press"), names("bench", equipment = "Barbell"))
    }
}
