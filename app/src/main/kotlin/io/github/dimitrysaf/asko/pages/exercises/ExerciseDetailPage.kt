package io.github.dimitrysaf.asko.pages.exercises

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import io.github.dimitrysaf.asko.data.model.MediaType
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.components.segmentedShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import io.github.dimitrysaf.asko.components.ExerciseDemo
import io.github.dimitrysaf.asko.components.ExerciseDemoViewer
import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
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
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.RecordKind
import io.github.dimitrysaf.asko.data.stats.statsFor
import io.github.dimitrysaf.asko.pages.stats.TrainingChart
import io.github.dimitrysaf.asko.pages.stats.WeightChart
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
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
    onOpenStats: (exerciseId: String) -> Unit,
    onEdit: (exerciseId: String) -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel = appViewModel { ExerciseDetailViewModel(exerciseId, it.exerciseRepository, it.favouritesRepository, it.workoutRepository, it.mediaRepository, it.customExerciseRepository) }
    val history by viewModel.history.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favourite by viewModel.favourite.collectAsStateWithLifecycle()

    val loaded = (state as? ExerciseDetailState.Loaded)?.exercise
    val title = loaded?.name.orEmpty()
    // The system's photo picker: a photo, GIF or video of yours, shown first wherever it is.
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && loaded != null) viewModel.addMedia(loaded.id, uri)
    }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    PageScaffold(
        title = title,
        onBack = onBack,
        // The exercise's notes: the same ones it shows in routines and workouts.
        // The star: a favourite, for the Favourites filter.
        actions = {
            if (state is ExerciseDetailState.Loaded) {
                IconToggleButton(checked = favourite, onCheckedChange = viewModel::setFavourite) {
                    Icon(
                        painterResource(if (favourite) R.drawable.ic_star else R.drawable.ic_star_outline),
                        stringResource(if (favourite) R.string.exercise_unfavourite else R.string.exercise_favourite),
                    )
                }
            }
            SubjectNotes(NoteSubject.EXERCISE, exerciseId)
            if (loaded != null) {
                IconButton(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) {
                    Icon(painterResource(R.drawable.ic_image), stringResource(R.string.media_add))
                }
            }
            // One of yours: Edit and Delete, as on a routine.
            loaded?.takeIf { it.source == ExerciseSource.CUSTOM }?.let { mine ->
                IconButton(onClick = { onEdit(mine.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.action_edit)) }
                IconButton(onClick = { confirmingDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val s = state) {
            ExerciseDetailState.Loading -> ExerciseDetailSkeleton(Modifier.pageSkeletonPadding(padding))
            ExerciseDetailState.NotFound -> EmptyState(
                painterResource(R.drawable.ic_fitness),
                stringResource(R.string.exercise_not_found),
                modifier,
            )
            is ExerciseDetailState.Loaded -> ExerciseDetailContent(
                exercise = s.exercise,
                similar = s.similar,
                history = history.orEmpty(),
                onExerciseClick = onExerciseClick,
                onOpenStats = onOpenStats,
                onRemoveMedia = viewModel::removeMedia,
                padding = padding,
            )
        }
    }
    if (confirmingDelete && loaded != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.custom_delete_title, loaded.name)) },
            text = { Text(stringResource(R.string.custom_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        viewModel.retire(loaded.id, onDeleted)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseDetailContent(
    exercise: Exercise,
    similar: List<Exercise>,
    history: List<Workout>,
    onExerciseClick: (exerciseId: String) -> Unit,
    onOpenStats: (exerciseId: String) -> Unit,
    onRemoveMedia: (ExerciseMedia) -> Unit,
    padding: PaddingValues,
) {
    var removingMedia by remember { mutableStateOf<ExerciseMedia?>(null) }
    removingMedia?.let { item ->
        AlertDialog(
            onDismissRequest = { removingMedia = null },
            title = { Text(stringResource(if (item.type == MediaType.VIDEO) R.string.media_remove_video else R.string.media_remove_photo)) },
            text = { Text(stringResource(R.string.media_remove_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removingMedia = null
                        onRemoveMedia(item)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_remove)) }
            },
            dismissButton = { TextButton(onClick = { removingMedia = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    // What you've done with it (under the id of the version shown, like the history).
    val allTime = remember(history, exercise.id) { history.statsFor(exercise.id) }
    var viewingDemo by remember { mutableStateOf(false) }
    if (viewingDemo) ExerciseDemoViewer(exercise, onDismiss = { viewingDemo = false })

    LazyColumn(
        modifier = Modifier.consumeWindowInsets(padding),
        contentPadding = pageContentPadding(padding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // The demo (animation, or photos in turn); tap for full screen.
                ExerciseDemo(
                    exercise = exercise,
                    // On a photo or video of yours: Remove, in its corner (asked first).
                    onRemoveMedia = { removingMedia = it },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.widthIn(max = DemoMaxWidth).fillMaxWidth().aspectRatio(1f)
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
        // Your progress: Stats' charts for this exercise, over the range you pick; All stats opens
        // its records. Only once it's been done.
        if (allTime.sessions > 0) {
            item(key = "progress-title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.exercise_progress)) }
                    IconButton(onClick = { onOpenStats(exercise.id) }, modifier = Modifier.padding(top = Dimens.SpaceS)) {
                        Icon(painterResource(R.drawable.ic_stats), stringResource(R.string.exercise_all_stats))
                    }
                }
            }
            item(key = "progress-training") { TrainingChart(history, exercise.id) }
            if (allTime.bests.containsKey(RecordKind.HEAVIEST)) {
                item(key = "progress-weight") { WeightChart(history, exercise.id) }
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
