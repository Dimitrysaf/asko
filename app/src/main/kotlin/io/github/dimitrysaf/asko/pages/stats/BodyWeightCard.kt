package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.AreaChart
import io.github.dimitrysaf.asko.components.ChartSeries
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.timelineLabels
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
 * Your body weight, if you log it, as the other Stats charts: the range, then the chart, each
 * point the last weight logged by then. Log (the card's corner) asks today's weight.
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
            FilledTonalIconButton(onClick = { logging = true }) { Icon(Icons.Filled.Add, stringResource(R.string.body_weight_log)) }
        },
    ) {
        RangePicker(range) { range = it }
        if (weights.isEmpty()) {
            EmptyState(painterResource(R.drawable.ic_fitness), stringResource(R.string.body_weight_empty), compact = true)
            return@ChartCard
        }
        val today = LocalDate.now()
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val (labels, values) = remember(weights, range, unit) {
            val line = timeline(range, today, weights.first().first, firstDayOfWeek)
            // Each bucket: the last weight logged by its end (carried on), none before the first.
            val ends = line.starts.drop(1).map { it.minusDays(1) } + today
            line to ends.map { end -> weights.lastOrNull { !it.first.isAfter(end) }?.second?.let(unit::fromKg) }
        }
        val color = MaterialTheme.colorScheme.primary
        AreaChart(
            series = listOf(ChartSeries("body weight", values, color) { resources.weightValue(formatAmount(it), unit) }),
            labels = timelineLabels(labels),
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
