package io.github.dimitrysaf.asko.pages.workout

import io.github.dimitrysaf.asko.components.WithTooltip
import io.github.dimitrysaf.asko.lib.format.LocalDistanceUnit
import io.github.dimitrysaf.asko.lib.format.distanceValueRes

import io.github.dimitrysaf.asko.lib.format.WeightUnit
import io.github.dimitrysaf.asko.lib.format.distanceNumber
import io.github.dimitrysaf.asko.components.Shrug
import io.github.dimitrysaf.asko.data.model.nothingDone
import io.github.dimitrysaf.asko.AskoApplication
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import io.github.dimitrysaf.asko.components.celebrationShimmer
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Shape
import io.github.dimitrysaf.asko.components.PinnedPillBar
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.pages.routines.ExercisePlanList
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.dimitrysaf.asko.components.pageContentPadding
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import androidx.compose.ui.platform.LocalResources
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.lib.format.formatDate
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId

/**
 * After Finish, in the streak item's design: a segmented list headed by "Workout done!" (a check
 * springing in at the left, with a buzz, and the streak's shimmer behind), then the numbers (time,
 * sets, reps, volume) fading in and counting up; when the workout went differently from its
 * routine, a card to update the routine; then the exercises done as in History (sets,
 * supersets, notes). Top right: See in History and the workout's notes;
 * Done is pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutFinishedPage(
    workoutId: Long,
    routineName: String,
    onDone: () -> Unit,
    onHistory: (day: LocalDate) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    val viewModel = appViewModel { WorkoutViewModel(workoutId, it.workoutRepository, it.exerciseRepository) }
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    CelebrationBuzz()
    // The routine as this workout went, when it went differently (see ActiveWorkoutViewModel.finish).
    val container = (LocalContext.current.applicationContext as AskoApplication).container
    val update = remember(workoutId) { container.workoutSession.routineUpdate?.takeIf { it.workoutId == workoutId } }
    var routineUpdated by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    WithTooltip(stringResource(R.string.finished_history)) {
                        IconButton(
                            onClick = { workout?.let { onHistory(it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()) } },
                            enabled = workout != null,
                        ) { Icon(painterResource(R.drawable.ic_history), stringResource(R.string.finished_history)) }
                    }
                    SubjectNotes(NoteSubject.WORKOUT, workoutId.toString())
                },
            )
        },
        bottomBar = { PinnedPillBar(Icons.Filled.Check, stringResource(R.string.finished_done), onClick = onDone) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // One segmented list, as the streak's: "Workout done!" first, then the numbers.
            item(key = "summary") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    DoneItem(routineName, workout, segmentedShapes(0, summaryCount(workout)).shape)
                    workout?.let { done -> Appear { WorkoutStats(done, first = 1, count = summaryCount(done)) } }
                }
            }
            if (update != null) {
                item(key = "routine-update") {
                    Appear {
                        RoutineUpdateCard(update, viewModel.library, routineUpdated) {
                            scope.launch {
                                container.routineRepository.save(update.after)
                                routineUpdated = true
                            }
                        }
                    }
                }
            }
            workout?.let { done ->
                item(key = "exercises") {
                    // The same read-only preview as History and the routine page.
                    val rows = done.exercises.mapNotNull { planned -> viewModel.library[planned.exerciseId]?.let { it to planned } }
                    Appear { ExercisePlanList(rows, onExerciseClick) }
                }
            }
        }
    }
}

/**
 * "Workout done!" in the streak item's design: a check in the app's cookie shape at the left
 * (springing in once, with a little bounce), the title and the routine on two lines, and the
 * streak's slow shimmer behind it, in the check's blues.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DoneItem(routineName: String, workout: Workout?, shape: Shape) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val colors = MaterialTheme.colorScheme
    Surface(shape = shape, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            // The check's own colours (the fire's orange belongs to the streak).
            Modifier.celebrationShimmer(first = colors.primary, second = colors.primaryContainer)
                .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            Box(
                Modifier
                    .size(HeroSize)
                    .graphicsLayer {
                        scaleX = grow.value
                        scaleY = grow.value
                    }
                    .background(colors.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                // Nothing done (every set logged empty): a shrug instead of the check.
                if (workout?.nothingDone == true) {
                    Shrug(HeroSize * 0.6f, description = null)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(HeroSize * 0.55f))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (workout?.nothingDone == true) R.string.finished_nothing_title else R.string.finished_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOfNotNull(routineName.ifBlank { null }, workout?.startedAt?.formatDate()).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

private val HeroSize = 44.dp

/**
 * Time, sets, reps and volume, as a segmented list: each row rises in after the one before and its
 * number counts up.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WorkoutStats(workout: Workout, first: Int, count: Int, animate: Boolean = true) {
    val sets = workout.exercises.flatMap { it.sets }
    val reps = sets.sumOf { it.reps ?: 0 }
    val unit = LocalWeightUnit.current
    val volume = unit.fromKg(sets.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }).toInt()
    val seconds = workout.duration?.seconds?.toInt()
    val number = NumberFormat.getIntegerInstance()
    val resources = LocalResources.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        StatRow(stringResource(R.string.finished_time), seconds, first, count, animate) { it?.let(::formatSeconds).orEmpty() }
        StatRow(stringResource(R.string.finished_sets), workout.setCount, first + 1, count, animate) { number.format(it ?: 0) }
        StatRow(stringResource(R.string.finished_reps), reps, first + 2, count, animate) { number.format(it ?: 0) }
        StatRow(stringResource(R.string.finished_volume), volume, first + 3, count, animate) { resources.weightValue(number.format(it ?: 0), unit) }
        // With distance sets: how far, in all (counting up in metres).
        val meters = workout.distanceMeters
        val distanceUnit = LocalDistanceUnit.current
        if (meters > 0) {
            StatRow(stringResource(R.string.finished_distance), meters.toInt(), first + 4, count, animate) {
                resources.getString(distanceValueRes(distanceUnit), distanceUnit.distanceNumber((it ?: 0).toDouble()))
            }
        }
    }
}

/** One number of the workout, in its place ([index]) of the segmented list, counting up from 0. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatRow(label: String, value: Int?, index: Int, count: Int, animate: Boolean, show: (Int?) -> String) {
    val counting = remember { Animatable(if (animate) 0f else (value ?: 0).toFloat()) }
    LaunchedEffect(value) {
        counting.animateTo((value ?: 0).toFloat(), tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    ListItem(
        modifier = Modifier.clip(segmentedShapes(index, count).shape),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                show(value?.let { counting.value.toInt() }),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        },
    )
}

/** The summary list: Workout done, then time, sets, reps and volume. */
/** Metres covered in all, over its distance sets. */
private val Workout.distanceMeters: Double get() = exercises.filter { it.distance }.sumOf { e -> e.sets.sumOf { it.distanceMeters ?: 0.0 } }

/** Rows in the summary list: "Workout done!", time, sets, reps, volume, and distance when there is any. */
private fun summaryCount(workout: Workout?): Int = 1 + workoutStatCount(workout)

/** The numbers [WorkoutStats] lists: time, sets, reps, volume, and distance when there is any. */
internal fun workoutStatCount(workout: Workout?): Int = if ((workout?.distanceMeters ?: 0.0) > 0) 5 else 4

/** Fades in as the screen opens (everything below the check at once). */
@Composable
private fun Appear(content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = state, enter = fadeIn(tween(durationMillis = 400, delayMillis = APPEAR_DELAY_MS))) { content() }
}

/** The rest fades in once the check has sprung in. */
private const val APPEAR_DELAY_MS = 250

/** A short buzz as the screen opens: a rise then a click where the phone can, a heavy click otherwise. */
@Composable
private fun CelebrationBuzz() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return@LaunchedEffect
        val rise = VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
        val click = VibrationEffect.Composition.PRIMITIVE_CLICK
        val effect = if (vibrator.areAllPrimitivesSupported(rise, click)) {
            VibrationEffect.startComposition().addPrimitive(rise).addPrimitive(click).compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator.vibrate(effect)
    }
}
