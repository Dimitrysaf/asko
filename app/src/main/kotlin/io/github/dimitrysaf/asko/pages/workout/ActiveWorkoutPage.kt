package io.github.dimitrysaf.asko.pages.workout

import io.github.dimitrysaf.asko.components.ExerciseDemo
import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.AskoApplication
import io.github.dimitrysaf.asko.lib.notify.WorkoutService
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.pm.PackageManager
import android.Manifest
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.pages.routines.SupersetTitle
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.components.RemoteImage
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.sharpenEnlarged
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.exercisedb.ExerciseDbApi
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.pages.routines.ColumnLabel
import io.github.dimitrysaf.asko.pages.routines.SetBadge
import io.github.dimitrysaf.asko.pages.routines.SetColumnWidth
import io.github.dimitrysaf.asko.pages.routines.TimeField
import io.github.dimitrysaf.asko.pages.routines.WeightField
import io.github.dimitrysaf.asko.pages.routines.WeightSheet
import io.github.dimitrysaf.asko.pages.routines.WholeNumberField
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import io.github.dimitrysaf.asko.pages.routines.isBodyweight
import io.github.dimitrysaf.asko.pages.routines.setNumbers
import kotlin.math.ceil

private val MediaMaxWidth = 480.dp

/**
 * Doing a routine. On top Exit, the routine and the workout clock (red while paused), and Pause.
 * Then a progress bar with a segment per exercise, the exercise's name and demo (ⓘ swaps it for
 * how to do it) and its sets, the current one highlighted: values come from the plan and can be changed, a
 * set's badge opens the ways to log it. While resting a full-screen clock counts down instead;
 * a timed set's clock runs in its row. Pinned at the bottom: Previous, Next and Done / Undone
 * (Finish on the last set); buttons with nothing to do are hidden. The screen stays on.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActiveWorkoutPage(
    routineId: Long,
    onBack: () -> Unit,
    onFinished: (workoutId: Long?, routineName: String) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    val viewModel = appViewModel {
        ActiveWorkoutViewModel(routineId, it.routineRepository, it.workoutRepository, it.exerciseRepository, it.activeWorkoutStore, it.workoutSession)
    }
    var confirmingExit by rememberSaveable { mutableStateOf(false) }
    var confirmingFinish by rememberSaveable { mutableStateOf(false) }
    val finish = { if (viewModel.allDone) viewModel.finish { id -> onFinished(id, viewModel.routineName) } else confirmingFinish = true }
    BackHandler { confirmingExit = true }
    val settings = (LocalContext.current.applicationContext as AskoApplication).container.settings
    val keepScreenOn by settings.keepScreenOn.collectAsStateWithLifecycle()
    val buzz by settings.restBuzz.collectAsStateWithLifecycle()
    if (keepScreenOn) KeepScreenOn()
    VibrateOnAlerts(viewModel, enabled = buzz)
    WorkoutNotification(viewModel)
    val resting = viewModel.countdown?.kind == CountdownKind.REST
    val motion = MaterialTheme.motionScheme

    Scaffold(
        topBar = {
            // Exit on the left, Pause / Resume on the right; it stays during the rest.
            TopAppBar(
                navigationIcon = {
                    ControlIconButton(
                        icon = painterResource(R.drawable.ic_exit),
                        description = stringResource(R.string.workout_exit),
                        onClick = { confirmingExit = true },
                        container = MaterialTheme.colorScheme.errorContainer,
                        content = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = Dimens.SpaceS),
                        large = false,
                    )
                },
                title = {
                    Column(Modifier.padding(horizontal = Dimens.SpaceS)) {
                        Text(viewModel.routineName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        WorkoutClock(viewModel)
                    }
                },
                actions = { PauseButton(viewModel, Modifier.padding(end = Dimens.SpaceS)) },
            )
        },
        bottomBar = { WorkoutControls(viewModel = viewModel, onFinish = finish) },
    ) { padding ->
        // Top padding here; the lists take the bottom one, so they scroll behind the bottom bar.
        Column(Modifier.consumeWindowInsets(padding).padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            val listPadding = PaddingValues(
                start = Dimens.PageGutter,
                end = Dimens.PageGutter,
                top = Dimens.PageGutter,
                bottom = padding.calculateBottomPadding() + Dimens.PageGutter,
            )
            val exercise = viewModel.currentExercise
            // The progress bar stays put; the rest clock takes the place of the exercise below it.
            if (viewModel.loaded) {
                WorkoutProgress(viewModel, Modifier.padding(start = Dimens.PageGutter, end = Dimens.PageGutter, top = Dimens.SpaceS))
            }
            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = resting,
                transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.defaultEffectsSpec()) },
                label = "rest",
            ) { counting ->
                when {
                    counting -> CountdownScreen(viewModel, onExerciseClick, listPadding)
                    !viewModel.loaded || exercise == null -> Column(
                        Modifier.padding(Dimens.PageGutter),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                    ) {
                        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(1f), MaterialTheme.shapes.extraLarge)
                        SkeletonBox(Modifier.fillMaxWidth().height(56.dp))
                    }
                    else -> {
                        val details = viewModel.library[exercise.exerciseId]
                        LazyColumn(
                            contentPadding = listPadding,
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            item(key = "name") {
                                Column(Modifier.fillMaxWidth()) {
                                    // Part of a superset: its title over the name, as in the routine.
                                    if (viewModel.inSuperset) SupersetTitle(start = 0.dp)
                                    // The name, and the exercise's notes at its right ("Seat 4" before you start).
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            details?.name.orEmpty(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        SubjectNotes(NoteSubject.EXERCISE, exercise.exerciseId, tonal = true)
                                    }
                                }
                            }
                            item(key = "media") { ExerciseMedia(details, exercise.key) }
                            item(key = "sets") { SetList(viewModel, exercise, details?.isBodyweight == true) }
                        }
                    }
                }
            }
        }
    }

    if (confirmingExit) {
        AlertDialog(
            onDismissRequest = { confirmingExit = false },
            title = { Text(stringResource(R.string.workout_discard_title)) },
            text = { Text(stringResource(R.string.workout_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingExit = false
                        viewModel.discard(onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.workout_exit)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingExit = false }) { Text(stringResource(R.string.workout_keep_going)) }
            },
        )
    }
    if (confirmingFinish) {
        val left = viewModel.totalSets - viewModel.doneSets
        AlertDialog(
            onDismissRequest = { confirmingFinish = false },
            title = { Text(stringResource(R.string.workout_finish_title)) },
            text = {
                Text(
                    if (viewModel.doneSets == 0) {
                        stringResource(R.string.workout_finish_nothing)
                    } else {
                        pluralStringResource(R.plurals.workout_finish_left, left, left)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingFinish = false
                    viewModel.finish { id -> onFinished(id, viewModel.routineName) }
                }) { Text(stringResource(R.string.workout_finish)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingFinish = false }) { Text(stringResource(R.string.workout_keep_going)) }
            },
        )
    }
}

/** The workout's time, without pauses; red while paused. */
@Composable
private fun WorkoutClock(viewModel: ActiveWorkoutViewModel, style: TextStyle = MaterialTheme.typography.labelLarge) {
    val color by animateColorAsState(
        if (viewModel.paused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "clock",
    )
    Text(formatSeconds((viewModel.elapsedMs / 1000).toInt()), style = style, color = color)
}

/**
 * How far along the workout is: one segment per exercise, filled by its sets logged. The current
 * exercise's segment is in the primary colour, the others in the tertiary.
 */
@Composable
private fun WorkoutProgress(viewModel: ActiveWorkoutViewModel, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        viewModel.items.forEachIndexed { index, exercise ->
            val logged = exercise.sets.count { it.status != SetStatus.PENDING }
            val progress by animateFloatAsState(
                if (exercise.sets.isEmpty()) 0f else logged.toFloat() / exercise.sets.size,
                MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "exercise progress",
            )
            val current = index == viewModel.exerciseIndex
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(if (current) 8.dp else 4.dp).align(Alignment.CenterVertically),
                color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                drawStopIndicator = {},
            )
        }
    }
}

