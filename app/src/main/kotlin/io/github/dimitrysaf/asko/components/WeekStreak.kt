package io.github.dimitrysaf.asko.components

import io.github.dimitrysaf.asko.data.model.DayMark
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import io.github.dimitrysaf.asko.lib.time.currentWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The current week with day letters, drawn exactly like a row of the month calendar ([DayRow]):
 * the same capsules, today's number circled.
 * [workoutDays] null means still loading.
 */
@Composable
fun WeekStreak(
    workoutDays: Set<LocalDate>?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onDayLongClick: ((LocalDate) -> Unit)? = null,
    marks: ((LocalDate) -> DayMark?)? = null,
    counts: ((LocalDate) -> Boolean)? = null,
) {
    val week = remember { currentWeek() }
    val today = remember { LocalDate.now() }
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            week.forEach { day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        DayRow(
            days = week,
            workoutDays = workoutDays,
            today = today,
            onDayClick = onDayClick,
            onDayLongClick = onDayLongClick,
            marks = marks,
            counts = counts,
        )
    }
}
