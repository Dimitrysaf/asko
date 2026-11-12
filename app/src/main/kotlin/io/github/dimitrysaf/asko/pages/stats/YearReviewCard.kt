package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.YearHeatMap
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.yearReview
import io.github.dimitrysaf.asko.data.stats.years
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.weightValue
import java.text.NumberFormat
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A year in review, in a card: a chip per year (when there's more than one), the year's heat map
 * (sets per day), then what it added up to: workouts and days, time, sets, reps, volume, records
 * set and the exercise done most.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YearReviewCard(history: List<Workout>, library: Map<String, Exercise>) {
    val years = remember(history) { history.years() }
    if (years.isEmpty()) return
    var year by rememberSaveable { mutableIntStateOf(years.first()) }
    if (year !in years) year = years.first()
    val review = remember(history, year) { history.yearReview(year) }
    val number = remember { NumberFormat.getIntegerInstance() }
    val unit = LocalWeightUnit.current

    ChartCard(stringResource(R.string.stats_review_title, year)) {
        if (years.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                years.forEach { y -> FilterChip(selected = y == year, onClick = { year = y }, label = { Text(y.toString()) }) }
            }
        }
        YearHeatMap(year, review.setsByDay, WeekFields.of(Locale.getDefault()).firstDayOfWeek)
        val hours = review.time.toHours().toInt()
        val minutes = review.time.toMinutesPart()
        val rows = buildList {
            add(stringResource(R.string.stats_review_workouts) to number.format(review.workouts))
            add(stringResource(R.string.stats_review_days) to number.format(review.activeDays))
            add(
                stringResource(R.string.finished_time) to
                    if (hours > 0) stringResource(R.string.stats_hours_minutes, hours, minutes) else stringResource(R.string.stats_minutes, minutes),
            )
            add(stringResource(R.string.finished_sets) to number.format(review.sets))
            add(stringResource(R.string.finished_reps) to number.format(review.reps))
            if (review.volumeKg > 0) {
                add(stringResource(R.string.finished_volume) to weightValue(unit.toKg(unit.fromKg(review.volumeKg).roundToInt().toDouble())))
            }
            add(stringResource(R.string.stats_review_records) to number.format(review.records))
            review.topExerciseId?.let(library::get)?.let { add(stringResource(R.string.stats_review_top) to it.name) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, (label, value) ->
                ListItem(
                    modifier = Modifier.clip(segmentedShapes(index, rows.size).shape),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    headlineContent = { Text(label) },
                    trailingContent = {
                        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    },
                )
            }
        }
    }
}
