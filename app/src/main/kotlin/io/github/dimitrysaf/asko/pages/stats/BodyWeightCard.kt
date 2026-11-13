package io.github.dimitrysaf.asko.pages.stats

import io.github.dimitrysaf.asko.components.WithTooltip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.MetricChart
import io.github.dimitrysaf.asko.theme.Metric
import io.github.dimitrysaf.asko.theme.metricColor
import java.time.format.DateTimeFormatter
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.timeline
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.formatAmount
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.pages.routines.CustomWeightDialog
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Your body weight, if you log it, as the other Stats charts: the range, the latest weight on top,
 * then a line with a dot per bucket, the last weight logged by then. Log (the card's corner) asks
 * today's weight.
 */
@Composable
fun BodyWeightCard(weights: List<Pair<LocalDate, Double>>, onLog: (kg: Double) -> Unit) {
    var logging by rememberSaveable { mutableStateOf(false) }
    var range by rememberSaveable { mutableStateOf(StatsRange.QUARTER) }
    val unit = LocalWeightUnit.current
    val resources = LocalResources.current
    ChartCard(
        title = stringResource(R.string.stats_body_weight),
        action = {
            WithTooltip(stringResource(R.string.body_weight_log)) {
                FilledTonalIconButton(onClick = { logging = true }) { Icon(Icons.Filled.Add, stringResource(R.string.body_weight_log)) }
            }
        },
    ) {
        RangePicker(range) { range = it }
        val today = LocalDate.now()
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val (line, values) = remember(weights, range, unit) {
            val line = timeline(range, today, weights.firstOrNull()?.first, firstDayOfWeek)
            // Each bucket: the last weight logged by its end (carried on), none before the first.
            val ends = line.starts.drop(1).map { it.minusDays(1) } + today
            line to ends.map { end -> weights.lastOrNull { !it.first.isAfter(end) }?.second?.let(unit::fromKg) }
        }
        val format: (Double) -> String = { resources.weightValue(formatAmount(it), unit) }
        val latest = weights.lastOrNull()
        MetricChart(
            values = values,
            timeline = line,
            color = metricColor(Metric.BODY_WEIGHT),
            format = format,
            headline = latest?.let { format(unit.fromKg(it.second)) },
            headlineLabel = latest?.let { listOf(stringResource(R.string.chart_latest), it.first.format(DateTimeFormatter.ofPattern("d MMM"))).joinToString(" · ") }
                ?: periodLabel(range),
            line = true,
        )
    }
    if (logging) {
        CustomWeightDialog(
            initialKg = weights.lastOrNull()?.second,
            title = stringResource(R.string.body_weight_today),
            onPick = {
                logging = false
                onLog(it)
            },
            onDismiss = { logging = false },
        )
    }
}
