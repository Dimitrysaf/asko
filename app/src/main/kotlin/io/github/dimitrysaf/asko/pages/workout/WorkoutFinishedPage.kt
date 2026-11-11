package io.github.dimitrysaf.asko.pages.workout

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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
 * After Finish: a celebration, then the workout in numbers. An expressive shape springs in and
 * keeps turning behind a check that pops in (with a buzz), "Workout done!" and the routine rise in,
 * then time, sets, reps and volume count up in their cards one after another, and the exercises
 * done follow with their sets. Done goes back; See in History opens the day.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutFinishedPage(workoutId: Long, routineName: String, onDone: () -> Unit, onHistory: (day: LocalDate) -> Unit) {
    val viewModel = appViewModel { WorkoutViewModel(workoutId, it.workoutRepository, it.exerciseRepository) }
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    CelebrationBuzz()

    Scaffold(
        // Only the workout's notes up here, at the right.
        topBar = {
            TopAppBar(
                title = {},
                actions = { SubjectNotes(NoteSubject.WORKOUT, workoutId.toString(), Modifier.padding(end = Dimens.SpaceS)) },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Dimens.PageGutter, vertical = Dimens.SpaceS),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                ) {
                    FilledTonalButton(
                        onClick = { workout?.let { onHistory(it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()) } },
                        enabled = workout != null,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) { Text(stringResource(R.string.finished_history)) }
                    Button(
                        onClick = onDone,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) { Text(stringResource(R.string.finished_done)) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "hero") { CelebrationHero(Modifier.padding(top = Dimens.SpaceL)) }
            item(key = "title") {
                Appear(delayMs = 350) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                        Text(
                            stringResource(R.string.finished_title),
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            listOfNotNull(routineName.ifBlank { null }, workout?.startedAt?.formatDate()).joinToString(" · "),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            workout?.let { done ->
                item(key = "stats") { Stats(done) }
                item(key = "exercises") {
                    Appear(delayMs = 1100) { ExercisesDone(done, viewModel) }
                }
            }
        }
    }
}

/**
 * Two of Material's expressive shapes, turning slowly in opposite directions, spring in with a
 * bounce; the check pops in a moment later.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CelebrationHero(modifier: Modifier = Modifier) {
    val grow = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        grow.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(250)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val turning = rememberInfiniteTransition(label = "turning")
    val turn by turning.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(HeroSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(HeroSize)
                .graphicsLayer {
                    scaleX = grow.value
                    scaleY = grow.value
                    rotationZ = -turn
                }
                .background(colors.tertiaryContainer, MaterialShapes.SoftBurst.toShape()),
        )
        Box(
            Modifier
                .size(HeroSize * 0.78f)
                .graphicsLayer {
                    scaleX = grow.value
                    scaleY = grow.value
                    rotationZ = turn * 1.5f
                }
                .background(colors.primary, MaterialShapes.Cookie12Sided.toShape()),
        )
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(HeroSize * 0.4f).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        )
    }
}

private val HeroSize = 200.dp

/** Time, sets, reps and volume, each in a card that rises in after the one before and counts up. */
@Composable
private fun Stats(workout: Workout) {
    val sets = workout.exercises.flatMap { it.sets }
    val reps = sets.sumOf { it.reps ?: 0 }
    val volume = sets.sumOf { (it.reps ?: 0) * (it.weightKg ?: 0.0) }.toInt()
    val seconds = workout.duration?.seconds?.toInt()
    val number = NumberFormat.getIntegerInstance()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            StatCard(stringResource(R.string.finished_time), seconds, 500, Modifier.weight(1f)) { it?.let(::formatSeconds) ?: "-" }
            StatCard(stringResource(R.string.finished_sets), workout.setCount, 650, Modifier.weight(1f)) { number.format(it ?: 0) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            StatCard(stringResource(R.string.finished_reps), reps, 800, Modifier.weight(1f)) { number.format(it ?: 0) }
            val kg = stringResource(R.string.finished_volume_value)
            StatCard(stringResource(R.string.finished_volume), volume, 950, Modifier.weight(1f)) { kg.format(number.format(it ?: 0)) }
        }
    }
}

/** A number that counts up from 0 once its card has risen in. */
@Composable
private fun StatCard(label: String, value: Int?, delayMs: Int, modifier: Modifier, show: (Int?) -> String) {
    Box(modifier) {
        Appear(delayMs) {
            val counting = remember { Animatable(0f) }
            LaunchedEffect(value) {
                delay(150)
                counting.animateTo((value ?: 0).toFloat(), tween(durationMillis = 900, easing = FastOutSlowInEasing))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        show(value?.let { counting.value.toInt() }),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** The exercises done, as a segmented list: picture, name, and the sets as done. */
@Composable
private fun ExercisesDone(workout: Workout, viewModel: WorkoutViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.finished_exercises),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = Dimens.SpaceM),
        )
        LoggedWorkoutList(workout, viewModel.library)
    }
}

/** Fades in and rises a little, after [delayMs]: for the parts arriving one after another. */
@Composable
private fun Appear(delayMs: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(durationMillis = 400, delayMillis = delayMs)) +
            slideInVertically(tween(durationMillis = 500, delayMillis = delayMs, easing = FastOutSlowInEasing)) { it / 3 },
    ) { content() }
}

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
