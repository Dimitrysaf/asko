package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SimilarExercisesTest {
    private fun exercise(
        name: String,
        targets: List<String>,
        secondary: List<String> = emptyList(),
        equipment: List<String> = listOf("Barbell"),
    ) = Exercise(
        id = name, name = name, category = "Chest", primaryMuscles = targets, secondaryMuscles = secondary,
        equipment = equipment, animationUrl = "", instructions = emptyList(),
    )

    private val bench = exercise("Bench Press", listOf("Pectorals"), listOf("Triceps"))

    // Alphabetical, as the catalog delivers them.
    private val library = listOf(
        exercise("Barbell Incline Press", listOf("Pectorals"), listOf("Delts")),
        bench,
        exercise("Cable Fly", listOf("Pectorals"), listOf("Triceps"), equipment = listOf("Cable")),
        exercise("Close-Grip Press", listOf("Pectorals"), listOf("Triceps")),
        exercise("Squat", listOf("Quads")),
        exercise("Triceps Dip", listOf("Triceps"), listOf("Pectorals"), equipment = listOf("Bodyweight")),
    )

    private fun similarNames(limit: Int = 10) = library.similarTo(bench, limit).map { it.name }

    @Test
    fun `shared targets required, best overlap first, other equipment breaks ties`() {
        // Cable Fly and Close-Grip Press share target + secondary; Cable Fly wins on different equipment.
        assertEquals(listOf("Cable Fly", "Close-Grip Press", "Barbell Incline Press"), similarNames())
    }

    @Test
    fun `never lists the exercise itself and respects the limit`() {
        assertEquals(listOf("Cable Fly"), similarNames(limit = 1))
        assertFalse("Bench Press" in similarNames())
    }
}
