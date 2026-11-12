package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.components.PlateCalculatorSheet
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import io.github.dimitrysaf.asko.components.pageContentPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import io.github.dimitrysaf.asko.components.animatedSegmentedShape
import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.WeightUnit
import io.github.dimitrysaf.asko.lib.format.sameWeight
import io.github.dimitrysaf.asko.lib.format.weightUnitSymbol
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.components.DurationInput
import io.github.dimitrysaf.asko.components.rememberDurationInputState
import io.github.dimitrysaf.asko.components.PinnedPillBar
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.SkeletonLine
import io.github.dimitrysaf.asko.components.selectableSegmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.lib.di.appViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


/**
 * Create ([routineId] 0, named [initialName]) or edit a routine: Discard / name ✎ / Save on top,
 * Add exercises pinned at the bottom, the exercises in [PlanEditorScaffold]. Nothing is stored
 * until Save.
 */
@Composable
fun RoutineEditorPage(
    routineId: Long,
    initialName: String,
    onBack: () -> Unit,
    onSaved: (routineId: Long) -> Unit,
) {
    val viewModel = appViewModel { RoutineEditorViewModel(routineId, initialName, it.routineRepository, it.exerciseRepository, it.settings) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    PlanEditorScaffold(
        viewModel = viewModel,
        onBack = onBack,
        discardMessage = stringResource(R.string.routine_discard_message),
        emptyMessage = stringResource(R.string.routine_empty_message),
        title = { EditableTitle(viewModel.name, stringResource(R.string.routine_rename)) { renaming = true } },
        actions = {
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = viewModel.canSave,
                modifier = Modifier.padding(end = Dimens.SpaceS),
            ) { Text(stringResource(R.string.action_save)) }
        },
        bottomBar = { openPicker ->
            PinnedPillBar(Icons.Filled.Add, stringResource(R.string.routine_add_exercises), onClick = openPicker)
        },
    )
    if (renaming) {
        RoutineNameDialog(
            initialName = viewModel.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.rename(it)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}

/**
 * The editor screen shared by routines and logged workouts: a red Discard (asking first when there
 * are changes), [title] and [actions] on top, [bottomBar] pinned below, and the exercises as a
 * segmented list. Each exercise is a read-only preview until tapped; tap to edit its sets
 * (tap a set badge for its type), rest and Reps/Time; hold to select it and drag it into place.
 * [actions] and [bottomBar] get openPicker, which opens the exercise picker. With [startPicking]
 * the picker is open from the start, and closing it with no exercises yet leaves via [onBack].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanEditorScaffold(
    viewModel: PlanEditorViewModel,
    onBack: () -> Unit,
    discardMessage: String,
    emptyMessage: String,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.(openPicker: () -> Unit) -> Unit,
    bottomBar: @Composable (openPicker: () -> Unit) -> Unit,
    startPicking: Boolean = false,
) {
    var picking by rememberSaveable { mutableStateOf(startPicking) }
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    val discard = { if (viewModel.dirty) confirmingDiscard = true else onBack() }
    val openPicker = { picking = true }
    BackHandler(onBack = discard)
    BackHandler(enabled = viewModel.selectedKey != null) { viewModel.clearSelection() }
    val byId = viewModel.library.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Button(
                        onClick = discard,
                        modifier = Modifier.padding(start = Dimens.SpaceS),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(R.string.action_discard)) }
                },
                title = title,
                actions = { actions(openPicker) },
            )
        },
        bottomBar = { bottomBar(openPicker) },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()
        when {
            !viewModel.loaded -> Column(Modifier.pageSkeletonPadding(padding), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                repeat(3) { SkeletonBox(Modifier.fillMaxWidth().height(88.dp), MaterialTheme.shapes.large) }
            }
            viewModel.items.isEmpty() -> EmptyEditor(emptyMessage, modifier)
            // The list scrolls behind the bars instead of stopping short of them.
            else -> ExerciseBlocks(viewModel, byId, pageContentPadding(padding), Modifier.consumeWindowInsets(padding).imePadding())
        }
    }

    if (picking) {
        ExercisePicker(
            exercises = viewModel.browsable,
            onAdd = { ids ->
                viewModel.addExercises(ids)
                picking = false
            },
            onDismiss = {
                picking = false
                if (startPicking && viewModel.items.isEmpty()) onBack()
            },
        )
    }
    if (confirmingDiscard) {
        AlertDialog(
            onDismissRequest = { confirmingDiscard = false },
            title = { Text(stringResource(R.string.routine_discard_title)) },
            text = { Text(discardMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDiscard = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDiscard = false }) { Text(stringResource(R.string.routine_keep_editing)) }
            },
        )
    }
}

/** A top bar title spanning its space, with ✎ at the right end; tapping it edits ([editDescription]). */
@Composable
fun EditableTitle(text: String, editDescription: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Edit, contentDescription = editDescription, modifier = Modifier.size(18.dp))
    }
}

