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

/** What the routine chart shows. */
enum class RoutineMetric { VOLUME, REPS, DURATION }

/**
 * Workouts done from the routine [routineName] (as named when done) over [range] up to [today]:
 * per bucket, the average of [metric] per workout (volume: kilos lifted, reps × weight; reps;
 * duration in minutes, from workouts with a known length), 0 where none was done.
 */
fun List<Workout>.routineSeries(
    routineName: String,
    metric: RoutineMetric,
    range: StatsRange,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Pair<Timeline, List<Double?>> {
    val done = filter { it.routineName == routineName }
    val timeline = timeline(range, today, done.minOfOrNull { it.startedAt }?.localDate(), firstDayOfWeek)
    val values = List(timeline.starts.size) { mutableListOf<Double>() }
    done.forEach { w ->
        val i = timeline.indexOf(w.startedAt.localDate()) ?: return@forEach
        val sets = w.exercises.flatMap { it.sets }
        val value = when (metric) {
            RoutineMetric.VOLUME -> sets.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }
            RoutineMetric.REPS -> sets.sumOf { it.reps ?: 0 }.toDouble()
            RoutineMetric.DURATION -> w.duration?.seconds?.div(60.0) ?: return@forEach
        }
        values[i] += value
    }
    // 0 where none was done, like the other charts (a line needs two points to show at all).
    return timeline to values.map { if (it.isEmpty()) 0.0 else it.average() }
}
