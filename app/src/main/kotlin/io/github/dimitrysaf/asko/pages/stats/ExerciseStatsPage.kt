package io.github.dimitrysaf.asko.pages.stats

import io.github.dimitrysaf.asko.components.WithTooltip
import io.github.dimitrysaf.asko.data.stats.RecordEvent
import io.github.dimitrysaf.asko.data.stats.RepMax
import io.github.dimitrysaf.asko.data.stats.recordHistory
import io.github.dimitrysaf.asko.data.stats.repMaxes
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.data.stats.statsFor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ExerciseListSkeleton
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.pageContentPadding
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.stats.ExerciseStats
import io.github.dimitrysaf.asko.data.stats.RecordKind
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.lib.format.formatDate
import io.github.dimitrysaf.asko.pages.workout.recordName
import io.github.dimitrysaf.asko.pages.workout.recordValue

/**
 * One exercise's stats, from the Stats page: an area chart of its sets, reps and (for timed sets)
 * time, each toggled on or off, and one of its heaviest weight, each over a range you pick; then
 * its all-time bests, its heaviest set for each number of reps, and every record it has set.
 * ⓘ opens the exercise's page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseStatsPage(exerciseId: String, onBack: () -> Unit, onOpenExercise: () -> Unit) {
    val viewModel = appViewModel { ExerciseStatsViewModel(exerciseId, it.workoutRepository, it.exerciseRepository) }
    val history by viewModel.history.collectAsStateWithLifecycle()

    PageScaffold(
        title = viewModel.exercise?.name.orEmpty(),
        onBack = onBack,
        actions = {
            WithTooltip(stringResource(R.string.stats_open_exercise)) {
                IconButton(onClick = onOpenExercise) { Icon(Icons.Filled.Info, stringResource(R.string.stats_open_exercise)) }
            }
        },
    ) { padding ->
        val done = history
        if (done == null) {
            Column(Modifier.pageSkeletonPadding(padding), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                SkeletonBox(Modifier.fillMaxWidth().height(320.dp), MaterialTheme.shapes.extraLarge)
                ExerciseListSkeleton(count = 3)
            }
            return@PageScaffold
        }
        // The version shown of this exercise: the id its sets are counted under (see the view model).
        val shownId = viewModel.exercise?.id ?: exerciseId
        val allTime = remember(done, shownId) { done.statsFor(shownId) }
        val repMaxes = remember(done, shownId) { done.repMaxes(shownId) }
        val records = remember(done, shownId) { done.recordHistory(shownId) }
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            item(key = "training") {
                var metric by rememberSaveable { mutableStateOf(TrainingMetric.SETS) }
                TrainingChart(done, metric, { metric = it }, shownId)
            }
            if (allTime.bests.containsKey(RecordKind.HEAVIEST)) {
                item(key = "weight") { WeightChart(done, shownId) }
            }
            item(key = "all-time-title") { SectionTitle(stringResource(R.string.stats_all_time)) }
            item(key = "all-time") { AllTimeList(allTime) }
            if (repMaxes.isNotEmpty()) {
                item(key = "rep-max-title") { SectionTitle(stringResource(R.string.stats_rep_maxes)) }
                item(key = "rep-max") { RepMaxList(repMaxes) }
            }
            if (records.isNotEmpty()) {
                item(key = "record-history-title") { SectionTitle(stringResource(R.string.stats_record_history)) }
                item(key = "record-history") { RecordHistoryList(records) }
            }
        }
    }
}

/** Each all-time best, how often it's been done, and when last. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AllTimeList(stats: ExerciseStats) {
    val rows = buildList {
        RecordKind.entries.forEach { kind -> stats.bests[kind]?.let { add(recordName(kind) to recordValue(kind, it)) } }
        add(
            stringResource(R.string.stats_done) to listOf(
                pluralStringResource(R.plurals.stats_times, stats.sessions, stats.sessions),
                pluralStringResource(R.plurals.stats_sets, stats.sets, stats.sets),
            ).joinToString(" · "),
        )
        stats.lastDone?.let { add(stringResource(R.string.stats_last_done) to it.formatDate()) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { index, (label, value) ->
            ListItem(
                modifier = Modifier.clip(segmentedShapes(index, rows.size).shape),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                headlineContent = { Text(label) },
                trailingContent = { Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
            )
        }
    }
}

/** The heaviest set for each number of reps (1 to 12), with when it was lifted. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RepMaxList(maxes: List<RepMax>) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        maxes.forEachIndexed { index, max ->
            ListItem(
                modifier = Modifier.clip(segmentedShapes(index, maxes.size).shape),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                headlineContent = { Text(pluralStringResource(R.plurals.record_reps, max.reps, max.reps)) },
                supportingContent = { Text(max.at.formatDate()) },
                trailingContent = {
                    Text(weightValue(max.weightKg), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                },
            )
        }
    }
}

/** Every record the exercise has set, newest first: which, the new best, when, and what it beat. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecordHistoryList(events: List<RecordEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        events.forEachIndexed { index, event ->
            ListItem(
                modifier = Modifier.clip(segmentedShapes(index, events.size).shape),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                headlineContent = { Text(recordName(event.kind)) },
                supportingContent = {
                    Text(stringResource(R.string.stats_record_when, event.at.formatDate(), recordValue(event.kind, event.previous)))
                },
                trailingContent = {
                    Text(recordValue(event.kind, event.value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                },
            )
        }
    }
}
