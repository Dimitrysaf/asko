package io.github.dimitrysaf.asko.lib.reps

/** The joints the camera counter follows; their angles bend and straighten with each rep. */
enum class Joint { ELBOW, KNEE, HIP }

/**
 * Counts reps from joint angles, frame by frame (degrees, any of them missing when not seen).
 *
 * It doesn't need to know the exercise: each joint's angle is smoothed and its recent range kept;
 * the joint that swings the most (at least [minSwing] degrees) is the one followed, and once a rep
 * is counted it stays followed for the set. A rep is one full cycle: the angle goes past the low
 * end of its range, then back past the high end (a squat's knee bends, then straightens).
 */
class RepDetector(
    private val minSwing: Double = 35.0,
    private val windowMs: Long = 5_000,
    private val smoothing: Double = 0.4,
) {
    private class Track {
        var smoothed: Double? = null
        val history = ArrayDeque<Pair<Long, Double>>()
        fun range(): Pair<Double, Double>? =
            if (history.isEmpty()) null else history.minOf { it.second } to history.maxOf { it.second }
    }

    private val tracks = Joint.entries.associateWith { Track() }
    private var followed: Joint? = null

    /** The joint being counted, once one swings enough; null before. */
    val following: Joint? get() = followed
    private var wentLow = false

    var reps = 0
        private set

    /** Feeds one frame; true when it completed a rep. */
    fun onFrame(angles: Map<Joint, Double?>, timeMs: Long): Boolean {
        for ((joint, track) in tracks) {
            val angle = angles[joint] ?: continue
            val smoothed = track.smoothed?.let { it + smoothing * (angle - it) } ?: angle
            track.smoothed = smoothed
            track.history.addLast(timeMs to smoothed)
            while (track.history.isNotEmpty() && timeMs - track.history.first().first > windowMs) track.history.removeFirst()
        }
        // Until a rep is counted, follow whichever joint swings the most.
        if (reps == 0) {
            val widest = tracks.maxByOrNull { (_, t) -> t.range()?.let { it.second - it.first } ?: 0.0 }
            val swing = widest?.value?.range()?.let { it.second - it.first } ?: 0.0
            if (widest != null && swing >= minSwing && widest.key != followed) {
                followed = widest.key
                wentLow = false
            }
        }
        val joint = followed ?: return false
        val track = tracks.getValue(joint)
        val now = track.smoothed ?: return false
        val (low, high) = track.range() ?: return false
        val swing = high - low
        if (swing < minSwing) return false
        // A third of the way in from each end: noise near the ends doesn't count twice.
        if (now <= low + swing / 3) wentLow = true
        if (wentLow && now >= high - swing / 3) {
            wentLow = false
            reps++
            return true
        }
        return false
    }
}
