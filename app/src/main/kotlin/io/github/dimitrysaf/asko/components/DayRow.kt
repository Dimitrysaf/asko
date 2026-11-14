package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.data.model.DayMark
import io.github.dimitrysaf.asko.theme.Metric
import io.github.dimitrysaf.asko.theme.metricColor
import java.time.LocalDate

/** How tall a day's capsule is, in the month calendar and the week strip alike. */
val DayCapsuleHeight = 48.dp
private val CapsuleWidth = 36.dp
private val SelectedGap = 3.dp
private val TodaySize = 24.dp
private val IconSize = 18.dp

/** How faint the icon of a day that doesn't count shows on its empty track. */
private const val TrackIconAlpha = 0.6f

/**
 * Seven consecutive days in a row, the building block of the week strip and the month calendar,
 * drawn the way Google Health draws exercise days: each day a capsule, its number on top.
 *
 * Days that count towards the streak ([counts]; without it, workout days) and sick or travel days are filled in their
 * streak mark's colour (fire orange, slime green, …; teal without [marks]), with the mark's icon
 * under the number, and back-to-back ones join into one bar that fades from one day's colour to
 * the next. Other days are an empty track, a mark (sick, travelling) faint on
 * it. With [circleToday], today's number sits in a filled circle; the [selected] day is ringed;
 * days [muted] (outside the shown month) are only a faded number. [workoutDays] null means still
 * loading. [height]: [DayCapsuleHeight] by default.
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
    onDayLongClick: ((LocalDate) -> Unit)? = null,
    marks: ((LocalDate) -> DayMark?)? = null,
    counts: ((LocalDate) -> Boolean)? = null,
    height: Dp = DayCapsuleHeight,
    circleToday: Boolean = true,
) {
    val activity = metricColor(Metric.SETS)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    // Sick and travel days keep a streak going too, so they're filled and join it.
    val worked = days.map {
        val excused = marks?.invoke(it).let { mark -> mark == DayMark.SICK || mark == DayMark.TRAVEL }
        !muted(it) && workoutDays != null && (excused || (counts?.invoke(it) ?: (it in workoutDays)))
    }
    val fills = days.mapIndexed { i, day ->
        val mark = if (muted(day)) null else marks?.invoke(day)
        when {
            !worked[i] -> track
            mark != null -> dayMarkColor(mark)
            else -> activity
        }
    }
    Row(modifier.fillMaxWidth()) {
        days.forEachIndexed { i, day ->
            val isMuted = muted(day)
            val isWorked = worked[i]
            val mark = if (isMuted) null else marks?.invoke(day)
            val fill = fills[i]
            // Back-to-back streak days join into one bar, each day in its own mark's colour,
            // fading into its neighbour's halfway between them.
            val left = if (isWorked && i > 0 && worked[i - 1]) lerp(fills[i - 1], fill, 0.5f) else null
            val right = if (isWorked && i < days.lastIndex && worked[i + 1]) lerp(fill, fills[i + 1], 0.5f) else null
            Box(
                Modifier
                    .weight(1f)
                    .height(height + SelectedGap * 2)
                    .drawBehind { if (isWorked) drawStreakBar(fill, left, right, CapsuleWidth.toPx(), height.toPx()) },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isMuted -> Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    )
                    workoutDays == null -> SkeletonBox(Modifier.width(CapsuleWidth).height(height), CircleShape)
                    else -> {
                        // Black or white on a filled day, whichever reads better on its colour.
                        val content = when {
                            !isWorked -> MaterialTheme.colorScheme.onSurface
                            fill.luminance() > 0.45f -> Color.Black
                            else -> Color.White
                        }
                        Box(
                            Modifier
                                .then(if (day == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .padding(SelectedGap)
                                .width(CapsuleWidth)
                                .height(height)
                                .clip(CircleShape)
                                // A streak day's colour is the bar drawn behind; the others are an empty track.
                                .then(if (isWorked) Modifier else Modifier.background(track))
                                .then(
                                    if (onDayClick != null || onDayLongClick != null) {
                                        Modifier.combinedClickable(
                                            role = Role.Button,
                                            onLongClick = onDayLongClick?.let { { it(day) } },
                                            onClick = { onDayClick?.invoke(day) },
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            ) {
                                val circled = circleToday && day == today
                                Box(
                                    Modifier.size(TodaySize).background(if (circled) content else Color.Transparent, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = day.dayOfMonth.toString(),
                                        style = if (day == today) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
                                        color = if (circled) fill else content,
                                    )
                                }
                                if (mark != null) {
                                    DayMarkIcon(
                                        mark,
                                        size = IconSize,
                                        tint = if (isWorked) content else dayMarkColor(mark).copy(alpha = TrackIconAlpha),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A streak day's part of the bar, centred in its cell: a capsule [width] wide and [height] tall in
 * [fill], reaching out to the cell's edge on a side that joins a neighbour ([left] or [right],
 * the colour halfway to it, which the fill fades into) and rounded on a side that doesn't.
 */
private fun DrawScope.drawStreakBar(fill: Color, left: Color?, right: Color?, width: Float, height: Float) {
    val centre = size.width / 2
    val top = (size.height - height) / 2
    val start = if (left != null) 0f else centre - width / 2
    val end = if (right != null) size.width else centre + width / 2
    val radius = CornerRadius(width / 2)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(start, top, end, top + height),
                topLeft = if (left != null) CornerRadius.Zero else radius,
                bottomLeft = if (left != null) CornerRadius.Zero else radius,
                topRight = if (right != null) CornerRadius.Zero else radius,
                bottomRight = if (right != null) CornerRadius.Zero else radius,
            ),
        )
    }
    val middle = ((centre - start) / (end - start)).coerceIn(0f, 1f)
    val brush = Brush.horizontalGradient(
        0f to (left ?: fill),
        middle to fill,
        1f to (right ?: fill),
        startX = start,
        endX = end,
    )
    drawPath(path, brush)
}
