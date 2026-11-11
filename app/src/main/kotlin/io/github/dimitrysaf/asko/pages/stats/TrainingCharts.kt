package io.github.dimitrysaf.asko.pages.stats

import io.github.dimitrysaf.asko.components.EmptyState
import androidx.compose.ui.res.painterResource
import kotlin.math.roundToInt
import java.text.NumberFormat
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import io.github.dimitrysaf.asko.pages.routines.formatKg
import androidx.compose.ui.platform.LocalResources
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.AreaChart
import io.github.dimitrysaf.asko.components.ChartSeries
import io.github.dimitrysaf.asko.components.timelineLabels
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.trainingSeries
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Sets, reps and time over a chosen range, as one area chart: each in its own colour, drawn on top
 * of each other, each hidden or shown with its toggle. Everything done, or one exercise
 * ([exerciseId]: its sets, reps and the time its sets were held; Time only if it has any).
 */
@Composable
fun TrainingChart(history: List<Workout>, exerciseId: String? = null) {
    var range by rememberSaveable { mutableStateOf(StatsRange.QUARTER) }
    var showSets by rememberSaveable { mutableStateOf(true) }
    var showReps by rememberSaveable { mutableStateOf(true) }
    var showTime by rememberSaveable { mutableStateOf(true) }
    val series = remember(history, range, exerciseId) { history.trainingSeries(range, LocalDate.now(), firstDayOfWeek(), exerciseId) }
    val colors = MaterialTheme.colorScheme
    val hasTime = series.minutes.any { it > 0 }
    val resources = LocalResources.current
    ChartCard(stringResource(R.string.stats_training)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            SeriesToggle(stringResource(R.string.stats_series_sets), colors.primary, showSets) { showSets = it }
            SeriesToggle(stringResource(R.string.stats_series_reps), colors.tertiary, showReps) { showReps = it }
            if (hasTime) SeriesToggle(stringResource(R.string.stats_series_time), colors.secondary, showTime) { showTime = it }
        }
        RangePicker(range) { range = it }
        val anyShown = showSets || showReps || (showTime && hasTime)
        val anyDone = series.sets.any { it > 0 }
        when {
            !anyShown -> EmptyState(painterResource(R.drawable.ic_stats), stringResource(R.string.stats_none_shown), compact = true)
            !anyDone -> EmptyState(painterResource(R.drawable.ic_fitness), stringResource(R.string.stats_nothing_done), compact = true)
            else -> AreaChart(
                series = buildList {
                    if (showSets) add(ChartSeries("sets", series.sets, colors.primary) { count(resources, R.plurals.stats_sets, it) })
                    if (showReps) add(ChartSeries("reps", series.reps, colors.tertiary) { count(resources, R.plurals.record_reps, it) })
                    if (showTime && hasTime) add(ChartSeries("time", series.minutes, colors.secondary) { formatSeconds((it * 60).roundToInt()) })
                },
                labels = timelineLabels(series.timeline),
            )
        }
    }
}

/**
 * Weight over a chosen range, as an area chart: the kilos lifted in all (reps × weight), or for
 * one exercise ([exerciseId]) its heaviest set. Nothing shows when there's no weight to chart.
 */
@Composable
fun WeightChart(history: List<Workout>, exerciseId: String? = null) {
    var range by rememberSaveable { mutableStateOf(StatsRange.QUARTER) }
    val series = remember(history, range, exerciseId) { history.trainingSeries(range, LocalDate.now(), firstDayOfWeek(), exerciseId) }
    val color = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current
    val number = remember { NumberFormat.getIntegerInstance() }
    ChartCard(stringResource(if (exerciseId == null) R.string.stats_weight_lifted else R.string.stats_heaviest)) {
        RangePicker(range) { range = it }
        val weights = series.weight.map { w -> w?.takeIf { it > 0 } }
        if (weights.all { it == null }) {
            EmptyState(painterResource(R.drawable.ic_fitness), stringResource(R.string.stats_no_weight), compact = true)
        } else {
            AreaChart(
                series = listOf(
                    ChartSeries("weight", weights, color) { kg ->
                        val shown = if (exerciseId == null) number.format(kg.roundToInt()) else formatKg((kg * 2).roundToInt() / 2.0)
                        resources.getString(R.string.routine_kg_value, shown)
                    },
                ),
                labels = timelineLabels(series.timeline),
            )
        }
    }
}

private fun firstDayOfWeek() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

/** "12 sets", "1 rep": a whole count with its word. */
private fun count(resources: Resources, plural: Int, value: Double): String {
    val n = value.roundToInt()
    return resources.getQuantityString(plural, n, n)
}

/** Shows or hides one series: Material's filter chip, with a dot in the series' colour. */
@Composable
private fun SeriesToggle(label: String, color: Color, shown: Boolean, onShown: (Boolean) -> Unit) {
    FilterChip(
        selected = shown,
        onClick = { onShown(!shown) },
        label = { Text(label) },
        leadingIcon = { Box(Modifier.size(10.dp).background(color, CircleShape)) },
    )
}

/** 1M · 3M · 1Y · All (month, 3 months, year, everything), as Material's segmented buttons. */
@Composable
fun RangePicker(range: StatsRange, onRange: (StatsRange) -> Unit) {
    val ranges = StatsRange.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ranges.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == range,
                onClick = { onRange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, ranges.size),
                modifier = Modifier.weight(1f),
                // No checkmark: the selected one is filled, and the labels stay short and centred.
                icon = {},
            ) {
                Text(
                    stringResource(
                        when (option) {
                            StatsRange.MONTH -> R.string.stats_range_month
                            StatsRange.QUARTER -> R.string.stats_range_quarter
                            StatsRange.YEAR -> R.string.stats_range_year
                            StatsRange.ALL -> R.string.stats_range_all
                        },
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** A titled chart with its controls, in a card. */
@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
