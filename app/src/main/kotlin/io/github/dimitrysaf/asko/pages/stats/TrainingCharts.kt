package io.github.dimitrysaf.asko.pages.stats

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ChoiceGroup
import io.github.dimitrysaf.asko.components.MetricChart
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.RoutineMetric
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.routineSeries
import io.github.dimitrysaf.asko.data.stats.trainingSeries
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.formatAmount
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import io.github.dimitrysaf.asko.theme.Metric
import io.github.dimitrysaf.asko.theme.metricColor
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/** What the training chart shows, one at a time. */
enum class TrainingMetric { SETS, REPS, TIME }

/**
 * Sets, reps or time ([metric], picked above the chart) over a chosen range: the total on top,
 * then a bar per day, week or month. Everything done, or one exercise ([exerciseId]: its sets,
 * reps and the time its sets were held; Time only if it has any).
 */
@Composable
fun TrainingChart(
    history: List<Workout>,
    metric: TrainingMetric,
    onMetric: (TrainingMetric) -> Unit,
    exerciseId: String? = null,
) {
    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    val today = LocalDate.now()
    val series = remember(history, range, exerciseId) { history.trainingSeries(range, today, firstDayOfWeek(), exerciseId) }
    // Whether Time is worth offering at all: any time held, ever (so it doesn't come and go with the range).
    val hasTime = remember(history, exerciseId) {
        exerciseId == null || history.trainingSeries(StatsRange.ALL, today, firstDayOfWeek(), exerciseId).minutes.any { it > 0 }
    }
    val shown = if (metric == TrainingMetric.TIME && !hasTime) TrainingMetric.SETS else metric
    val resources = LocalResources.current
    val values = when (shown) {
        TrainingMetric.SETS -> series.sets
        TrainingMetric.REPS -> series.reps
        TrainingMetric.TIME -> series.minutes
    }
    val format: (Double) -> String = when (shown) {
        TrainingMetric.SETS -> { v -> count(resources, R.plurals.stats_sets, v) }
        TrainingMetric.REPS -> { v -> count(resources, R.plurals.record_reps, v) }
        TrainingMetric.TIME -> { v -> formatSeconds((v * 60).roundToInt()) }
    }
    ChartCard(stringResource(R.string.stats_training)) {
        ChoiceGroup(
            options = if (hasTime) TrainingMetric.entries else listOf(TrainingMetric.SETS, TrainingMetric.REPS),
            selected = shown,
            onSelect = onMetric,
            label = {
                stringResource(
                    when (it) {
                        TrainingMetric.SETS -> R.string.stats_series_sets
                        TrainingMetric.REPS -> R.string.stats_series_reps
                        TrainingMetric.TIME -> R.string.stats_series_time
                    },
                )
            },
        )
        RangePicker(range) { range = it }
        MetricChart(
            values = values,
            timeline = series.timeline,
            color = metricColor(shown.metric),
            format = format,
            headline = format(values.sum()),
            headlineLabel = periodLabel(range),
        )
    }
}

/** The colour a training metric is drawn in. */
val TrainingMetric.metric: Metric
    get() = when (this) {
        TrainingMetric.SETS -> Metric.SETS
        TrainingMetric.REPS -> Metric.REPS
        TrainingMetric.TIME -> Metric.TIME
    }

/**
 * Weight over a chosen range: the kilos lifted in all (reps × weight) with their total on top, or
 * for one exercise ([exerciseId]) its heaviest set, the heaviest of the range on top.
 */
@Composable
fun WeightChart(history: List<Workout>, exerciseId: String? = null) {
    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    val series = remember(history, range, exerciseId) { history.trainingSeries(range, LocalDate.now(), firstDayOfWeek(), exerciseId) }
    val resources = LocalResources.current
    val number = remember { NumberFormat.getIntegerInstance() }
    val unit = LocalWeightUnit.current
    ChartCard(stringResource(if (exerciseId == null) R.string.stats_weight_lifted else R.string.stats_heaviest)) {
        RangePicker(range) { range = it }
        // In the app's unit, so the chart's axis reads in it too.
        val weights = series.weight.map { w -> w?.takeIf { it > 0 }?.let(unit::fromKg) }
        val format: (Double) -> String = { value ->
            val shown = if (exerciseId == null) number.format(value.roundToInt()) else formatAmount((value * 2).roundToInt() / 2.0)
            resources.weightValue(shown, unit)
        }
        val known = weights.filterNotNull()
        MetricChart(
            values = weights,
            timeline = series.timeline,
            color = metricColor(Metric.WEIGHT),
            format = format,
            headline = known.takeIf { it.isNotEmpty() }?.let { format(if (exerciseId == null) it.sum() else it.max()) },
            headlineLabel = periodLabel(range),
        )
    }
}

