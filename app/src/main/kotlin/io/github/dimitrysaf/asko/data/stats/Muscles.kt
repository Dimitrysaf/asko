package io.github.dimitrysaf.asko.data.stats

import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Muscle
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.lib.time.localDate
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Sets each muscle got over [range] up to [today], by the muscle's name (a library's, or the
 * body map's when it has the muscle): a set counts once for an exercise's main muscles and half
 * for the others it works ([library] says which). Muscles with none are left out.
 */
fun List<Workout>.setsPerMuscle(
    library: Map<String, Exercise>,
    range: StatsRange,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Map<String, Double> {
    val from = timeline(range, today, minOfOrNull { it.startedAt }?.localDate(), firstDayOfWeek).starts.first()
    val sets = HashMap<String, Double>()
    filter { !it.startedAt.localDate().isBefore(from) }.forEach { w ->
        w.exercises.forEach { e ->
            val exercise = library[e.exerciseId] ?: return@forEach
            val count = e.sets.size.toDouble()
            val main = exercise.primaryMuscles.map(::shownName).toSet()
            main.forEach { sets.merge(it, count, Double::plus) }
            (exercise.secondaryMuscles.map(::shownName).toSet() - main).forEach { sets.merge(it, count / 2, Double::plus) }
        }
    }
    return sets
}

/** A muscle's name as shown: the body map's when it has the muscle ("Core" is Abdominals), else the library's. */
private fun shownName(name: String): String = Muscle.of(name)?.label ?: name.trim()