/**
 * The buttons pinned at the bottom, separate and on one line: Previous as an icon button, then
 * Next with its label and Done / Undone.
 * On the last set Done / Undone takes Next's place and Finish follows; once every set is logged
 * Finish expands with its label and Done shrinks to its icon. Buttons with nothing to do are hidden.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkoutControls(viewModel: ActiveWorkoutViewModel, onFinish: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val set = viewModel.currentSet
    val logged = set != null && (set.status == SetStatus.DONE || set.status == SetStatus.FAILED)
    Surface(color = colors.surfaceContainer) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PageGutter, vertical = Dimens.SpaceS),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (viewModel.hasPrevious) {
                ControlIconButton(
                    icon = painterResource(R.drawable.ic_skip_previous),
                    description = stringResource(R.string.workout_previous),
                    onClick = viewModel::previousSet,
                    container = colors.surfaceContainerHighest,
                    content = colors.onSurface,
                )
            }
            val done = DoneButton(
                logged = logged,
                onClick = viewModel::toggleCurrent,
                container = if (logged) colors.surfaceContainerHighest else colors.primary,
                content = if (logged) colors.onSurface else colors.onPrimary,
            )
            val finishing = ControlAction(
                icon = painterResource(R.drawable.ic_flag),
                label = stringResource(R.string.workout_finish),
                onClick = onFinish,
                container = colors.tertiary,
                content = colors.onTertiary,
            )
            when {
                // Resting: Skip ends the rest, in Next's place.
                viewModel.countdown?.kind == CountdownKind.REST -> {
                    ExpandingControlButton(
                        ControlAction(
                            icon = painterResource(R.drawable.ic_skip_next),
                            label = stringResource(R.string.workout_rest_skip),
                            onClick = viewModel::skipCountdown,
                            container = colors.secondaryContainer,
                            content = colors.onSecondaryContainer,
                        ),
                        expanded = true,
                    )
                    ExpandingControlButton(done, expanded = false)
                }
                // Not the last set: Next (with its label), then Done / Undone.
                viewModel.hasNext -> {
                    ExpandingControlButton(
                        ControlAction(
                            icon = painterResource(R.drawable.ic_skip_next),
                            label = stringResource(R.string.workout_next),
                            onClick = viewModel::next,
                            container = colors.surfaceContainerHighest,
                            content = colors.onSurface,
                        ),
                        expanded = true,
                    )
                    ExpandingControlButton(done, expanded = false)
                }
                // The last set: Done / Undone takes Next's place, Finish waits at the end; once every
                // set is logged, Finish takes the room and Done shrinks back to its icon.
                else -> {
                    ExpandingControlButton(done, expanded = !viewModel.allDone)
                    ExpandingControlButton(finishing, expanded = viewModel.allDone)
                }
            }
        }
    }
}

/** What a bottom button shows and does. */
private class ControlAction(val icon: Painter, val label: String, val onClick: () -> Unit, val container: Color, val content: Color)

