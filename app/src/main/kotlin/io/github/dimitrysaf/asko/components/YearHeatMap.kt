package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

/**
 * A year as a heat map: a column per week, a square per day, darker the more sets were done that
 * day ([setsByDay]), empty on days with none; month names above, and a Less–More key below. It
 * scrolls sideways, opening on today in the current year.
 */
@Composable
fun YearHeatMap(year: Int, setsByDay: Map<LocalDate, Int>, firstDayOfWeek: DayOfWeek, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val first = LocalDate.of(year, 1, 1)
    val start = first.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val weeks = (ChronoUnit.WEEKS.between(start, LocalDate.of(year, 12, 31)) + 1).toInt()
    val most = setsByDay.values.maxOrNull() ?: 0
    val empty = colors.surfaceContainerHighest
    fun level(sets: Int): Int = if (sets <= 0 || most == 0) 0 else ceil(sets * LEVELS.toDouble() / most).toInt().coerceIn(1, LEVELS)
    fun colorOf(level: Int): Color = if (level == 0) empty else lerp(colors.primaryContainer, colors.primary, (level - 1f) / (LEVELS - 1))

    val scroll = rememberScrollState()
    val step = with(LocalDensity.current) { (Cell + Gap).roundToPx() }
    LaunchedEffect(year, scroll.maxValue) {
        // This year: today's week at the right edge. Other years: from January.
        if (year == today.year) {
            val week = ChronoUnit.WEEKS.between(start, today).toInt()
            scroll.scrollTo(((week + 1) * step - scroll.viewportSize).coerceIn(0, scroll.maxValue))
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val monthNames = remember(locale) { (1..12).map { java.time.Month.of(it).getDisplayName(TextStyle.SHORT, locale) } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Column(Modifier.horizontalScroll(scroll), verticalArrangement = Arrangement.spacedBy(Gap)) {
            // Month names over the week each month starts in.
            Box(Modifier.width((Cell + Gap) * weeks).height(16.dp)) {
                (1..12).forEach { month ->
                    val week = ChronoUnit.WEEKS.between(start, LocalDate.of(year, month, 1)).toInt()
                    Text(
                        monthNames[month - 1],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.offsetX(step * week),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                repeat(weeks) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                        repeat(7) { d ->
                            val day = start.plusDays(week * 7L + d)
                            val color = when {
                                day.year != year -> Color.Transparent
                                day.isAfter(today) -> empty.copy(alpha = 0.4f)
                                else -> colorOf(level(setsByDay[day] ?: 0))
                            }
                            Box(Modifier.size(Cell).background(color, MaterialTheme.shapes.extraSmall))
                        }
                    }
                }
            }
        }
        // The key: Less, the shades, More.
        Row(
            Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            (0..LEVELS).forEach { Box(Modifier.size(Cell).background(colorOf(it), MaterialTheme.shapes.extraSmall)) }
            Text(stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

/** Moves a label [px] to the right. */
private fun Modifier.offsetX(px: Int) = offset { IntOffset(px, 0) }

private val Cell = 14.dp
private val Gap = 3.dp

/** Shades for days with sets (the empty shade aside). */
private const val LEVELS = 4
