package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.components.EmptyState
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.components.pageContentPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ExerciseListSkeleton
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.PillButton
import io.github.dimitrysaf.asko.components.RoutineListItem
import io.github.dimitrysaf.asko.components.WeekStreak
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.lib.di.appViewModel
import java.time.LocalDate

/**
 * The Routines tab: this week, Log / Create routine, and your routines.
 * [onDayClick]: a day in the week strip was tapped (opens History on it).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutinesPage(
    onDayClick: (LocalDate) -> Unit,
    onLog: () -> Unit,
    onCreateRoutine: (name: String) -> Unit,
    onRoutineClick: (routineId: Long) -> Unit,
    onResumeWorkout: (routineId: Long) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
) {
    val viewModel = appViewModel { RoutinesViewModel(it.workoutRepository, it.routineRepository, it.exerciseRepository, it.activeWorkoutStore) }
    val workoutDays by viewModel.workoutDays.collectAsStateWithLifecycle()
    val routines by viewModel.list.collectAsStateWithLifecycle()
    // While dragging, the list moves here; the order is saved when the routine is dropped.
    var dragged by remember { mutableStateOf<List<RoutineSummary>?>(null) }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
        val toId = to.key as? Long ?: return@rememberReorderableLazyListState
        val current = (dragged ?: routines ?: return@rememberReorderableLazyListState).toMutableList()
        val fromIndex = current.indexOfFirst { it.routine.id == fromId }
        val toIndex = current.indexOfFirst { it.routine.id == toId }
        if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
        current.add(toIndex, current.removeAt(fromIndex))
        dragged = current
    }
    // Once the saved order comes back from the database, the list follows it again.
    LaunchedEffect(routines) { if (!reorder.isAnyItemDragging) dragged = null }
    var naming by rememberSaveable { mutableStateOf(false) }
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    if (naming) {
        RoutineNameDialog(
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { name ->
                naming = false
                onCreateRoutine(name)
            },
            onDismiss = { naming = false },
        )
    }

    PageScaffold(
        title = stringResource(R.string.nav_routines),
        actions = {
            IconButton(onClick = onStats) { Icon(painterResource(R.drawable.ic_stats), stringResource(R.string.stats_page_title)) }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings_title)) }
        },
    ) { padding ->
        // Routines sit at the segmented gap; everything else is set apart by the page's spacing.
        LazyColumn(
            state = listState,
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding, top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            item(key = "week") {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                        Text(stringResource(R.string.routines_this_week), style = MaterialTheme.typography.titleMedium)
                        WeekStreak(workoutDays, onDayClick)
                    }
                }
            }
            item(key = "actions") {
                Row(Modifier.padding(top = PageSpacing), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    // Widths follow the labels (1 : 1.5) so "Create routine" fits on one line next to "Log".
                    PillButton(Icons.Filled.Add, stringResource(R.string.action_log), onClick = onLog, Modifier.weight(1f))
                    PillButton(Icons.Filled.Edit, stringResource(R.string.action_create_routine), onClick = { naming = true }, Modifier.weight(1.5f))
                }
            }
            item(key = "routines-title") {
                Text(
                    stringResource(R.string.routines_my_routines),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = PageSpacing + Dimens.SpaceS, bottom = PageSpacing),
                )
            }
            val list = dragged ?: routines
            when {
                list == null -> item(key = "routines-loading") { ExerciseListSkeleton(count = 3) }
                list.isEmpty() -> item(key = "routines-empty") {
                    EmptyState(
                        rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                        stringResource(R.string.routines_empty),
                        compact = true,
                    )
                }
                // Hold a routine to pick it up and drag it into place.
                else -> itemsIndexed(list, key = { _, summary -> summary.routine.id }) { index, summary ->
                    ReorderableItem(reorder, key = summary.routine.id) { dragging ->
                        RoutineListItem(
                            routine = summary.routine,
                            exercises = summary.exercises,
                            index = index,
                            count = list.size,
                            // A workout in progress shows as selected (so does the routine being dragged);
                            // tapping it goes straight back into the workout.
                            selected = dragging || summary.routine.id == activeWorkout?.routineId,
                            modifier = Modifier.longPressDraggableHandle(
                                onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragStopped = {
                                    dragged?.let { order -> viewModel.setOrder(order.map { it.routine.id }) }
                                },
                            ),
                            onClick = {
                                if (summary.routine.id == activeWorkout?.routineId) {
                                    onResumeWorkout(summary.routine.id)
                                } else {
                                    onRoutineClick(summary.routine.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Space between the page's sections (the list itself is spaced by the segmented gap). */
private val PageSpacing = Dimens.SpaceM
