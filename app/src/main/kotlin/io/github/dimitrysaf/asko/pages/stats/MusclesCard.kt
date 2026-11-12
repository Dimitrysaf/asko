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
import io.github.dimitrysaf.asko.components.EmptyState
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
        if (sets.isEmpty()) {
            EmptyState(painterResource(R.drawable.ic_fitness), stringResource(R.string.stats_nothing_done), compact = true)
            return@ChartCard
        }
        val most = sets.values.max()
        BodyMap(
            // The muscles wger draws; each as dark as its share of the most sets.
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