@Composable
private fun DoneButton(logged: Boolean, onClick: () -> Unit, container: Color, content: Color) = ControlAction(
    icon = if (logged) painterResource(R.drawable.ic_undo) else rememberVectorPainter(Icons.Filled.Check),
    label = stringResource(if (logged) R.string.workout_undone else R.string.workout_done),
    onClick = onClick,
    container = container,
    content = content,
)

/**
 * A bottom button that is either just its icon (the size of the icon buttons) or [expanded]:
 * filling the room left with its label next to the icon. The label slides in and out.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.ExpandingControlButton(action: ControlAction, expanded: Boolean) {
    val size = IconButtonDefaults.mediumContainerSize()
    val motion = MaterialTheme.motionScheme
    Button(
        onClick = action.onClick,
        shapes = ButtonDefaults.shapes(),
        modifier = (if (expanded) Modifier.weight(1f) else Modifier.width(size.width)).height(size.height),
        colors = ButtonDefaults.buttonColors(containerColor = action.container, contentColor = action.content),
        contentPadding = PaddingValues(horizontal = if (expanded) Dimens.SpaceM else 0.dp),
    ) {
        Icon(action.icon, contentDescription = if (expanded) null else action.label, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkHorizontally(motion.defaultSpatialSpec()) + fadeOut(motion.defaultEffectsSpec()),
        ) {
            Row {
                Spacer(Modifier.width(Dimens.SpaceS))
                Text(action.label, maxLines = 1)
            }
        }
    }
}

/** Pause / Resume, in the top bar; Start for a timed set waiting for its clock. */
@Composable
private fun PauseButton(viewModel: ActiveWorkoutViewModel, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val startsClock = viewModel.holdWaiting && !viewModel.paused
    val play = viewModel.paused || startsClock
    ControlIconButton(
        icon = if (play) rememberVectorPainter(Icons.Filled.PlayArrow) else painterResource(R.drawable.ic_pause),
        description = stringResource(
            when {
                viewModel.paused -> R.string.workout_resume
                startsClock -> R.string.workout_start
                else -> R.string.workout_pause
            },
        ),
        onClick = viewModel::togglePause,
        container = if (play) colors.tertiaryContainer else colors.secondaryContainer,
        content = if (play) colors.onTertiaryContainer else colors.onSecondaryContainer,
        modifier = modifier,
        large = false,
    )
}

