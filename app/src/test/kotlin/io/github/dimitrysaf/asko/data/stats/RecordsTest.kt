package io.github.dimitrysaf.asko.data.stats

import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.data.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecordsTest {
    private fun workout(id: Long, day: Long, vararg sets: PlannedSet, exerciseId: String = "bench") = Workout(
        id = id,
        startedAt = Instant.ofEpochSecond(day * 86_400),
        endedAt = null,
        note = null,
        routineName = null,
        exercises = listOf(RoutineExercise(exerciseId, 90, timed = false, sets = sets.toList())),
    )

    private fun set(kg: Double?, reps: Int?) = PlannedSet(SetType.NORMAL, reps = reps, weightKg = kg)

    @Test
    fun `first time doing an exercise sets no records`() {
        val first = workout(1, 1, set(60.0, 8))
        assertTrue(recordsIn(first, listOf(first)).isEmpty())
    }

    @Test
    fun `beating an earlier best is a record, matching it is not`() {
        val earlier = workout(1, 1, set(60.0, 8))
        val later = workout(2, 2, set(62.5, 8), set(60.0, 8))
        val records = recordsIn(later, listOf(later, earlier))
        val heaviest = records.single { it.kind == RecordKind.HEAVIEST }
        assertEquals(62.5, heaviest.value, 0.0)
        assertEquals(60.0, heaviest.previous, 0.0)
        assertTrue(records.none { it.kind == RecordKind.MOST_REPS })
    }

    @Test
    fun `later workouts don't count against an earlier one`() {
        val earlier = workout(1, 1, set(60.0, 8))
        val middle = workout(2, 2, set(65.0, 8))
        val later = workout(3, 3, set(70.0, 8))
        assertEquals(65.0, recordsIn(middle, listOf(later, middle, earlier)).single { it.kind == RecordKind.HEAVIEST }.value, 0.0)
    }

    @Test
    fun `estimated one rep max ignores sets over twelve reps`() {
        val stats = listOf(workout(1, 1, set(50.0, 20), set(60.0, 10))).statsFor("bench")
        assertEquals(60.0 * (1 + 10 / 30.0), stats.bests.getValue(RecordKind.ONE_REP_MAX), 1e-9)
        assertEquals(20.0, stats.bests.getValue(RecordKind.MOST_REPS), 0.0)
    }

    @Test
    fun `stats count sessions and sets of the exercise only`() {
        val history = listOf(
            workout(1, 1, set(60.0, 8), set(60.0, 8)),
            workout(2, 2, set(40.0, 10), exerciseId = "row"),
            workout(3, 3, set(62.5, 6)),
        )
        val stats = history.statsFor("bench")
        assertEquals(2, stats.sessions)
        assertEquals(3, stats.sets)
        assertEquals(Instant.ofEpochSecond(3 * 86_400), stats.lastDone)
    }
}
