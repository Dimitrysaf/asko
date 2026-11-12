package io.github.dimitrysaf.asko.data.stats

import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.lib.time.localDate
import java.time.Duration
import java.time.LocalDate

/** What a year held: its workouts added up, and the sets done each day (for the heat map). */
data class YearReview(
    val year: Int,
    val workouts: Int,
    /** Days with at least one workout. */
    val activeDays: Int,
    /** Time spent, from workouts with a known length. */
    val time: Duration,
    val sets: Int,
    val reps: Int,
    /** Kilos lifted in all (reps × weight). */
    val volumeKg: Double,
    /** Personal records set. */
    val records: Int,
    /** The exercise with the most sets, if any. */
    val topExerciseId: String?,
    val setsByDay: Map<LocalDate, Int>,
)

/** The years with workouts, latest first. */
fun List<Workout>.years(): List<Int> = map { it.startedAt.localDate().year }.distinct().sortedDescending()

/** [year] in review, from these workouts (all of history: records are counted against it). */
fun List<Workout>.yearReview(year: Int): YearReview {
    val inYear = filter { it.startedAt.localDate().year == year }
    val sets = inYear.flatMap { w -> w.exercises.flatMap { e -> e.sets.map { e.exerciseId to it } } }
    return YearReview(
        year = year,
        workouts = inYear.size,
        activeDays = inYear.map { it.startedAt.localDate() }.distinct().size,
        time = inYear.mapNotNull { it.duration }.fold(Duration.ZERO, Duration::plus),
        sets = sets.size,
        reps = sets.sumOf { it.second.reps ?: 0 },
        volumeKg = sets.sumOf { (_, s) -> (s.reps ?: 0) * (s.weightKg ?: 0.0) },
        records = inYear.sumOf { recordsIn(it, this).size },
        topExerciseId = sets.groupingBy { it.first }.eachCount().maxByOrNull { it.value }?.key,
        setsByDay = inYear.groupBy { it.startedAt.localDate() }.mapValues { (_, ws) -> ws.sumOf { it.setCount } },
    )
}
