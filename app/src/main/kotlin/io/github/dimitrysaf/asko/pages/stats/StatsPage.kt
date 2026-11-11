package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.ExerciseListSkeleton
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.pageContentPadding
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.pages.workout.RecordList

/**
 * Stats, from the Routines tab: an area chart of sets, reps and time (each toggled on or off) and
 * one of the weight lifted, each over a range you pick; the latest personal records; and every
 * exercise you've done, most sets first. Tap an exercise for its own stats.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsPage(onBack: () -> Unit, onExerciseClick: (exerciseId: String) -> Unit) {
    val viewModel = appViewModel { StatsViewModel(it.workoutRepository, it.exerciseRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    PageScaffold(title = stringResource(R.string.stats_page_title), onBack = onBack) { padding ->
        val stats = state
        when {
            stats == null -> Column(Modifier.pageSkeletonPadding(padding), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                SkeletonBox(Modifier.fillMaxWidth().height(320.dp), MaterialTheme.shapes.extraLarge)
                ExerciseListSkeleton(count = 4)
            }
            stats.history.isEmpty() -> EmptyState(
                painterResource(R.drawable.ic_stats),
                stringResource(R.string.stats_empty),
                Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.consumeWindowInsets(padding),
                contentPadding = pageContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                item(key = "training") { TrainingChart(stats.history) }
                item(key = "weight") { WeightChart(stats.history) }
                if (stats.latestRecords.isNotEmpty()) {
                    item(key = "records-title") { SectionTitle(stringResource(R.string.stats_latest_records)) }
                    item(key = "records") {
                        RecordList(
                            records = stats.latestRecords.map { it.record },
                            library = viewModel.library,
                            dates = stats.latestRecords.map { it.at },
                        )
                    }
                }
                item(key = "most-used-title") { SectionTitle(stringResource(R.string.stats_most_used)) }
                // One block, so rows sit at the segmented gap rather than the page's spacing.
                item(key = "most-used") {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        stats.mostUsed.forEachIndexed { index, row ->
                            MostUsedItem(row, viewModel.library[row.exerciseId], index, stats.mostUsed.size) {
                                onExerciseClick(row.exerciseId)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One exercise in the most used list: picture, name, its sets and times. Tap for its stats. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MostUsedItem(row: ExerciseRow, exercise: Exercise?, index: Int, count: Int, onClick: () -> Unit) {
    val stats = row.stats
    SegmentedListItem(
        onClick = onClick,
        shapes = segmentedShapes(index, count),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            if (exercise != null) {
                ExerciseThumbnail(exercise, Modifier.size(Dimens.ExerciseThumbnailSize))
            } else {
                Box(Modifier.size(Dimens.ExerciseThumbnailSize))
            }
        },
        supportingContent = {
            Text(
                listOf(
                    pluralStringResource(R.plurals.stats_sets, stats.sets, stats.sets),
                    pluralStringResource(R.plurals.stats_times, stats.sessions, stats.sessions),
                ).joinToString(" · "),
            )
        },
        content = { Text(exercise?.name.orEmpty()) },
    )
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Dimens.SpaceS))
}
