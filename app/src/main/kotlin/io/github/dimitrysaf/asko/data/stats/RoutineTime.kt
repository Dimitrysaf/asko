package io.github.dimitrysaf.asko.data.stats

import io.github.dimitrysaf.asko.data.model.Workout
import java.time.Duration

/**
 * How long workouts done from each routine took on average, by the routine's name (as named when
 * done). Only workouts with a known duration count: ones logged afterwards have none.
 */
fun List<Workout>.averageDurations(): Map<String, Duration> =
    mapNotNull { w -> w.routineName?.let { name -> w.duration?.let { name to it } } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, times) -> Duration.ofSeconds(times.sumOf { it.seconds } / times.size) }
