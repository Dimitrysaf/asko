package io.github.dimitrysaf.asko.lib.reps

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

class RepDetectorTest {
    /** [reps] squats: knee 170° standing to 90° down, 2 s each, at 30 frames a second, with a little noise; elbows barely move. */
    private fun squats(reps: Int, noise: Double = 3.0): Int {
        val detector = RepDetector()
        val random = Random(1)
        val frames = reps * 60 + 15
        for (f in 0..frames) {
            val t = f * 1000L / 30
            val phase = 2 * PI * f / 60.0
            val knee = 130 + 40 * cos(phase) + random.nextDouble(-noise, noise)
            val elbow = 160 + random.nextDouble(-noise, noise)
            detector.onFrame(mapOf(Joint.KNEE to knee, Joint.ELBOW to elbow, Joint.HIP to null), t)
        }
        return detector.reps
    }

    @Test
    fun countsEachSquat() = assertEquals(10, squats(10))

    @Test
    fun noisyFramesDoNotDoubleCount() = assertEquals(8, squats(8, noise = 8.0))

    @Test
    fun standingStillCountsNothing() {
        val detector = RepDetector()
        val random = Random(2)
        for (f in 0..300) detector.onFrame(mapOf(Joint.KNEE to 170 + random.nextDouble(-5.0, 5.0), Joint.ELBOW to 150.0), f * 33L)
        assertEquals(0, detector.reps)
    }
}
