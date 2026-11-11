package io.github.dimitrysaf.asko.pages.workout

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
import androidx.compose.material3.TopAppBar
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
 * sets, reps, volume) fading in and counting up; then the exercises done as in History (sets,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(
                        onClick = { workout?.let { onHistory(it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()) } },
                        enabled = workout != null,
                    ) { Icon(painterResource(R.drawable.ic_history), stringResource(R.string.finished_history)) }
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
                    DoneItem(routineName, workout, segmentedShapes(0, SUMMARY_COUNT).shape)
                    workout?.let { done -> Appear { Stats(done) } }
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
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(HeroSize * 0.55f))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.finished_title), style = MaterialTheme.typography.titleMedium)
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
private fun Stats(workout: Workout) {
    val sets = workout.exercises.flatMap { it.sets }
    val reps = sets.sumOf { it.reps ?: 0 }
    val volume = sets.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }.toInt()
    val seconds = workout.duration?.seconds?.toInt()
    val number = NumberFormat.getIntegerInstance()
    val kg = stringResource(R.string.finished_volume_value)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        StatRow(stringResource(R.string.finished_time), seconds, 1) { it?.let(::formatSeconds).orEmpty() }
        StatRow(stringResource(R.string.finished_sets), workout.setCount, 2) { number.format(it ?: 0) }
        StatRow(stringResource(R.string.finished_reps), reps, 3) { number.format(it ?: 0) }
        StatRow(stringResource(R.string.finished_volume), volume, 4) { kg.format(number.format(it ?: 0)) }
    }
}

/** One number of the workout, in its place ([index]) of the segmented list, counting up from 0. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatRow(label: String, value: Int?, index: Int, show: (Int?) -> String) {
    val counting = remember { Animatable(0f) }
    LaunchedEffect(value) {
        counting.animateTo((value ?: 0).toFloat(), tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    ListItem(
        modifier = Modifier.clip(segmentedShapes(index, SUMMARY_COUNT).shape),
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
private const val SUMMARY_COUNT = 5

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
