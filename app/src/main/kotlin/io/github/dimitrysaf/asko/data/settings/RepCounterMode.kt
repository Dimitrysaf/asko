package io.github.dimitrysaf.asko.data.settings

/** How the workout counts reps during a set (Settings › Rep counter). */
enum class RepCounterMode {
    /** You count. */
    OFF,

    /** Counted aloud at a steady pace, each exercise's own. */
    METRONOME,

    /** The front camera watches you and counts each full movement, on the phone. */
    CAMERA,
}
