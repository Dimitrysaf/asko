package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.BodyMap
import io.github.dimitrysaf.asko.components.ChartValue
import io.github.dimitrysaf.asko.data.stats.timeline
import io.github.dimitrysaf.asko.lib.time.localDate
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Muscle
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.setsPerMuscle
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The muscles worked over a chosen range: the body from the front and the back, each muscle
 * darker the more sets it got, then the muscles by sets, most first (an exercise's main muscles
 * count each set; the others it works, half).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MusclesCard(history: List<Workout>, library: Map<String, Exercise>) {
    var range by rememberSaveable { mutableStateOf(StatsRange.QUARTER) }
    val sets = remember(history, library, range) {
        history.setsPerMuscle(library, range, LocalDate.now(), WeekFields.of(Locale.getDefault()).firstDayOfWeek)
    }
    ChartCard(stringResource(R.string.stats_muscles)) {
        RangePicker(range) { range = it }
        // The sets that went into them on top (each counted once, whatever it worked), "No data" with none.
        val total = remember(history, range) {
            val from = timeline(range, LocalDate.now(), history.minOfOrNull { it.startedAt }?.localDate(), WeekFields.of(Locale.getDefault()).firstDayOfWeek).starts.first()
            history.filter { !it.startedAt.localDate().isBefore(from) }.sumOf { w -> w.exercises.sumOf { it.sets.size } }
        }
        Column {
            ChartValue(if (sets.isEmpty()) stringResource(R.string.chart_no_data) else pluralStringResource(R.plurals.stats_sets, total, total))
            Text(periodLabel(range), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val most = sets.values.maxOrNull() ?: 1.0
        BodyMap(
            // The muscles wger draws; each as dark as its share of the most sets (none shaded with none).
            shade = sets.entries.mapNotNull { (name, count) -> Muscle.of(name)?.let { it to (count / most).toFloat() } }.toMap(),
            modifier = Modifier.fillMaxWidth().height(260.dp),
            description = stringResource(R.string.stats_muscles),
        )
        val rows = sets.entries.sortedByDescending { it.value }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            rows.forEachIndexed { index, (name, count) ->
                val shown = count.roundToInt().coerceAtLeast(1)
                ListItem(
                    modifier = Modifier.clip(segmentedShapes(index, rows.size).shape),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    headlineContent = { Text(name) },
                    trailingContent = {
                        Text(
                            pluralStringResource(R.plurals.stats_sets, shown, shown),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
        }
    }
}
