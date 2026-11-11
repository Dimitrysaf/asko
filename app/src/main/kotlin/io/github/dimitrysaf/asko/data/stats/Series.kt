package io.github.dimitrysaf.asko.data.stats

import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.lib.time.localDate
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What was done per day, week or month of a [timeline]: sets, reps and minutes (one value per
 * bucket, 0 where nothing was done), and the weight: kilos lifted in all (reps × weight), or for
 * one exercise its heaviest set (null where it wasn't done).
 */
data class TrainingSeries(
    val timeline: Timeline,
    val sets: List<Double>,
    val reps: List<Double>,
    val minutes: List<Double>,
    val weight: List<Double?>,
)

/**
 * The series for [range] up to [today]. With an [exerciseId], only that exercise: its sets and
 * reps, the time its sets were held, and its heaviest weight. Without, everything: all sets and
 * reps, how long the workouts took, and the total weight lifted.
 */
fun List<Workout>.trainingSeries(
    range: StatsRange,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    exerciseId: String? = null,
): TrainingSeries {
    val done = if (exerciseId == null) this else filter { w -> w.exercises.any { it.exerciseId == exerciseId } }
    val timeline = timeline(range, today, done.minOfOrNull { it.startedAt }?.localDate(), firstDayOfWeek)
    val size = timeline.starts.size
    val sets = DoubleArray(size)
    val reps = DoubleArray(size)
    val minutes = DoubleArray(size)
    val weight = arrayOfNulls<Double>(size)
    done.forEach { w ->
        val i = timeline.indexOf(w.startedAt.localDate()) ?: return@forEach
        val all = w.exercises.filter { exerciseId == null || it.exerciseId == exerciseId }.flatMap { it.sets }
        sets[i] += all.size
        reps[i] += all.sumOf { it.reps ?: 0 }
        if (exerciseId == null) {
            minutes[i] += (w.duration?.seconds ?: 0) / 60.0
            weight[i] = (weight[i] ?: 0.0) + all.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }
        } else {
            minutes[i] += all.sumOf { it.durationSeconds ?: 0 } / 60.0
            all.mapNotNull { it.valueFor(RecordKind.HEAVIEST) }.maxOrNull()?.let { heaviest ->
                weight[i] = maxOf(weight[i] ?: heaviest, heaviest)
            }
        }
    }
    return TrainingSeries(timeline, sets.toList(), reps.toList(), minutes.toList(), weight.toList())
}
