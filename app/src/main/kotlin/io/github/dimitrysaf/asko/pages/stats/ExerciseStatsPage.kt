package io.github.dimitrysaf.asko.pages.stats

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
 * its all-time bests. ⓘ opens the exercise's page.
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
            IconButton(onClick = onOpenExercise) { Icon(Icons.Filled.Info, stringResource(R.string.stats_open_exercise)) }
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
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            item(key = "training") { TrainingChart(done, shownId) }
            if (allTime.bests.containsKey(RecordKind.HEAVIEST)) {
                item(key = "weight") { WeightChart(done, shownId) }
            }
            item(key = "all-time-title") { SectionTitle(stringResource(R.string.stats_all_time)) }
            item(key = "all-time") { AllTimeList(allTime) }
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
