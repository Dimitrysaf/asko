package io.github.dimitrysaf.asko.components

import io.github.dimitrysaf.asko.data.model.DayMark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/** How far back the calendar can be swiped. */
private const val MONTHS_BACK = 120

/**
 * Swipeable month calendar ([DayRow]s of capsules): swipe or use the tonal ‹ › to change month
 * (never past this one); Today jumps back. Opens on [selected]'s month, or this month. [workoutDays] null
 * means still loading.
 */
@Composable
fun MonthCalendar(
    workoutDays: Set<LocalDate>?,
    selected: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onDayLongClick: ((LocalDate) -> Unit)? = null,
    marks: ((LocalDate) -> DayMark?)? = null,
    counts: ((LocalDate) -> Boolean)? = null,
) {
    val today = remember { LocalDate.now() }
    val thisMonth = remember { YearMonth.from(today) }
    val lastPage = MONTHS_BACK
    fun monthOf(page: Int): YearMonth = thisMonth.minusMonths((lastPage - page).toLong())
    val pager = rememberPagerState(
        initialPage = selected
            ?.let { lastPage - ChronoUnit.MONTHS.between(YearMonth.from(it), thisMonth).toInt() }
            ?.coerceIn(0, lastPage)
            ?: lastPage,
    ) { lastPage + 1 }
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // "LLLL" is the stand-alone month name, correct on its own in every language.
                text = monthOf(pager.currentPage).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // Google Health's period controls: back to now, then tonal round ‹ ›.
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs), verticalAlignment = Alignment.CenterVertically) {
                if (pager.currentPage != lastPage) {
                    FilledTonalButton(onClick = { scope.launch { pager.animateScrollToPage(lastPage) } }) {
                        Text(stringResource(R.string.calendar_today))
                    }
                }
                WithTooltip(stringResource(R.string.calendar_previous_month)) {
                    FilledTonalIconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        enabled = pager.currentPage > 0,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month))
                    }
                }
                WithTooltip(stringResource(R.string.calendar_next_month)) {
                    FilledTonalIconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        enabled = pager.currentPage < lastPage,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            (0L..6L).forEach { offset ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = firstDayOfWeek.plus(offset).getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalPager(state = pager, verticalAlignment = Alignment.Top) { page ->
            val month = monthOf(page)
            val weeks = remember(month, firstDayOfWeek) { month.weeks(firstDayOfWeek) }
            Column {
                weeks.forEach { week ->
                    DayRow(
                        days = week,
                        workoutDays = workoutDays,
                        today = today,
                        selected = selected,
                        muted = { YearMonth.from(it) != month },
                        onDayClick = onDayClick,
                        onDayLongClick = onDayLongClick,
                        marks = marks,
                        counts = counts,
                    )
                }
            }
        }
    }
}

/** Full weeks covering the month, starting on [firstDayOfWeek]; edge days belong to the neighbouring months. */
private fun YearMonth.weeks(firstDayOfWeek: DayOfWeek): List<List<LocalDate>> {
    val start = atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val end = atEndOfMonth()
    return generateSequence(start) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(end) }
        .map { weekStart -> List(7) { weekStart.plusDays(it.toLong()) } }
        .toList()
}
