package io.github.dimitrysaf.asko.data.model

/**
 * The muscles wger has images for (its muscle API: https://wger.de/api/v2/muscle/), by its
 * [wgerId], named as the libraries name them ([label]). [front]: drawn on the front of the body,
 * else the back. [bodyPart] is the muscle group they're filed under (the libraries' categories,
 * Discovery's Muscle group).
 */
enum class Muscle(val wgerId: Int, val label: String, val bodyPart: String, val front: Boolean) {
    TRAPEZIUS(9, "Trapezius", "Back", front = false),
    DELTOIDS(2, "Deltoids", "Shoulders", front = true),
    PECTORALS(4, "Pectorals", "Chest", front = true),
    BICEPS(1, "Biceps", "Upper Arms", front = true),
    BRACHIALIS(13, "Brachialis", "Upper Arms", front = true),
    TRICEPS(5, "Triceps", "Upper Arms", front = false),
    SERRATUS(3, "Serratus Anterior", "Waist", front = true),
    ABDOMINALS(6, "Abdominals", "Waist", front = true),
    OBLIQUES(14, "Obliques", "Waist", front = true),
    LATS(12, "Latissimus Dorsi", "Back", front = false),
    GLUTES(8, "Glutes", "Upper Legs", front = false),
    QUADRICEPS(10, "Quadriceps", "Upper Legs", front = true),
    HAMSTRINGS(11, "Hamstrings", "Upper Legs", front = false),
    CALVES(7, "Calves", "Lower Legs", front = false),
    SOLEUS(15, "Soleus", "Lower Legs", front = false),
    ;

    companion object {
        /** The muscle a library's name means ("Core" is the abs, "Upper Back" the trapezius…); null when wger has none. */
        fun of(name: String): Muscle? {
            val key = name.trim().lowercase()
            return entries.firstOrNull { it.label.lowercase() == key } ?: ALIASES[key]
        }

        /** The muscles of a muscle group (a library category); none for Cardio, Lower Arms and Neck. */
        fun ofBodyPart(bodyPart: String): Set<Muscle> =
            entries.filter { it.bodyPart.equals(bodyPart, ignoreCase = true) }.toSet() +
                (if (bodyPart.equals("Neck", ignoreCase = true)) setOf(TRAPEZIUS) else emptySet())

        private val ALIASES = mapOf(
            "abs" to ABDOMINALS,
            "core" to ABDOMINALS,
            "upper back" to TRAPEZIUS,
            "middle back" to TRAPEZIUS,
            "rhomboids" to TRAPEZIUS,
            "back" to LATS,
            "lats" to LATS,
            "traps" to TRAPEZIUS,
            "levator scapulae" to TRAPEZIUS,
            "rear deltoids" to DELTOIDS,
            "rotator cuff" to DELTOIDS,
            "shoulders" to DELTOIDS,
            "chest" to PECTORALS,
            "quads" to QUADRICEPS,
        )
    }
}

/** The exercise's muscles on the body map: its main ones, and the others it also works. */
val Exercise.mainMuscles: Set<Muscle> get() = primaryMuscles.mapNotNull(Muscle::of).toSet()
val Exercise.otherMuscles: Set<Muscle> get() = secondaryMuscles.mapNotNull(Muscle::of).toSet() - mainMuscles
