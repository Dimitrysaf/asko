package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val CellHeight = 44.dp
private val DaySize = 36.dp
private val RingSize = 42.dp

/**
 * Seven consecutive days in a row, the building block of the week strip and the month calendar.
 *
 * Workout days are filled: back-to-back ones join into one pill (a streak), a lone one gets
 * Material's expressive cookie shape. Today is outlined, the [selected] day ringed, and days
 * [muted] (outside the shown month) faded. [workoutDays] null means still loading.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayRow(
    days: List<LocalDate>,
    workoutDays: Set<LocalDate>?,
    today: LocalDate,
    modifier: Modifier = Modifier,
    selected: LocalDate? = null,
    muted: (LocalDate) -> Boolean = { false },
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val fill = MaterialTheme.colorScheme.primary
    val worked = days.map { !muted(it) && workoutDays?.contains(it) == true }
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
                    // Streak pill: a bar to each joined neighbour plus a round cap, edge to edge across cells.
                    .drawBehind {
                        if (joinsLeft || joinsRight) {
                            val radius = DaySize.toPx() / 2
                            val center = Offset(size.width / 2, size.height / 2)
                            val left = if (joinsLeft) 0f else center.x
                            val right = if (joinsRight) size.width else center.x
                            drawRect(fill, Offset(left, center.y - radius), Size(right - left, radius * 2))
                            drawCircle(fill, radius, center)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    workoutDays == null && !isMuted -> SkeletonBox(Modifier.size(DaySize), CircleShape)
                    isWorked && !joinsLeft && !joinsRight ->
                        Box(Modifier.size(DaySize).background(fill, MaterialShapes.Cookie9Sided.toShape()))
                    !isWorked && day == today ->
                        Box(Modifier.size(DaySize).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape))
                }
                if (day == selected) {
                    Box(Modifier.size(RingSize).border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
                }
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