/** Centred icon in an expressive shape, with the hint below it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyEditor(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.SpaceXl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(Dimens.SpaceL))
        Text(stringResource(R.string.routine_empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.SpaceS))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The exercises as a segmented list, supersets as groups of their own under a title; hold an
 * exercise to select it and drag it into place.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseBlocks(
    viewModel: PlanEditorViewModel,
    byId: Map<String, Exercise>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        // Superset titles aren't exercises; nothing moves onto them.
        val fromKey = from.key as? Int
        val toKey = to.key as? Int
        if (fromKey != null && toKey != null) viewModel.move(fromKey, toKey)
    }
    val segments = viewModel.items.segments { it.supersetWithNext }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        viewModel.items.forEachIndexed { index, draft ->
            val segment = segments.getOrNull(index) ?: return@forEachIndexed
            if (segment.supersetStart) {
                item(key = "superset-${draft.key}") {
                    SupersetTitle(Modifier.animateItem().padding(top = if (segment.groupStart) GroupSpacing else 0.dp))
                }
            }
            item(key = draft.key) {
                val expanded = viewModel.expandedKey == draft.key
                // Only closed exercises can be dragged; an open one is being edited.
                ReorderableItem(reorder, key = draft.key, enabled = !expanded) { dragging ->
                    ExerciseBlock(
                        item = draft,
                        exercise = byId[draft.exerciseId],
                        index = segment.index,
                        count = segment.count,
                        showRest = !segment.restHidden,
                        canSuperset = index < viewModel.items.lastIndex,
                        supersetNumber = segment.supersetNumber,
                        modifier = if (segment.groupStart && !segment.supersetStart) Modifier.padding(top = GroupSpacing) else Modifier,
                        expanded = expanded,
                        selected = viewModel.selectedKey == draft.key,
                        dragging = dragging,
                        // A long press selects the exercise (without opening it) and picks it up.
                        dragHandle = Modifier.longPressDraggableHandle(
                            enabled = !expanded,
                            onDragStarted = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.select(draft.key)
                            },
                        ),
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

/**
 * One exercise. Closed, it is a read-only preview of the open one: the same rest and set rows,
 * values only (no field backgrounds, no buttons, no Reps/Time switch); tap it to open it.
 * Selected or open, its corners round evenly.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseBlock(
    item: DraftExercise,
    exercise: Exercise?,
    index: Int,
    count: Int,
    showRest: Boolean,
    canSuperset: Boolean,
    supersetNumber: Int?,
    modifier: Modifier,
    expanded: Boolean,
    selected: Boolean,
    dragging: Boolean,
    dragHandle: Modifier,
    viewModel: PlanEditorViewModel,
) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val highlighted = expanded || selected
    // Open or selected stands out; otherwise a superset's exercises share its colour.
    val container by animateColorAsState(
        when {
            highlighted -> colors.secondaryContainer
            supersetNumber != null -> supersetColor(supersetNumber)
            else -> colors.surfaceContainer
        },
        motion.defaultEffectsSpec(),
        label = "block",
    )
    val bodyweight = exercise?.isBodyweight == true
    var confirmingRemove by rememberSaveable { mutableStateOf(false) }
    val toggle = { viewModel.toggleExpanded(item.key) }
    Surface(
        modifier = modifier,
        shape = animatedSegmentedShape(index, count, rounded = highlighted),
        color = container,
        shadowElevation = if (dragging) 8.dp else 0.dp,
    ) {
        // Closed: the whole block opens it and is the drag handle. Open: only the header closes it.
        Column(if (expanded) Modifier else Modifier.then(dragHandle).clickable(onClick = toggle)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.clickable(onClick = toggle) else Modifier)
                    .padding(Dimens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                if (exercise != null) {
                    ExerciseThumbnail(exercise, Modifier.size(ThumbnailSize))
                } else {
                    SkeletonBox(Modifier.size(ThumbnailSize))
                }
                Box(Modifier.weight(1f)) {
                    if (exercise != null) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    } else {
                        SkeletonLine(0.6f)
                    }
                }
                // The exercise's notes (the same in every routine), then the superset link.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubjectNotes(NoteSubject.EXERCISE, item.exerciseId, tonal = true)
                    // Superset with the exercise after it: on, it stays lit to show the link.
                    if (canSuperset) {
                        FilledTonalIconToggleButton(
                            checked = item.supersetWithNext,
                            onCheckedChange = { viewModel.setSuperset(item.key, it) },
                        ) {
                            Icon(painterResource(R.drawable.ic_link), stringResource(R.string.routine_superset_with_next))
                        }
                    }
                }
            }
            Column(Modifier.padding(start = Dimens.SpaceM, end = Dimens.SpaceM, bottom = Dimens.SpaceM)) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                    exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    TrackSelector(item, viewModel, Modifier.padding(bottom = Dimens.SpaceS))
                }
                // In a superset, only the last exercise's rest is used: the rest after each round.
                AnimatedVisibility(
                    visible = showRest,
                    enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                    exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    RestControl(item, editable = expanded, viewModel = viewModel)
                }
                SetTable(item, bodyweight, viewModel, editable = expanded)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                    exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    Column(Modifier.padding(top = Dimens.SpaceS), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                        Button(onClick = { viewModel.addSet(item.key) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Dimens.SpaceS))
                            Text(stringResource(R.string.routine_add_set))
                        }
                        FilledTonalButton(onClick = { viewModel.duplicateExercise(item.key) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(painterResource(R.drawable.ic_copy), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Dimens.SpaceS))
                            Text(stringResource(R.string.routine_duplicate_exercise))
                        }
                        Button(
                            onClick = { confirmingRemove = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Dimens.SpaceS))
                            Text(stringResource(R.string.routine_remove_exercise))
                        }
                    }
                }
            }
        }
    }
    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            title = { Text(stringResource(R.string.routine_remove_exercise_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.routine_remove_exercise_message,
                        exercise?.name ?: stringResource(R.string.routine_this_exercise),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingRemove = false
                        viewModel.removeExercise(item.key)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
                ) { Text(stringResource(R.string.action_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRemove = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Whether sets count reps, time, or a distance (with its time), full width. Only shown while the exercise is open. */
