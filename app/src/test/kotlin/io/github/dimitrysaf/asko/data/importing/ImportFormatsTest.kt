package io.github.dimitrysaf.asko.data.importing

import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.lib.format.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class ImportFormatsTest {
    @Test
    fun strong() {
        val csv = """
            Date,Workout Name,Duration,Exercise Name,Set Order,Weight,Reps,Distance,Seconds,Notes,Workout Notes,RPE
            2024-01-15 07:30:00,"Push, heavy",1h 5m,Bench Press (Barbell),W,40,10,0,0,,Felt good,
            2024-01-15 07:30:00,"Push, heavy",1h 5m,Bench Press (Barbell),1,80,5,0,0,,Felt good,8
            2024-01-15 07:30:00,"Push, heavy",1h 5m,Plank,1,0,0,0,60,,Felt good,
            2024-01-15 07:30:00,"Push, heavy",1h 5m,Plank,Rest Timer,0,0,0,90,,Felt good,
            2024-01-17 18:00:00,Run,30m,Running,1,0,0,5.2,1800,,,
        """.trimIndent()
        val parsed = parseImport(csv, WeightUnit.KG, zone = ZoneOffset.UTC)!!
        assertEquals(ImportSource.STRONG, parsed.source)
        assertEquals(2, parsed.workouts.size)
        val push = parsed.workouts[0]
        assertEquals("Push, heavy", push.name)
        assertEquals(3900L, push.endedAt!!.epochSecond - push.startedAt.epochSecond)
        assertEquals("Felt good", push.note)
        assertEquals(listOf("Bench Press (Barbell)", "Plank"), push.exercises.map { it.name })
        assertEquals(SetType.WARMUP, push.exercises[0].sets[0].type)
        assertEquals(80.0, push.exercises[0].sets[1].weightKg!!, 0.001)
        assertEquals(1, push.exercises[1].sets.size) // the rest timer row is skipped
        assertEquals(60, push.exercises[1].sets[0].seconds)
        assertEquals(5200.0, parsed.workouts[1].exercises[0].sets[0].meters!!, 0.001)
    }

    @Test
    fun strongSemicolonsInPounds() {
        val csv = "Date;Workout Name;Exercise Name;Set Order;Weight;Weight Unit;Reps;RPE;Distance;Distance Unit;Seconds;Notes;Workout Notes;Workout Duration\n" +
            "2020-03-01 10:00:00;Legs;Squat (Barbell);1;225;lbs;5;;;;;;;45m\n"
        val parsed = parseImport(csv, WeightUnit.KG, zone = ZoneOffset.UTC)!!
        val set = parsed.workouts.single().exercises.single().sets.single()
        assertEquals(102.06, set.weightKg!!, 0.01)
        assertEquals(2700L, parsed.workouts.single().let { it.endedAt!!.epochSecond - it.startedAt.epochSecond })
    }

    @Test
    fun hevy() {
        val csv = """
            "title","start_time","end_time","description","exercise_title","superset_id","exercise_notes","set_index","set_type","weight_kg","reps","distance_km","duration_seconds","rpe"
            "Upper","15 Jan 2024, 07:30","15 Jan 2024, 08:25","","Bench Press (Barbell)","0","","0","warmup","40","10","","",""
            "Upper","15 Jan 2024, 07:30","15 Jan 2024, 08:25","","Bench Press (Barbell)","0","","1","normal","80","5","","",""
            "Upper","15 Jan 2024, 07:30","15 Jan 2024, 08:25","","Bent Over Row (Barbell)","0","","0","normal","60","8","","",""
            "Upper","15 Jan 2024, 07:30","15 Jan 2024, 08:25","","Plank","","","0","normal","","","","45",""
        """.trimIndent()
        val parsed = parseImport(csv, WeightUnit.KG, zone = ZoneOffset.UTC)!!
        assertEquals(ImportSource.HEVY, parsed.source)
        val w = parsed.workouts.single()
        assertEquals(3300L, w.endedAt!!.epochSecond - w.startedAt.epochSecond)
        assertEquals(3, w.exercises.size)
        assertTrue(w.exercises[0].supersetWithNext)
        assertTrue(!w.exercises[1].supersetWithNext)
        assertEquals(SetType.WARMUP, w.exercises[0].sets[0].type)
        assertEquals(45, w.exercises[2].sets[0].seconds)
    }

    @Test
    fun names() {
        assertEquals(nameKey("barbell bench press"), nameKey("Bench Press (Barbell)"))
        assertEquals(nameKey("pull-up"), nameKey("Pull Up"))
        assertEquals(nameKey("dumbbell bicep curls"), nameKey("Bicep Curl (Dumbbell)"))
    }
}
