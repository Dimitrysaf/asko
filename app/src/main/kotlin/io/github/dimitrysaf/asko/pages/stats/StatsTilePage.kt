package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.SegmentedListItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.data.stats.BucketSize
import io.github.dimitrysaf.asko.lib.time.localDate
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.MetricChart
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.bucketTitle
import io.github.dimitrysaf.asko.components.pageContentPadding
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.trainingSeries
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import io.github.dimitrysaf.asko.theme.Metric
import io.github.dimitrysaf.asko.theme.metricColor
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * One Stats tile's own page, Google Health's detail page: the range, the total on top and a bar
 * per day, week or month, then those days, weeks or months with anything in them, newest first,
 * each with its value. Sets, reps or time done, or the weight lifted ([tile]), across all workouts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsTilePage(tile: StatsTile, onBack: () -> Unit, onExerciseClick: (exerciseId: String) -> Unit) {
    val viewModel = appViewModel { StatsViewModel(it.workoutRepository, it.exerciseRepository, it.bodyWeightRepository, it.noteRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    val resources = LocalResources.current
    val number = remember { NumberFormat.getIntegerInstance() }
    val unit = LocalWeightUnit.current
    val locale = LocalConfiguration.current.locales[0]
    val title = stringResource(
        when (tile) {
            StatsTile.SETS -> R.string.stats_series_sets
            StatsTile.REPS -> R.string.stats_series_reps
            StatsTile.TIME -> R.string.stats_series_time
            StatsTile.WEIGHT -> R.string.stats_weight_lifted
        },
    )
    val color = metricColor(
        when (tile) {
            StatsTile.SETS -> Metric.SETS
            StatsTile.REPS -> Metric.REPS
            StatsTile.TIME -> Metric.TIME
            StatsTile.WEIGHT -> Metric.WEIGHT
        },
    )
    val format: (Double) -> String = when (tile) {
        StatsTile.SETS -> { v -> count(resources, R.plurals.stats_sets, v) }
        StatsTile.REPS -> { v -> count(resources, R.plurals.record_reps, v) }
        StatsTile.TIME -> { v -> formatSeconds((v * 60).roundToInt()) }
        StatsTile.WEIGHT -> { v -> resources.weightValue(number.format(v.roundToInt()), unit) }
    }

    PageScaffold(title = title, onBack = onBack) { padding ->
        val stats = state
        if (stats == null) {
            Column(Modifier.pageSkeletonPadding(padding)) {
                SkeletonBox(Modifier.fillMaxWidth().height(360.dp), MaterialTheme.shapes.extraLarge)
            }
            return@PageScaffold
        }
        val series = remember(stats.history, range) { stats.history.trainingSeries(range, LocalDate.now(), firstDayOfWeek()) }
        val values: List<Double?> = when (tile) {
            StatsTile.SETS -> series.sets
            StatsTile.REPS -> series.reps
            StatsTile.TIME -> series.minutes
            // In the app's unit, so the chart's axis reads in it too.
            StatsTile.WEIGHT -> series.weight.map { w -> w?.takeIf { it > 0 }?.let(unit::fromKg) }
        }
        // Each exercise's part in it over the range, most first, and how many workouts it was in.
        val from = series.timeline.starts.first()
        val exercises = remember(stats.history, range, tile, unit) {
            val totals = mutableMapOf<String, Pair<Double, Int>>()
            stats.history.filter { !it.startedAt.localDate().isBefore(from) }.forEach { workout ->
                workout.exercises.forEach { done ->
                    val value = when (tile) {
                        StatsTile.SETS -> done.sets.size.toDouble()
                        StatsTile.REPS -> done.sets.sumOf { it.reps ?: 0 }.toDouble()
                        StatsTile.TIME -> done.sets.sumOf { it.durationSeconds ?: 0 } / 60.0
                        StatsTile.WEIGHT -> unit.fromKg(done.sets.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) })
                    }
                    if (value > 0) {
                        val (sum, times) = totals[done.exerciseId] ?: (0.0 to 0)
                        totals[done.exerciseId] = sum + value to times + 1
                    }
                }
            }
            totals.entries.sortedByDescending { it.value.first }.map { Triple(it.key, it.value.first, it.value.second) }
        }
        // The buckets with anything in them, newest first.
        val done = values.indices.filter { (values[it] ?: 0.0) > 0 }.reversed()
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            item(key = "chart") {
                ChartCard(title = null) {
                    RangePicker(range) { range = it }
                    MetricChart(
                        values = values,
                        timeline = series.timeline,
                        color = color,
                        format = format,
                        headline = format(values.sumOf { it ?: 0.0 }),
                        headlineLabel = periodLabel(range),
                    )
                }
            }
            if (exercises.isNotEmpty()) {
                item(key = "exercises-title") { SectionTitle(stringResource(R.string.stats_tile_exercises)) }
                item(key = "exercises") {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        exercises.forEachIndexed { index, (id, value, times) ->
                            val exercise = viewModel.library[id]
                            SegmentedListItem(
                                verticalAlignment = Alignment.CenterVertically,
                                onClick = { onExerciseClick(id) },
                                shapes = segmentedShapes(index, exercises.size),
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                leadingContent = {
                                    if (exercise != null) {
                                        ExerciseThumbnail(exercise, Modifier.size(Dimens.ExerciseThumbnailSize))
                                    } else {
                                        Box(Modifier.size(Dimens.ExerciseThumbnailSize))
                                    }
                                },
                                supportingContent = { Text(pluralStringResource(R.plurals.stats_times, times, times)) },
                                trailingContent = { Text(format(value), style = MaterialTheme.typography.titleMedium, color = color) },
                                content = { Text(exercise?.name.orEmpty()) },
                            )
                        }
                    }
                }
            }
            if (done.isNotEmpty()) {
                item(key = "list-title") {
                    SectionTitle(
                        stringResource(
                            when (series.timeline.size) {
                                BucketSize.DAY -> R.string.stats_by_day
                                BucketSize.WEEK -> R.string.stats_by_week
                                BucketSize.MONTH -> R.string.stats_by_month
                            },
                        ),
                    )
                }
                item(key = "list") {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        done.forEachIndexed { index, i ->
                            ListItem(
                                modifier = Modifier.clip(segmentedShapes(index, done.size).shape),
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                headlineContent = { Text(bucketTitle(series.timeline, i, locale)) },
                                trailingContent = {
                                    Text(format(values[i] ?: 0.0), style = MaterialTheme.typography.titleMedium, color = color)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