internal fun firstDayOfWeek() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

/** "12 sets", "1 rep": a whole count with its word. */
internal fun count(resources: Resources, plural: Int, value: Double): String {
    val n = value.roundToInt()
    return resources.getQuantityString(plural, n, n)
}

/** "Last 7 days", "Last 3 months", "All time": the range a chart's headline covers. */
@Composable
fun periodLabel(range: StatsRange): String = stringResource(
    when (range) {
        StatsRange.WEEK -> R.string.stats_period_week
        StatsRange.MONTH -> R.string.stats_period_month
        StatsRange.QUARTER -> R.string.stats_period_quarter
        StatsRange.YEAR -> R.string.stats_period_year
        StatsRange.ALL -> R.string.stats_period_all
    },
)

/** 1W · 1M · 3M · 1Y · All, as a connected button group. */
@Composable
fun RangePicker(range: StatsRange, onRange: (StatsRange) -> Unit) {
    ChoiceGroup(
        options = StatsRange.entries,
        selected = range,
        onSelect = onRange,
        label = {
            stringResource(
                when (it) {
                    StatsRange.WEEK -> R.string.stats_range_week
                    StatsRange.MONTH -> R.string.stats_range_month
                    StatsRange.QUARTER -> R.string.stats_range_quarter
                    StatsRange.YEAR -> R.string.stats_range_year
                    StatsRange.ALL -> R.string.stats_range_all
                },
            )
        },
    )
}

/** A chart with its controls, in a card, under its [title] (none on a page of its own). */
@Composable
fun ChartCard(title: String?, action: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            if (action == null) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            } else {
                // The card's own control sits in its top-right corner, beside the title.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title.orEmpty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    action()
                }
            }
            content()
        }
    }
}

/**
 * How the workouts done from a routine ([routineName]) went, over a chosen range: Volume (weight
 * lifted), Reps or Duration, one at a time; the average per workout on top, then a bar per day,
 * week or month, each that bucket's average per workout. Stats' chart design, in a card.
 */
@Composable
fun RoutineChart(history: List<Workout>, routineName: String) {
    var range by rememberSaveable { mutableStateOf(StatsRange.QUARTER) }
    var metric by rememberSaveable { mutableStateOf(RoutineMetric.VOLUME) }
    val (timeline, values, average) = remember(history, routineName, metric, range) {
        history.routineSeries(routineName, metric, range, LocalDate.now(), firstDayOfWeek())
    }
    val unit = LocalWeightUnit.current
    val resources = LocalResources.current
    val number = remember { NumberFormat.getIntegerInstance() }
    ChartCard(stringResource(R.string.routine_chart_title)) {
        ChoiceGroup(
            options = RoutineMetric.entries,
            selected = metric,
            onSelect = { metric = it },
            label = {
                stringResource(
                    when (it) {
                        RoutineMetric.VOLUME -> R.string.routine_metric_volume
                        RoutineMetric.REPS -> R.string.routine_metric_reps
                        RoutineMetric.DURATION -> R.string.routine_metric_duration
                    },
                )
            },
        )
        RangePicker(range) { range = it }
        // Volume in the app's unit, so the chart's axis reads in it too.
        val toShown: (Double) -> Double = if (metric == RoutineMetric.VOLUME) unit::fromKg else { v -> v }
        val format: (Double) -> String = { value ->
            when (metric) {
                RoutineMetric.VOLUME -> resources.weightValue(number.format(value.roundToInt()), unit)
                RoutineMetric.REPS -> count(resources, R.plurals.record_reps, value)
                RoutineMetric.DURATION -> formatSeconds((value * 60).roundToInt())
            }
        }
        MetricChart(
            values = values.map { v -> v?.let(toShown) },
            timeline = timeline,
            color = metricColor(
                when (metric) {
                    RoutineMetric.VOLUME -> Metric.WEIGHT
                    RoutineMetric.REPS -> Metric.REPS
                    RoutineMetric.DURATION -> Metric.TIME
                },
            ),
            format = format,
            headline = average?.let { format(toShown(it)) },
            headlineLabel = listOf(stringResource(R.string.chart_per_workout), periodLabel(range)).joinToString(" · "),
        )
    }
}
