package io.github.dimitrysaf.asko.lib.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import java.time.Duration

/**
 * "~45 min", "~1 h 10 min", "~2 h": roughly how long, rounded up to the next 5 minutes (at least 5).
 */
@Composable
fun aboutDuration(duration: Duration): String {
    val minutes = ((duration.seconds + ROUND_SECONDS - 1) / ROUND_SECONDS * 5).toInt().coerceAtLeast(5)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.about_minutes, minutes)
        rest == 0 -> stringResource(R.string.about_hours, hours)
        else -> stringResource(R.string.about_hours_minutes, hours, rest)
    }
}

private const val ROUND_SECONDS = 5 * 60L
