package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.components.WithTooltip
import io.github.dimitrysaf.asko.components.StreakDialog
import io.github.dimitrysaf.asko.navigation.LocalTabShown
import io.github.dimitrysaf.asko.components.RoutineListSkeleton

import io.github.dimitrysaf.asko.components.StreakHeroSkeleton

import io.github.dimitrysaf.asko.pages.workout.ActiveWorkoutViewModel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import io.github.dimitrysaf.asko.components.StreakHero
import io.github.dimitrysaf.asko.components.segmentedShapes
import androidx.compose.ui.Alignment
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
    onStartRoutine: (routineId: Long) -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
) {
    val viewModel = appViewModel { RoutinesViewModel(it.workoutRepository, it.routineRepository, it.exerciseRepository, it.activeWorkoutStore, it.streakRepository) }
    val workoutDays by viewModel.workoutDays.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    // A streak lost to missed days: ask why (once per session if the sheet is swiped away).
    var lostAsked by rememberSaveable { mutableStateOf(false) }
    // Only while this tab is the one shown (it stays built, hidden, behind the others).
    streak?.lost?.takeIf { !lostAsked && LocalTabShown.current }?.let { lost ->
        LostStreakSheet(
            lost = lost,
            onExcuse = { excuse ->
                lostAsked = true
                viewModel.excuse(lost.missed, excuse)
            },
            onLetGo = {
                lostAsked = true
                viewModel.letGo(lost.missed)
            },
            onDismiss = { lostAsked = true },
        )
    }
    var reasonDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    reasonDay?.let { day ->
        DayReasonSheet(
            day = day,
            current = streak?.markOf(day).excuse(),
            onPick = {
                viewModel.setExcuse(day, it)
                reasonDay = null
            },
            onDismiss = { reasonDay = null },
        )
    }
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
        // A routine moves within its folder; Move to folder takes it to another.
        if (!current[fromIndex].routine.folder.equals(current[toIndex].routine.folder, ignoreCase = true)) return@rememberReorderableLazyListState
        current.add(toIndex, current.removeAt(fromIndex))
        dragged = current
    }
    // Once the saved order comes back from the database, the list follows it again.
    LaunchedEffect(routines) { if (!reorder.isAnyItemDragging) dragged = null }
    var naming by rememberSaveable { mutableStateOf(false) }
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val averageDurations by viewModel.averageDurations.collectAsStateWithLifecycle()
    // Start on a routine while another's workout is in progress: asks first (its routine id).
    var confirmingStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var choosingLog by rememberSaveable { mutableStateOf(false) }
    /** Starts a routine's workout, or one started empty; asks first if another is in progress. */
    fun start(routineId: Long) {
        val active = activeWorkout
        if (active == null || active.routineId == routineId) onStartRoutine(routineId) else confirmingStart = routineId
    }
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
            WithTooltip(stringResource(R.string.stats_page_title)) {
                IconButton(onClick = onStats) { Icon(painterResource(R.drawable.ic_stats), stringResource(R.string.stats_page_title)) }
            }
            WithTooltip(stringResource(R.string.settings_title)) {
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings_title)) }
            }
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
                // A segmented list: the streak, then this week (each day shows its mark).
                // Until the streak is worked out, a skeleton holds its place, so nothing below moves.
                val s = streak
                val count = 2
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    var showingStreak by remember { mutableStateOf(false) }
                    if (showingStreak && s != null) StreakDialog(s) { showingStreak = false }
                    if (s != null) StreakHero(s, segmentedShapes(0, count).shape, onClick = { showingStreak = true }) else StreakHeroSkeleton(segmentedShapes(0, count).shape)
                    Surface(shape = segmentedShapes(count - 1, count).shape, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                            Text(stringResource(R.string.routines_this_week), style = MaterialTheme.typography.titleMedium)
                            WeekStreak(
                                workoutDays,
                                onDayClick,
                                // Holding a missed day asks why: sick, travelling, or just missed.
                                onDayLongClick = { day -> if (canGiveReason(day, workoutDays)) reasonDay = day },
                                marks = s?.let { it::markOf },
                                counts = s?.let { st -> { day: LocalDate -> st.counts(day) } },
                            )
                        }
                    }
                }
            }
            item(key = "actions") {
                Row(Modifier.padding(top = PageSpacing), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    // Widths follow the labels (1 : 1.5) so "Create routine" fits on one line next to "Log".
                    PillButton(Icons.Filled.Add, stringResource(R.string.action_log), onClick = { choosingLog = true }, Modifier.weight(1f))
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
                list == null -> item(key = "routines-loading") { RoutineListSkeleton() }
                list.isEmpty() -> item(key = "routines-empty") {
                    EmptyState(
                        rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                        stringResource(R.string.routines_empty),
                        compact = true,
                    )
                }
                // Routines outside a folder first, then each folder under its title (in the order the
                // list puts them). Hold a routine to pick it up and drag it into place in its group.
                else -> list.groupBy { it.routine.folder?.lowercase() }.entries
                    .sortedBy { it.key != null }
                    .forEachIndexed { g, (_, group) ->
                        val folder = group.first().routine.folder
                        if (folder != null) {
                            item(key = "folder-${folder.lowercase()}") {
                                FolderTitle(folder, Modifier.padding(top = if (g > 0) GroupSpacing else 0.dp))
                            }
                        }
                        itemsIndexed(group, key = { _, summary -> summary.routine.id }) { index, summary ->
                            ReorderableItem(reorder, key = summary.routine.id) { dragging ->
                                RoutineListItem(
                                    routine = summary.routine,
                                    exercises = summary.exercises,
                                    index = index,
                                    count = group.size,
                                    // A workout in progress shows as selected (so does the routine being dragged);
                                    // tapping it goes straight back into the workout.
                                    selected = dragging || summary.routine.id == activeWorkout?.routineId,
                                    inProgress = summary.routine.id == activeWorkout?.routineId,
                                    modifier = Modifier.longPressDraggableHandle(
                                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                        onDragStopped = {
                                            dragged?.let { order -> viewModel.setOrder(order.map { it.routine.id }) }
                                        },
                                    ),
                                    averageDuration = averageDurations[summary.routine.name],
                                    // Its own workout in progress: back into it. Another's: ask first.
                                    onStart = { start(summary.routine.id) },
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

    if (choosingLog) {
        LogChoiceSheet(
            resumingEmpty = activeWorkout?.routineId == ActiveWorkoutViewModel.EMPTY_WORKOUT,
            onStartEmpty = {
                choosingLog = false
                start(ActiveWorkoutViewModel.EMPTY_WORKOUT)
            },
            onLogPast = {
                choosingLog = false
                onLog()
            },
            onDismiss = { choosingLog = false },
        )
    }

    val other = activeWorkout
    val starting = confirmingStart
    if (starting != null && other != null) {
        AlertDialog(
            onDismissRequest = { confirmingStart = null },
            title = { Text(stringResource(R.string.workout_in_progress)) },
            text = { Text(stringResource(R.string.workout_switch_message, other.routineName)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingStart = null
                    onResumeWorkout(other.routineId)
                }) { Text(stringResource(R.string.workout_resume)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmingStart = null
                        viewModel.discardActiveWorkout { onStartRoutine(starting) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.workout_discard_and_start)) }
            },
        )
    }
}

/** Space between the page's sections (the list itself is spaced by the segmented gap). */
private val PageSpacing = Dimens.SpaceM

/**
 * What Log does, picked in a sheet: start an empty workout (the clock runs, exercises are added as
 * you go; Resume when one is in progress), or log a workout already done, for now or another day.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogChoiceSheet(resumingEmpty: Boolean, onStartEmpty: () -> Unit, onLogPast: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.log_choice_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            SegmentedListItem(
                verticalAlignment = Alignment.CenterVertically,
                onClick = onStartEmpty,
                shapes = segmentedShapes(0, 2),
                leadingContent = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                supportingContent = { Text(stringResource(R.string.log_choice_empty_hint)) },
                content = { Text(stringResource(if (resumingEmpty) R.string.log_choice_resume_empty else R.string.log_choice_empty)) },
            )
            SegmentedListItem(
                verticalAlignment = Alignment.CenterVertically,
                onClick = onLogPast,
                shapes = segmentedShapes(1, 2),
                leadingContent = { Icon(painterResource(R.drawable.ic_history), contentDescription = null) },
                supportingContent = { Text(stringResource(R.string.log_choice_past_hint)) },
                content = { Text(stringResource(R.string.log_choice_past)) },
            )
        }
    }
}