/**
 * A workout icon button: Material's filled icon button, shape-morphing on press; medium size at
 * the bottom, standard size ([large] false) in the top bar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ControlIconButton(
    icon: Painter,
    description: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    large: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        modifier = if (large) modifier.size(IconButtonDefaults.mediumContainerSize()) else modifier,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
    ) { Icon(icon, contentDescription = description) }
}

/**
 * The exercise's animated demo, large. ⓘ (a toggle) swaps it for how to do it, step by step; the
 * button then shows an image icon to swap back. Opens on the demo for each new exercise.
 */
@Composable
private fun ExerciseMedia(exercise: Exercise?, exerciseKey: Int) {
    var howTo by rememberSaveable(exerciseKey) { mutableStateOf(false) }
    val motion = MaterialTheme.motionScheme
    Box(
        Modifier
            .widthIn(max = MediaMaxWidth)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        AnimatedContent(
            targetState = howTo,
            transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.defaultEffectsSpec()) },
            label = "media",
        ) { showingHowTo ->
            when {
                exercise == null -> SkeletonBox(Modifier.fillMaxSize(), MaterialTheme.shapes.extraLarge)
                showingHowTo -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.SpaceL),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                ) {
                    Text(stringResource(R.string.exercise_how_to), style = MaterialTheme.typography.titleLarge)
                    if (exercise.instructions.isEmpty()) {
                        Text(stringResource(R.string.workout_no_how_to), style = MaterialTheme.typography.bodyLarge)
                    }
                    exercise.instructions.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(48.dp)) // clear of the toggle when scrolled to the end
                }
                else -> ExerciseDemo(exercise, MaterialTheme.shapes.extraLarge, Modifier.fillMaxSize())
            }
        }
        FilledTonalIconToggleButton(
            checked = howTo,
            onCheckedChange = { howTo = it },
            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.SpaceS),
        ) {
            if (howTo) {
                Icon(painterResource(R.drawable.ic_image), stringResource(R.string.workout_show_demo))
            } else {
                Icon(Icons.Filled.Info, stringResource(R.string.workout_show_how_to))
            }
        }
    }
}

