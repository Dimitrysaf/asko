package io.github.dimitrysaf.asko.pages.exercises

import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import androidx.compose.ui.res.painterResource
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import io.github.dimitrysaf.asko.components.pageContentPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.ExerciseListItem
import io.github.dimitrysaf.asko.components.ImageViewer
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.RemoteImage
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.SkeletonLine
import io.github.dimitrysaf.asko.components.sharpenEnlarged
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.exercisedb.ExerciseDbApi
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.lib.di.appViewModel

/** Full width on phones; tablets stop here so the demo doesn't dominate the page. */
private val DemoMaxWidth = 480.dp

@Composable
fun ExerciseDetailPage(
    exerciseId: String,
    onBack: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    val viewModel = appViewModel { ExerciseDetailViewModel(exerciseId, it.exerciseRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val title = (state as? ExerciseDetailState.Loaded)?.exercise?.name.orEmpty()
    PageScaffold(
        title = title,
        onBack = onBack,
        // The exercise's notes: the same ones it shows in routines and workouts.
        actions = { SubjectNotes(NoteSubject.EXERCISE, exerciseId) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val s = state) {
            ExerciseDetailState.Loading -> ExerciseDetailSkeleton(Modifier.pageSkeletonPadding(padding))
            ExerciseDetailState.NotFound -> EmptyState(
                painterResource(R.drawable.ic_fitness),
                stringResource(R.string.exercise_not_found),
                modifier,
            )
            is ExerciseDetailState.Loaded -> ExerciseDetailContent(s.exercise, s.similar, onExerciseClick, padding)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    similar: List<Exercise>,
    onExerciseClick: (exerciseId: String) -> Unit,
    padding: PaddingValues,
) {
    var viewingDemo by remember { mutableStateOf(false) }
    if (viewingDemo) ImageViewer(exercise.animationUrl, exercise.name, onDismiss = { viewingDemo = false })

    LazyColumn(
        modifier = Modifier.consumeWindowInsets(padding),
        contentPadding = pageContentPadding(padding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // The animated demo, sharpened (the free GIFs are small); tap for full screen.
                RemoteImage(
                    url = exercise.animationUrl,
                    contentDescription = exercise.name,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.sharpenEnlarged(ExerciseDbApi.GIF_SIZE_PX)
                        .widthIn(max = DemoMaxWidth).fillMaxWidth().aspectRatio(1f)
                        .clickable { viewingDemo = true },
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(
                    text = (exercise.primaryMuscles + exercise.secondaryMuscles).ifEmpty { listOf(exercise.category) }.joinToString(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (exercise.equipment.isNotEmpty()) {
                    Text(
                        text = exercise.equipment.joinToString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (exercise.instructions.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.exercise_how_to)) }
            itemsIndexed(exercise.instructions) { index, step ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (similar.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.exercise_similar)) }
            item {
                // One block, so rows sit at the segmented gap rather than the page's spacing.
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    similar.forEachIndexed { index, other ->
                        ExerciseListItem(other, index, similar.size, onClick = { onExerciseClick(other.id) })
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.credit_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.SpaceL),
            )
        }
    }
}

/** Shaped like the loaded page: demo, muscles line, heading, steps. */
@Composable
private fun ExerciseDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkeletonBox(Modifier.widthIn(max = DemoMaxWidth).fillMaxWidth().aspectRatio(1f), MaterialTheme.shapes.large)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            SkeletonLine(0.5f)
            SkeletonLine(0.4f, Modifier.padding(top = Dimens.SpaceS), height = 24.dp)
            repeat(3) { SkeletonLine(1f) }
            SkeletonLine(0.7f)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Dimens.SpaceS))
}
