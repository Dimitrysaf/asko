package io.github.dimitrysaf.asko.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** What a chart or tile measures. Each has its own colour, whatever the theme. */
enum class Metric { SETS, REPS, TIME, WEIGHT, BODY_WEIGHT, PAIN }

/**
 * A metric's colour, fixed like Google Health's (teal activity, purple sleep): the same on every
 * wallpaper, a lighter tone on dark backgrounds.
 */
@Composable
fun metricColor(metric: Metric): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val (light, onDark) = when (metric) {
        Metric.SETS -> 0xFF00897B to 0xFF4DD0C1
        Metric.REPS -> 0xFF1A73E8 to 0xFF8AB4F8
        Metric.TIME -> 0xFFE37400 to 0xFFFCAD70
        Metric.WEIGHT -> 0xFF5B5FC7 to 0xFFAFB3FF
        Metric.BODY_WEIGHT -> 0xFF9334E6 to 0xFFD7AEFB
        Metric.PAIN -> 0xFFD93025 to 0xFFF28B82
    }
    return Color(if (dark) onDark else light)
}
