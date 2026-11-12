package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.components.PillButton
import io.github.dimitrysaf.asko.components.selectableSegmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * The workout's exercises, full screen: each with how many of its sets are logged, the current
 * one selected. Tap one to go to it; hold and drag to move it (supersets behave as in the routine
 * editor). An exercise with nothing logged yet can be swapped for another or removed. Add
 * exercises is pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutExercisesDialog(
    viewModel: ActiveWorkoutViewModel,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onSwap: (exerciseKey: Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromKey = from.key as? Int ?: return@rememberReorderableLazyListState
        val toKey = to.key as? Int ?: return@rememberReorderableLazyListState
        viewModel.moveExercise(fromKey, toKey)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // A Surface, so text and icons get the theme's content colour (a Dialog gives none).
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxSize()
                    .systemBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) }
                    Text(stringResource(R.string.workout_exercises), style = MaterialTheme.typography.titleLarge)
                }
                val items = viewModel.items
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Dimens.PageGutter, vertical = Dimens.SpaceS),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(items, key = { _, e -> e.key }) { index, exercise ->
                        ReorderableItem(reorder, key = exercise.key) { dragging ->
                            val details = viewModel.library[exercise.exerciseId]
                            val logged = exercise.sets.count { it.status == SetStatus.DONE || it.status == SetStatus.FAILED }
                            val changeable = viewModel.canChange(exercise)
                            SegmentedListItem(
                                selected = dragging || index == viewModel.exerciseIndex,
                                onClick = {
                                    viewModel.openExercise(exercise.key)
                                    onDismiss()
                                },
                                shapes = selectableSegmentedShapes(index, items.size),
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.longPressDraggableHandle(
                                    onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                ),
                                leadingContent = {
                                    if (details != null) {
                                        ExerciseThumbnail(details, Modifier.size(Dimens.ExerciseThumbnailSize))
                                    } else {
                                        Box(Modifier.size(Dimens.ExerciseThumbnailSize))
                                    }
                                },
                                supportingContent = { Text(pluralStringResource(R.plurals.workout_sets_logged, exercise.sets.size, logged, exercise.sets.size)) },
                                // Swap and remove only while nothing of it is logged.
                                trailingContent = if (changeable) {
                                    {
                                        Row {
                                            IconButton(onClick = { onSwap(exercise.key) }) {
                                                Icon(painterResource(R.drawable.ic_swap), stringResource(R.string.workout_swap_exercise))
                                            }
                                            IconButton(onClick = { viewModel.removeExercise(exercise.key) }) {
                                                Icon(Icons.Filled.Delete, stringResource(R.string.routine_remove_exercise))
                                            }
                                        }
                                    }
                                } else {
                                    null
                                },
                                content = { Text(details?.name.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
                PillButton(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.routine_add_exercises),
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = Dimens.SpaceS),
                )
            }
        }
    }
}