/**
 * The rest after a set, laid out like the exercise it replaces: "Rest" where the name is, the clock
 * (Material's circular wavy progress, time left in the middle, − 15 s / + 15 s inside) where the
 * demo is, and what's up next as one segmented list item: the set, when it's the same exercise,
 * or the next exercise's image and name (tap for its page). Skip is in the bottom bar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CountdownScreen(
    viewModel: ActiveWorkoutViewModel,
    onExerciseClick: (exerciseId: String) -> Unit,
    contentPadding: PaddingValues,
) {
    // Keeps the last rest on screen while it fades out.
    var shown by remember { mutableStateOf(viewModel.countdown) }
    viewModel.countdown?.takeIf { it.kind == CountdownKind.REST }?.let { shown = it }
    val countdown = shown ?: return
    val timeColor by animateColorAsState(
        if (viewModel.paused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "time",
    )
    // The wave is drawn for a small indicator; at clock size it needs a thicker line and a longer
    // wavelength, or it looks jagged.
    val stroke = with(LocalDensity.current) { Stroke(width = ClockStroke.toPx(), cap = StrokeCap.Round) }
    val next = viewModel.currentExercise?.let { viewModel.library[it.exerciseId] }
    // The countdown moves on every tick (5 times a second); the ring glides from one to the next
    // at the same pace, so it drains smoothly instead of in steps.
    val ring by animateFloatAsState(countdown.progress, tween(TickMs, easing = LinearEasing), label = "rest ring")
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "title") {
            Text(
                stringResource(R.string.workout_rest),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "clock") {
            Box(
                Modifier.widthIn(max = MediaMaxWidth).fillMaxWidth().aspectRatio(1f).padding(Dimens.SpaceL),
                contentAlignment = Alignment.Center,
            ) {
                CircularWavyProgressIndicator(
                    progress = { ring },
                    modifier = Modifier.fillMaxSize(),
                    stroke = stroke,
                    trackStroke = stroke,
                    wavelength = ClockWavelength,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                    Text(
                        formatSeconds(ceil(countdown.remainingMs / 1000.0).toInt()),
                        style = MaterialTheme.typography.displayLarge,
                        color = timeColor,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS), verticalAlignment = Alignment.CenterVertically) {
                        RestAdjustButton(painterResource(R.drawable.ic_remove), stringResource(R.string.workout_less_time)) {
                            viewModel.adjustRest(-RestStepSeconds)
                        }
                        RestAdjustButton(rememberVectorPainter(Icons.Filled.Add), stringResource(R.string.workout_more_time)) {
                            viewModel.adjustRest(RestStepSeconds)
                        }
                    }
                }
            }
        }
        val set = viewModel.currentSet
        when {
            // Another set of the same exercise: just which set.
            viewModel.restWithinExercise && set != null -> item(key = "next") {
                val number = viewModel.currentExercise?.sets?.map { it.target }?.setNumbers()?.getOrNull(viewModel.setIndex)
                // Nothing to open, so a plain list item in the same segmented shape.
                ListItem(
                    modifier = Modifier.clip(segmentedShapes(index = 0, count = 1).shape),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    leadingContent = { SetBadge(set.target.type, number) },
                    overlineContent = { Text(stringResource(R.string.workout_up_next)) },
                    headlineContent = { Text(stringResource(R.string.workout_set_number, viewModel.setIndex + 1)) },
                )
            }
            // A new exercise: its image and name (tap for its page).
            next != null -> item(key = "next") {
                SegmentedListItem(
                    onClick = { onExerciseClick(next.id) },
                    shapes = segmentedShapes(index = 0, count = 1),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    leadingContent = { ExerciseThumbnail(next, Modifier.size(Dimens.ExerciseThumbnailSize)) },
                    overlineContent = { Text(stringResource(R.string.workout_up_next)) },
                    content = { Text(next.name) },
                )
            }
        }
    }
}

/** − 15 s / + 15 s inside the rest clock: Material's tonal button with the icon and the amount. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestAdjustButton(icon: Painter, description: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.SmallContentPadding,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Dimens.SpaceXs))
        Text(stringResource(R.string.workout_seconds, RestStepSeconds))
    }
}

private const val RestStepSeconds = 15
private val ClockStroke = 8.dp
private val ClockWavelength = 40.dp

/**
 * The current exercise's sets: badge (tap for the ways to log it) · reps or time · weight.
 * Values start as planned and can be changed; the current set is highlighted (tap a row to make it
 * current). Logged sets are grayed with ✓ (✕ when failed) over the middle; skipped ones are grayed.
 * While a timed set's clock runs, its row's background drains with the time and its time counts
 * down.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetList(viewModel: ActiveWorkoutViewModel, exercise: PlayerExercise, bodyweight: Boolean) {
    val numbers = exercise.sets.map { it.target }.setNumbers()
    var sheetFor by remember { mutableStateOf<Int?>(null) }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Dimens.SpaceS).heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        ) {
            ColumnLabel(stringResource(R.string.routine_col_set), Modifier.width(SetColumnWidth))
            ColumnLabel(stringResource(if (exercise.timed) R.string.routine_col_time else R.string.routine_col_reps), Modifier.weight(1f))
            ColumnLabel(stringResource(R.string.routine_col_weight), Modifier.weight(1f))
        }
        exercise.sets.forEachIndexed { i, set ->
            val current = i == viewModel.setIndex
            val hold = viewModel.countdown?.takeIf { current && it.kind == CountdownKind.SET }
            val highlight by animateColorAsState(
                if (current) colors.secondaryContainer else Color.Transparent,
                MaterialTheme.motionScheme.defaultEffectsSpec(),
                label = "current set",
            )
            val holdLeft by animateFloatAsState(hold?.progress ?: 0f, tween(TickMs, easing = LinearEasing), label = "hold")
            val logged = set.status == SetStatus.DONE || set.status == SetStatus.FAILED
            Surface(
                onClick = { viewModel.goTo(exercise.key, set.key) },
                shape = CircleShape,
                color = highlight,
                modifier = Modifier.padding(top = Dimens.SpaceXs),
            ) {
                Box(
                    Modifier.drawBehind {
                        // The time left of a running timed set, draining from the end.
                        if (hold != null) drawRect(colors.tertiaryContainer, size = size.copy(width = size.width * holdLeft))
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier
                            .padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs)
                            .alpha(if (logged || set.status == SetStatus.SKIPPED) 0.38f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                    ) {
                        SetBadge(set.target.type, numbers[i], onClick = { sheetFor = set.key })
                        val cell = Modifier.weight(1f)
                        if (exercise.timed) {
                            val seconds = hold?.let { ceil(it.remainingMs / 1000.0).toInt() } ?: set.actual.durationSeconds
                            TimeField(seconds, { viewModel.setDuration(exercise.key, set.key, it) }, cell)
                        } else {
                            WholeNumberField(set.actual.reps?.toString().orEmpty(), { viewModel.setReps(exercise.key, set.key, it.toIntOrNull()) }, cell)
                        }
                        WeightField(set.actual.weightKg, bodyweight, { viewModel.setWeight(exercise.key, set.key, it) }, Modifier.weight(1f))
                    }
                    when (set.status) {
                        SetStatus.DONE -> Icon(
                            Icons.Filled.Check,
                            stringResource(R.string.workout_state_done),
                            tint = colors.primary,
                            modifier = Modifier.size(LoggedIconSize),
                        )
                        SetStatus.FAILED -> Icon(
                            Icons.Filled.Close,
                            stringResource(R.string.workout_state_failed),
                            tint = colors.error,
                            modifier = Modifier.size(LoggedIconSize),
                        )
                        else -> {}
                    }
                }
            }
        }
    }
    val set = exercise.sets.firstOrNull { it.key == sheetFor }
    if (set != null) {
        LogSetSheet(
            set = set,
            number = numbers[exercise.sets.indexOf(set)],
            timed = exercise.timed,
            canRemove = exercise.sets.size > 1,
            viewModel = viewModel,
            exerciseKey = exercise.key,
            onDismiss = { sheetFor = null },
        )
    }
}

private val LoggedIconSize = 32.dp

/** The view model's clock ticks this often; the hold background animates over each tick. */
private const val TickMs = 200

