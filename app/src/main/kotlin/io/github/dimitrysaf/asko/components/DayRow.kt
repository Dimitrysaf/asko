package io.github.dimitrysaf.asko.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import io.github.dimitrysaf.asko.data.model.DayMark
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val CellHeight = 40.dp
private val DaySize = 34.dp
private val RingSize = 38.dp
private val IconSize = 30.dp
private val DayCorner = 10.dp
private val DayShape = RoundedCornerShape(DayCorner)
private val RingShape = RoundedCornerShape(DayCorner + 2.dp)

/** How strongly a day's icon shows behind its number (so the number stays readable). */
private const val IconAlpha = 0.45f

/** On a filled day the icon stands in front of the fill. */
private const val FilledIconAlpha = 0.9f

/** The fill: the theme's blue, this much darker. */
private const val FillDarkening = 0.2f

/**
 * Seven consecutive days in a row, the building block of the week strip and the month calendar.
 * Each day is a rounded square.
 *
 * Days that count towards the streak ([counts]: workouts and rest days; without it, workout
 * days) are filled in one colour, a little darker than the theme's blue, and back-to-back ones
 * join into one bar. Today is outlined, the [selected] day ringed, and days [muted] (outside the
 * shown month) faded. [workoutDays] null means still loading. With [marks], each day's streak icon
 * (fire, bed, slime, plane) sits large behind its number: in front of the fill on filled days,
 * faded on the others.
 */
@Composable
fun DayRow(
    days: List<LocalDate>,
    workoutDays: Set<LocalDate>?,
    today: LocalDate,
    modifier: Modifier = Modifier,
    selected: LocalDate? = null,
    muted: (LocalDate) -> Boolean = { false },
    onDayClick: ((LocalDate) -> Unit)? = null,
    marks: ((LocalDate) -> DayMark?)? = null,
    counts: ((LocalDate) -> Boolean)? = null,
) {
    val fill = lerp(MaterialTheme.colorScheme.primary, Color.Black, FillDarkening)
    val worked = days.map { !muted(it) && workoutDays != null && (counts?.invoke(it) ?: (it in workoutDays)) }
    Row(modifier.fillMaxWidth()) {
        days.forEachIndexed { i, day ->
            val isWorked = worked[i]
            val joinsLeft = isWorked && i > 0 && worked[i - 1]
            val joinsRight = isWorked && i < days.lastIndex && worked[i + 1]
            val isMuted = muted(day)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(CellHeight)
                    .then(
                        if (onDayClick != null && !isMuted) {
                            Modifier.clickable(role = Role.Button) { onDayClick(day) }
                        } else {
                            Modifier
                        },
                    )
                    // Workout days: a rounded square, joined to a worked neighbour edge to edge.
                    .drawBehind {
                        if (isWorked) {
                            val side = DaySize.toPx()
                            val center = Offset(size.width / 2, size.height / 2)
                            val top = center.y - side / 2
                            drawRoundRect(fill, Offset(center.x - side / 2, top), Size(side, side), CornerRadius(DayCorner.toPx()))
                            if (joinsLeft) drawRect(fill, Offset(0f, top), Size(center.x, side))
                            if (joinsRight) drawRect(fill, Offset(center.x, top), Size(size.width - center.x, side))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    workoutDays == null && !isMuted -> SkeletonBox(Modifier.size(DaySize), DayShape)
                    !isWorked && day == today ->
                        Box(Modifier.size(DaySize).border(2.dp, MaterialTheme.colorScheme.primary, DayShape))
                }
                if (day == selected) {
                    Box(Modifier.size(RingSize).border(2.dp, MaterialTheme.colorScheme.onSurface, RingShape))
                }
                // The day's streak icon, large behind its number.
                val mark = if (isMuted) null else marks?.invoke(day)
                if (mark != null) DayMarkIcon(mark, size = IconSize, alpha = if (isWorked) FilledIconAlpha else IconAlpha)
                if (workoutDays != null || isMuted) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = if (day == today) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
                        color = when {
                            isWorked -> MaterialTheme.colorScheme.onPrimary
                            isMuted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            day == today -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
