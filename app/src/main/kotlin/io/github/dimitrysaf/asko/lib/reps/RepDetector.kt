package io.github.dimitrysaf.asko.lib.reps

import io.github.dimitrysaf.asko.data.model.Muscle

/**
 * The joints the camera counter follows; their angles bend and straighten with each rep. [HAND]
 * is a hand seen on its own (no body in view): its height, as a 0–180 "angle", rises and falls.
 */
enum class Joint { ELBOW, KNEE, HIP, HAND }

/**
 * Counts reps from joint angles, frame by frame (degrees, any of them missing when not seen).
 *
 * The joint followed is [joint], from the exercise's muscles (see [jointFor]); without one, the
 * joint that swings the most (at least [minSwing] degrees) is followed, and once a rep is counted
 * it stays followed for the set. Each joint's angle is smoothed and its recent range kept. A rep is
 * one full cycle: the angle goes past the low end of its range, then back past the high end (a
 * squat's knee bends, then straightens).
 */
class RepDetector(
    private val joint: Joint? = null,
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
    private var followed: Joint? = joint

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
        // No joint from the exercise: until a rep is counted, follow whichever swings the most.
        if (joint == null && reps == 0) {
            val widest = tracks.maxByOrNull { (_, t) -> t.range()?.let { it.second - it.first } ?: 0.0 }
            val swing = widest?.value?.range()?.let { it.second - it.first } ?: 0.0
            if (widest != null && swing >= minSwing && widest.key != followed) {
                followed = widest.key
                wentLow = false
            }
        }
        val followedJoint = followed ?: return false
        // Only a hand in view (shown close up): its rise and fall stands in for the joint.
        val joint = if (angles[followedJoint] == null && angles[Joint.HAND] != null) Joint.HAND else followedJoint
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

/**
 * The joint an exercise's reps bend, from its main muscles: the knees for the quadriceps (squats,
 * lunges), the hips for the glutes, hamstrings and abs (deadlifts, bridges, sit-ups), the elbows
 * for the arms, chest, shoulders and back (curls, push-ups, presses, rows). Null when they say
 * nothing usable (cardio, calves): the detector then finds the joint itself.
 */
fun jointFor(main: Set<Muscle>): Joint? = when {
    Muscle.QUADRICEPS in main -> Joint.KNEE
    Muscle.GLUTES in main || Muscle.HAMSTRINGS in main -> Joint.HIP
    Muscle.ABDOMINALS in main || Muscle.OBLIQUES in main -> Joint.HIP
    main.any { it in ARM_MOVERS } -> Joint.ELBOW
    else -> null
}

/** Muscles whose exercises bend the elbows: arms, chest, shoulders, back. */
private val ARM_MOVERS = setOf(
    Muscle.BICEPS, Muscle.BRACHIALIS, Muscle.TRICEPS, Muscle.PECTORALS, Muscle.DELTOIDS,
    Muscle.LATS, Muscle.TRAPEZIUS, Muscle.SERRATUS,
)
