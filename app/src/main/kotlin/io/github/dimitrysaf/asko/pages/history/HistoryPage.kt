package io.github.dimitrysaf.asko.pages.history

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dimitrysaf.asko.data.model.Workout
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.pages.workout.workoutTimes
import androidx.compose.foundation.layout.Arrangement
import io.github.dimitrysaf.asko.components.pageContentPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.MonthCalendar
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.lib.time.localDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Workout history under a month calendar: every workout, newest first, grouped by day under a
 * separator (the date between two still waves), a day's workouts as one segmented list: the
 * routine, when it started, how long it took and when it finished, and how many exercises. Tap
 * one to see it all. Tapping a day in the calendar shows just that day (Show all brings back the
 * rest); [initialDay] opens with that day selected (from the week strip).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryPage(initialDay: LocalDate? = null, onWorkoutClick: (workoutId: Long) -> Unit) {
    val viewModel = appViewModel { HistoryViewModel(it.workoutRepository, it.exerciseRepository) }
    val history by viewModel.history.collectAsStateWithLifecycle()

    PageScaffold(title = stringResource(R.string.nav_history)) { padding ->
        val list = history
        val workoutDays = remember(list) { list?.map { it.startedAt.localDate() }?.toSet() }
        // Tapping a day shows just that day's workouts; tapping it again (or Show all) clears it.
        var selectedDay by rememberSaveable { mutableStateOf(initialDay) }
        val shown = remember(list, selectedDay) {
            selectedDay?.let { day -> list?.filter { it.startedAt.localDate() == day } } ?: list
        }
        val locale = LocalConfiguration.current.locales[0]
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            item(key = "calendar") {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    MonthCalendar(
                        workoutDays = workoutDays,
                        selected = selectedDay,
                        onDayClick = { day -> selectedDay = if (day == selectedDay) null else day },
                        modifier = Modifier.padding(Dimens.SpaceM),
                    )
                }
            }
            selectedDay?.let { day ->
                item(key = "selected-day") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { selectedDay = null }) { Text(stringResource(R.string.history_show_all)) }
                    }
                }
            }
            when {
                shown == null -> items(4) { SkeletonBox(Modifier.fillMaxWidth().height(76.dp), MaterialTheme.shapes.large) }
                shown.isEmpty() -> item {
                    EmptyState(
                        rememberVectorPainter(Icons.Filled.DateRange),
                        stringResource(if (selectedDay != null) R.string.history_no_workouts_on_day else R.string.history_empty),
                    )
                }
                else -> shown.groupBy { it.startedAt.localDate() }.forEach { (day, workouts) ->
                    // With one day picked, its title row above already says which.
                    if (selectedDay == null) {
                        item(key = "day-$day") { DateSeparator(day, Modifier.padding(top = Dimens.SpaceS)) }
                    }
                    // The day's workouts as one segmented list, in one block so they sit at the
                    // segmented gap rather than the page's spacing.
                    item(key = "workouts-$day") {
                        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                            workouts.forEachIndexed { index, workout ->
                                HistoryEntry(workout, index, workouts.size, onClick = { onWorkoutClick(workout.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One workout: the routine it was done from (or "Workout"), then "Started 18:30 · Took 52:10 ·
 * Finished 19:22" and how many exercises. Tap it for everything that was done.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HistoryEntry(workout: Workout, index: Int, count: Int, onClick: () -> Unit) {
    val times = workoutTimes(workout)
    val exercises = pluralStringResource(R.plurals.history_exercise_count, workout.exercises.size, workout.exercises.size)
    SegmentedListItem(
        onClick = onClick,
        shapes = segmentedShapes(index, count),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        supportingContent = { Text("$times\n$exercises") },
        content = { Text(workout.routineName ?: stringResource(R.string.history_workout)) },
    )
}

/** A day's title between the workouts: the date in the middle, a still wave on each side. */
@Composable
private fun DateSeparator(day: LocalDate, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]
    val label = when (day) {
        today -> stringResource(R.string.log_today)
        today.minusDays(1) -> stringResource(R.string.log_yesterday)
        else -> day.format(DateTimeFormatter.ofPattern(if (day.year == today.year) "EEEE, d MMMM" else "EEEE, d MMMM yyyy", locale))
    }
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        StillWave(Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StillWave(Modifier.weight(1f))
    }
}

/** Material's wavy progress indicator, full and not moving: a decorative wavy line. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StillWave(modifier: Modifier = Modifier) {
    LinearWavyProgressIndicator(
        progress = { 1f },
        modifier = modifier.clearAndSetSemantics {},
        color = MaterialTheme.colorScheme.outlineVariant,
        trackColor = Color.Transparent,
        stopSize = 0.dp,
        amplitude = { 1f },
        waveSpeed = 0.dp,
    )
}