/**
 * The ways to log a set: done as planned, fewer (or more) reps, failed it (with the reps you got),
 * a different weight (later sets follow), skip it, add another like it, remove it. (A set's type is
 * set in the routine, not here.)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogSetSheet(
    set: PlayerSet,
    number: Int?,
    timed: Boolean,
    canRemove: Boolean,
    viewModel: ActiveWorkoutViewModel,
    exerciseKey: Int,
    onDismiss: () -> Unit,
) {
    var typingReps by rememberSaveable { mutableStateOf(false) }
    var failing by rememberSaveable { mutableStateOf(false) }
    var choosingWeight by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Row(
                Modifier.padding(bottom = Dimens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                SetBadge(set.target.type, number)
                Text(stringResource(R.string.workout_log_set_title), style = MaterialTheme.typography.titleLarge)
            }
            val actions = buildList {
                add(LogAction(R.string.workout_log_as_planned, R.string.workout_log_as_planned_hint) {
                    viewModel.markDone(exerciseKey, set.key)
                    onDismiss()
                })
                if (!timed) add(LogAction(R.string.workout_log_reps, R.string.workout_log_reps_hint) { typingReps = true })
                if (!timed) add(LogAction(R.string.workout_log_failed, R.string.workout_log_failed_hint) { failing = true })
                add(LogAction(R.string.workout_log_weight, R.string.workout_log_weight_hint) { choosingWeight = true })
                add(LogAction(R.string.workout_log_skip, R.string.workout_log_skip_hint) {
                    viewModel.skip(exerciseKey, set.key)
                    onDismiss()
                })
                add(LogAction(R.string.workout_log_add, R.string.workout_log_add_hint) {
                    viewModel.addSetAfter(exerciseKey, set.key)
                    onDismiss()
                })
            }
            actions.forEachIndexed { index, action ->
                SegmentedListItem(
                    onClick = action.onClick,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = actions.size),
                    supportingContent = { Text(stringResource(action.hint)) },
                    content = { Text(stringResource(action.label)) },
                )
            }

            if (canRemove) {
                Button(
                    onClick = {
                        viewModel.removeSet(exerciseKey, set.key)
                        onDismiss()
                    },
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
    if (typingReps) {
        RepsDoneDialog(
            initial = set.actual.reps,
            title = stringResource(R.string.workout_reps_done_title),
            onLog = { reps ->
                viewModel.logReps(exerciseKey, set.key, reps)
                typingReps = false
                onDismiss()
            },
            onDismiss = { typingReps = false },
        )
    }
    if (failing) {
        RepsDoneDialog(
            initial = null,
            title = stringResource(R.string.workout_failed_title),
            allowZero = true,
            onLog = { reps ->
                viewModel.logFailed(exerciseKey, set.key, reps)
                failing = false
                onDismiss()
            },
            onDismiss = { failing = false },
        )
    }
    if (choosingWeight) {
        WeightSheet(
            current = set.actual.weightKg,
            onPick = {
                viewModel.setWeight(exerciseKey, set.key, it)
                choosingWeight = false
                onDismiss()
            },
            onDismiss = { choosingWeight = false },
        )
    }
}

private class LogAction(val label: Int, val hint: Int, val onClick: () -> Unit)

/** How many reps were done, typed into a standard dialog; Log logs the set with them. */
@Composable
private fun RepsDoneDialog(initial: Int?, title: String, onLog: (Int) -> Unit, onDismiss: () -> Unit, allowZero: Boolean = false) {
    var text by rememberSaveable { mutableStateOf(initial?.toString().orEmpty()) }
    val reps = text.toIntOrNull()?.takeIf { allowZero || it > 0 }
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input -> if (input.length <= 4 && input.all { it.isDigit() }) text = input },
                label = { Text(stringResource(R.string.routine_col_reps)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { reps?.let(onLog) }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { reps?.let(onLog) }, enabled = reps != null) { Text(stringResource(R.string.action_log)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Keeps the screen on while the workout is open. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * A buzz when the rest is over or a timed set has been held long enough, while the app is on
 * screen (off screen, the notification buzzes instead).
 */
@Composable
private fun VibrateOnAlerts(viewModel: ActiveWorkoutViewModel, enabled: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val buzzing by rememberUpdatedState(enabled)
    LaunchedEffect(viewModel) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        viewModel.alerts.collect {
            if (buzzing && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            }
        }
    }
}

/**
 * The workout's ongoing notification (see [WorkoutService]): asks once for permission to show it
 * (Android 13+), starts it once the workout is loaded, and tells it when the app is on screen.
 */
@Composable
private fun WorkoutNotification(viewModel: ActiveWorkoutViewModel) {
    val context = LocalContext.current
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(viewModel.loaded) {
        if (viewModel.loaded) WorkoutService.start(context)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setAppVisible(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setAppVisible(false)
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.setAppVisible(true)
        }
    }
}