@Composable
private fun TrackSelector(item: DraftExercise, viewModel: PlanEditorViewModel, modifier: Modifier = Modifier) {
    val tracks = Track.entries
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        tracks.forEachIndexed { index, track ->
            SegmentedButton(
                selected = item.track == track,
                onClick = { viewModel.setTrack(item.key, track) },
                shape = SegmentedButtonDefaults.itemShape(index, tracks.size),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    stringResource(
                        when (track) {
                            Track.REPS -> R.string.routine_track_reps
                            Track.TIME -> R.string.routine_track_time
                            Track.DISTANCE -> R.string.routine_track_distance
                        },
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The rest: a main (tonal) button opening the rest sheet while open; closed, the same content
 * read-only, without the button's background.
 */
@Composable
private fun RestControl(item: DraftExercise, editable: Boolean, viewModel: PlanEditorViewModel) {
    var choosingRest by remember { mutableStateOf(false) }
    val content = restContent(item.restSeconds)
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = editable,
        transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.defaultEffectsSpec()) },
        label = "rest",
    ) { open ->
        if (open) {
            FilledTonalButton(onClick = { choosingRest = true }, modifier = Modifier.fillMaxWidth(), content = content)
        } else {
            RestPreview(item.restSeconds)
        }
    }
    if (choosingRest) {
        RestSheet(
            current = item.restSeconds,
            onPick = {
                viewModel.setRest(item.key, it)
                choosingRest = false
            },
            onDismiss = { choosingRest = false },
        )
    }
}


/**
 * Set rows: badge (tap for type) · reps, a rep range or time · weight. Open, the cells are pills
 * to fill in and tapping the Reps title switches to a range (two pills); closed, the same rows
 * show only the values. Added rows grow in and removed ones shrink away, moving what is below.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTable(item: DraftExercise, bodyweight: Boolean, viewModel: PlanEditorViewModel, editable: Boolean) {
    val numbers = item.sets.map { it.set }.setNumbers()
    val motion = MaterialTheme.motionScheme
    // Rows already there when the table appears are shown as they are; only later ones animate in.
    val initialKeys = remember { item.sets.map { it.key }.toSet() }
    Column(Modifier.padding(top = Dimens.SpaceS)) {
        if (item.distance) {
            // A distance and its time; read-only, the pace too, and only the columns with values.
            DistanceTitles(if (editable) DistanceColumns() else distanceColumns(item.sets.map { it.set }))
        } else if (editable && !item.timed) {
            // Open: tapping the Reps title switches between one number and a range.
            Row(
                modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            ) {
                ColumnLabel(stringResource(R.string.routine_col_set), Modifier.width(SetColumnWidth))
                TextButton(
                    onClick = { viewModel.setRepRange(item.key, !item.repRange) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(repsTitle(item.timed, item.repRange)), style = MaterialTheme.typography.labelMedium) }
                ColumnLabel(stringResource(R.string.routine_col_weight), Modifier.weight(1f))
            }
        } else {
            SetTableTitles(item.timed, item.repRange, previewColumns(item.sets.map { it.set }, item.timed))
        }
        item.sets.forEachIndexed { i, draft ->
            key(draft.key) {
                val visibility = remember { MutableTransitionState(draft.key in initialKeys).apply { targetState = true } }
                // Removing first shrinks the row away, then drops it from the routine.
                LaunchedEffect(visibility) {
                    snapshotFlow { visibility.isIdle && !visibility.targetState }.first { it }
                    viewModel.removeSet(item.key, draft.key)
                }
                AnimatedVisibility(
                    visibleState = visibility,
                    enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
                    exit = shrinkVertically(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()),
                ) {
                    SetRow(
                        item = item,
                        draft = draft,
                        number = numbers[i],
                        bodyweight = bodyweight,
                        editable = editable,
                        viewModel = viewModel,
                        onRemove = { visibility.targetState = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    item: DraftExercise,
    draft: DraftSet,
    number: Int?,
    bodyweight: Boolean,
    editable: Boolean,
    viewModel: PlanEditorViewModel,
    onRemove: () -> Unit,
) {
    val set = draft.set
    if (item.distance && !editable) {
        DistancePreviewRow(set, number, distanceColumns(item.sets.map { it.set }))
        return
    }
    if (!editable) {
        SetPreviewRow(set, number, item.timed, item.repRange, bodyweight, previewColumns(item.sets.map { it.set }, item.timed))
        return
    }
    Row(
        modifier = Modifier.padding(top = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        SetTypeButton(
                set = set,
                number = number,
                canRemove = item.sets.size > 1,
                // To failure has no target: you do as many as you can and log what you did.
                onType = { type ->
                    viewModel.updateSet(item.key, draft.key) {
                        if (type == SetType.FAILURE) it.copy(type = type, reps = null, repsMax = null, durationSeconds = null) else it.copy(type = type)
                    }
                },
                onRemove = onRemove,
            )
        val cell = Modifier.weight(1f)
        if (item.distance) {
            DistanceField(set.distanceMeters, { viewModel.setDistance(item.key, draft.key, it) }, cell)
            TimeField(seconds = set.durationSeconds, onPick = { viewModel.setDuration(item.key, draft.key, it) }, modifier = cell)
            return@Row
        }
        when {
            set.type == SetType.FAILURE -> FixedField(stringResource(R.string.set_failure_target), cell)
            item.timed ->
                TimeField(seconds = set.durationSeconds, onPick = { viewModel.setDuration(item.key, draft.key, it) }, modifier = cell)
            item.repRange -> Row(cell, verticalAlignment = Alignment.CenterVertically) {
                WholeNumberField(set.reps?.toString().orEmpty(), { viewModel.setReps(item.key, draft.key, it.toIntOrNull()) }, Modifier.weight(1f))
                RangeDash()
                WholeNumberField(set.repsMax?.toString().orEmpty(), { viewModel.setRepsMax(item.key, draft.key, it.toIntOrNull()) }, Modifier.weight(1f))
            }
            else ->
                WholeNumberField(set.reps?.toString().orEmpty(), { viewModel.setReps(item.key, draft.key, it.toIntOrNull()) }, cell)
        }
        WeightField(
            weightKg = set.weightKg,
            bodyweight = bodyweight,
            onWeight = { viewModel.setWeight(item.key, draft.key, it) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** The set's badge; tapping it opens the set type sheet. */
@Composable
private fun SetTypeButton(set: PlannedSet, number: Int?, canRemove: Boolean, onType: (SetType) -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    SetBadge(set.type, number, onClick = { open = true })
    if (open) {
        SetTypeSheet(
            current = set.type,
            canRemove = canRemove,
            onType = {
                onType(it)
                open = false
            },
            onRemove = {
                onRemove()
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** Every set type with what it is for, as a selectable segmented list; Remove set at the bottom. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetTypeSheet(
    current: SetType,
    canRemove: Boolean,
    onType: (SetType) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.set_type_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            val types = SetType.entries
            types.forEachIndexed { index, type ->
                SegmentedListItem(
                    selected = type == current,
                    onClick = { onType(type) },
                    shapes = selectableSegmentedShapes(index, types.size),
                    leadingContent = { SetBadge(type, number = 1) },
                    trailingContent = if (type == current) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    supportingContent = { Text(setTypeDescription(type)) },
                    content = { Text(setTypeName(type)) },
                )
            }
            if (canRemove) {
                Button(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceL),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Dimens.SpaceS))
                    Text(stringResource(R.string.routine_remove_set))
                }
            }
        }
    }
}

