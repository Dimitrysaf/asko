package io.github.dimitrysaf.asko.data.model

/**
 * How far the exercise library's first download is: [done] of [total] steps (each exercise is
 * listed, then its animation is checked), [elapsedMs] since it started.
 */
data class DownloadProgress(val done: Int, val total: Int, val elapsedMs: Long) {
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)

    /** Exercises listed so far, of the library's size (for "350 of 1,500"). */
    val exercises: Int get() = (done / 2).coerceAtMost(total / 2)
    val exerciseTotal: Int get() = total / 2

    /** Time left at the pace so far; null until there's enough to go by. */
    val remainingMs: Long?
        get() = if (fraction < 0.05f) null else (elapsedMs * (1 - fraction) / fraction).toLong()
}
