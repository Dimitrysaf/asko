package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ChartValue
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.trainingSeries
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import io.github.dimitrysaf.asko.theme.Metric
import io.github.dimitrysaf.asko.theme.metricColor
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/** What a Stats tile, and the page it opens, shows. */
enum class StatsTile { SETS, REPS, TIME, WEIGHT }

/**
 * The last 7 days at a glance, Google Health's tiles: Sets, Reps, Time and Weight lifted, two by
 * two. Each has its total, a capsule per day and the days' letters, today's circled. Tap one to
 * open its page ([onOpen]).
 */
@Composable
fun MetricTiles(history: List<Workout>, onOpen: (StatsTile) -> Unit) {
    val today = LocalDate.now()
    val series = remember(history) { history.trainingSeries(StatsRange.WEEK, today, WeekFields.of(Locale.getDefault()).firstDayOfWeek) }
    val resources = LocalResources.current
    val number = remember { NumberFormat.getIntegerInstance() }
    val unit = LocalWeightUnit.current
    val days = series.timeline.starts
    val weights = series.weight.map { it?.let(unit::fromKg) ?: 0.0 }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Tile(
                title = stringResource(R.string.stats_series_sets),
                value = series.sets.sum().roundToInt().let { resources.getQuantityString(R.plurals.stats_sets, it, it) },
                values = series.sets,
                days = days,
                metric = Metric.SETS,
                onClick = { onOpen(StatsTile.SETS) },
                modifier = Modifier.weight(1f),
            )
            Tile(
                title = stringResource(R.string.stats_series_reps),
                value = series.reps.sum().roundToInt().let { resources.getQuantityString(R.plurals.record_reps, it, it) },
                values = series.reps,
                days = days,
                metric = Metric.REPS,
                onClick = { onOpen(StatsTile.REPS) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Tile(
                title = stringResource(R.string.stats_series_time),
                value = formatSeconds((series.minutes.sum() * 60).roundToInt()),
                values = series.minutes,
                days = days,
                metric = Metric.TIME,
                onClick = { onOpen(StatsTile.TIME) },
                modifier = Modifier.weight(1f),
            )
            Tile(
                title = stringResource(R.string.stats_weight_lifted),
                value = resources.weightValue(number.format(weights.sum().roundToInt()), unit),
                values = weights,
                days = days,
                metric = Metric.WEIGHT,
                onClick = { onOpen(StatsTile.WEIGHT) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One tile: [title] in the metric's colour, the total ([value], or "No data" when all [values]
 * are 0), a capsule per day in [days] filled to its share of the busiest, and the days' letters.
 */
@Composable
private fun Tile(
    title: String,
    value: String,
    values: List<Double>,
    days: List<LocalDate>,
    metric: Metric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = metricColor(metric)
    val most = values.maxOrNull()?.takeIf { it > 0 }
    val locale = LocalConfiguration.current.locales[0]
    val today = LocalDate.now()
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color, maxLines = 1)
            ChartValue(
                text = if (most == null) stringResource(R.string.chart_no_data) else value,
                style = MaterialTheme.typography.headlineSmall,
                unitStyle = MaterialTheme.typography.labelLarge,
            )
            // Each day in an equal column, so its capsule sits right over its letter.
            Row(Modifier.fillMaxWidth().height(BarsHeight)) {
                values.forEach { v ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        // The day's track, then its share of the busiest day filled from the bottom.
                        Box(
                            Modifier.width(BarWidth).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            val share = if (most == null) 0f else (v / most).toFloat()
                            if (share > 0f) {
                                Box(Modifier.fillMaxWidth().fillMaxHeight(share.coerceAtLeast(MinShare)).background(color, CircleShape))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    val isToday = day == today
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(DaySize)
                                .background(if (isToday) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        }
                    }
                }
            }
        }
    }
}

private val BarsHeight = 40.dp
private val BarWidth = 10.dp
private val DaySize = 18.dp

/** A day with anything at all shows at least this much, so it's never mistaken for none. */
private const val MinShare = 0.25f