/** A cell that can't be typed into, e.g. "Max" for a to-failure set: the same set cell, disabled. */
@Composable
internal fun FixedField(text: String, modifier: Modifier = Modifier) {
    SetCell(value = text, readOnly = true, enabled = false, modifier = modifier)
}


/**
 * One set cell, as the rows were designed: a filled, fully rounded text field (Material's own
 * TextFieldDefaults.DecorationBox) with the "-" placeholder when there's no target.
 * [readOnly] cells (kg, Max) aren't typed into; [onPress] then reacts to taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetCell(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Number,
    onPress: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    if (onPress != null) {
        // A read-only field doesn't report clicks, so react when it is pressed.
        LaunchedEffect(interaction) {
            interaction.interactions.collect { if (it is PressInteraction.Release) onPress() }
        }
    }
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        interactionSource = interaction,
        modifier = modifier.height(CellHeight),
        decorationBox = { field ->
            TextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = field,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                placeholder = {
                    Text("-", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                shape = CircleShape,
                colors = colors,
                contentPadding = PaddingValues(horizontal = Dimens.SpaceM),
            )
        },
    )
}

/** A timed set's time: read-only "m:ss" ("-" none); tap it for the time sheet. */
@Composable
internal fun TimeField(seconds: Int?, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    SetCell(value = seconds?.let(::formatSeconds).orEmpty(), readOnly = true, onPress = { open = true }, modifier = modifier)
    if (open) {
        SetTimeSheet(
            current = seconds,
            onPick = {
                onPick(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** Reps. Shows the draft's value, so a fill-down from the first row appears here too. */
@Composable
internal fun WholeNumberField(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    SetCell(
        value = value,
        onValueChange = { input -> if (input.length <= 4 && input.all { it.isDigit() }) onValue(input) },
        modifier = modifier,
    )
}

/**
 * The weight cell: read-only, tap it for the weight sheet. Shows the unit with the value: "8 kg",
 * "+8 kg" for bodyweight exercises (the added weight), "BW" for body weight, "-" for no target.
 */
@Composable
internal fun WeightField(
    weightKg: Double?,
    bodyweight: Boolean,
    onWeight: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    SetCell(
        value = weightText(weightKg, bodyweight),
        readOnly = true,
        onPress = { open = true },
        modifier = modifier,
    )
    if (open) {
        WeightSheet(
            current = weightKg,
            onPick = {
                onWeight(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/**
 * Any weight, typed into a standard dialog in the app's unit. Set returns it in kilos; the field
 * opens with the keyboard up.
 */
@Composable
internal fun CustomWeightDialog(
    initialKg: Double?,
    onPick: (Double) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.weight_custom_title),
) {
    val unit = LocalWeightUnit.current
    var text by rememberSaveable { mutableStateOf(initialKg?.takeIf { it > 0 }?.let(unit::number).orEmpty()) }
    val kg = text.toDoubleOrNull()?.takeIf { it > 0 }?.let(unit::toKg)
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val cleaned = input.replace(',', '.')
                    if (cleaned.length <= 6 && cleaned.all { it.isDigit() || it == '.' } && cleaned.count { it == '.' } <= 1) text = cleaned
                },
                label = { Text(weightUnitSymbol(unit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { kg?.let(onPick) }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { kg?.let(onPick) }, enabled = kg != null) { Text(stringResource(R.string.weight_set)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** A weight choice in the sheet: null = no target, 0 = body weight, or kg. */
private sealed interface WeightChoice {
    data class Value(val kg: Double?) : WeightChoice
    data object Custom : WeightChoice
}

/**
 * Picks a set's weight from one selectable list: No target, Body weight, each common weight, and
 * Custom… last (opens a dialog to type any weight). Total weight moved: two 4 kg dumbbells are 8.
 * Plate calculator on top works a weight out from a bar and its plates.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WeightSheet(current: Double?, onPick: (Double?) -> Unit, onDismiss: () -> Unit) {
    val unit = LocalWeightUnit.current
    val presets = unit.presetsKg
    val isCustom = current != null && current != 0.0 && presets.none { sameWeight(it, current) }
    var typing by rememberSaveable { mutableStateOf(false) }
    var calculating by rememberSaveable { mutableStateOf(false) }
    val choices: List<WeightChoice> =
        listOf(WeightChoice.Value(null), WeightChoice.Value(0.0)) + presets.map { WeightChoice.Value(it) } + WeightChoice.Custom
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.weight_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceXs),
            )
            Text(
                stringResource(if (unit == WeightUnit.LB) R.string.weight_hint_lb else R.string.weight_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            // Work the weight out from the bar and its plates.
            FilledTonalButton(
                onClick = { calculating = true },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.SpaceM),
            ) {
                Icon(painterResource(R.drawable.ic_fitness), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Dimens.SpaceS))
                Text(stringResource(R.string.calculator_title))
            }
            if (calculating) {
                PlateCalculatorSheet(
                    onUse = {
                        calculating = false
                        onPick(it)
                    },
                    onDismiss = { calculating = false },
                )
            }
            if (typing) {
                CustomWeightDialog(
                    initialKg = current,
                    onPick = {
                        typing = false
                        onPick(it)
                    },
                    onDismiss = { typing = false },
                )
            }
            choices.forEachIndexed { index, choice ->
                val selected = when (choice) {
                    is WeightChoice.Value -> sameWeight(current, choice.kg)
                    WeightChoice.Custom -> isCustom
                }
                SegmentedListItem(
                    selected = selected,
                    onClick = {
                        when (choice) {
                            is WeightChoice.Value -> onPick(choice.kg)
                            WeightChoice.Custom -> typing = true
                        }
                    },
                    shapes = selectableSegmentedShapes(index, choices.size),
                    trailingContent = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    content = {
                        Text(
                            when (choice) {
                                is WeightChoice.Value -> when (choice.kg) {
                                    null -> stringResource(R.string.weight_no_target)
                                    0.0 -> stringResource(R.string.weight_bodyweight)
                                    else -> weightValue(choice.kg)
                                }
                                // A custom weight already set shows on its row.
                                WeightChoice.Custom ->
                                    if (isCustom) weightValue(current!!) else stringResource(R.string.weight_custom)
                            },
                        )
                    },
                )
            }
        }
    }
}

/** Rest choices, in seconds; 0 = no timer ("Off"). */
internal val RestOptions = listOf(0, 30, 45, 60, 90, 120, 150, 180, 240, 300)

/** Time-per-set choices for timed exercises (plank, cardio), in seconds. */
internal val SetTimeOptions = listOf(10, 15, 20, 30, 45, 60, 90, 120, 180, 300, 600)

/** Picks the rest after each set. */
@Composable
private fun RestSheet(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    DurationSheet(
        title = stringResource(R.string.routine_rest_title),
        customTitle = stringResource(R.string.rest_custom_title),
        options = RestOptions,
        current = current,
        label = { restLabel(it) },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

/** Picks how long a timed set lasts (no "Off": a timed set always has a time once chosen). */
@Composable
internal fun SetTimeSheet(current: Int?, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    DurationSheet(
        title = stringResource(R.string.set_time_title),
        customTitle = stringResource(R.string.set_time_custom_title),
        options = SetTimeOptions,
        current = current,
        label = { formatSeconds(it) },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

/**
 * A duration as a selectable segmented list of [options] (shown with [label]), then Custom…,
 * which opens Material's time picker. A [current] value outside [options] stays pickable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DurationSheet(
    title: String,
    customTitle: String,
    options: List<Int>,
    current: Int?,
    label: @Composable (Int) -> String,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var typingCustom by rememberSaveable { mutableStateOf(false) }
    val all = (options + listOfNotNull(current)).distinct().sorted()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = Dimens.SpaceM))
            all.forEachIndexed { index, seconds ->
                SegmentedListItem(
                    selected = seconds == current,
                    onClick = { onPick(seconds) },
                    shapes = selectableSegmentedShapes(index, all.size + 1),
                    trailingContent = if (seconds == current) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    content = { Text(label(seconds)) },
                )
            }
            SegmentedListItem(
                onClick = { typingCustom = true },
                shapes = selectableSegmentedShapes(all.size, all.size + 1),
                content = { Text(stringResource(R.string.rest_custom)) },
            )
        }
    }
    if (typingCustom) {
        CustomDurationDialog(
            title = customTitle,
            initialSeconds = current ?: 0,
            onPick = {
                typingCustom = false
                onPick(it)
            },
            onDismiss = { typingCustom = false },
        )
    }
}

/**
 * Any duration: Material's TimePickerDialog holding [DurationInput], Material's TimeInput copied
 * to take minutes (0–99) and seconds instead of a time of day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomDurationDialog(title: String, initialSeconds: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDurationInputState(initialSeconds)
    val total = state.totalSeconds
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = { total?.let(onPick) }, enabled = total != null && total > 0) {
                Text(stringResource(R.string.weight_set))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DurationInput(state = state)
    }
}
