package io.github.dimitrysaf.asko.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.DayMark
import io.github.dimitrysaf.asko.data.model.Streak
import java.time.LocalDate

/**
 * The streak in its own item, compact: a flickering flame at the left as tall as both lines, then
 * "12 day streak" (the number rolling up to it) and a line to keep going under it; while the
 * streak is alive, the loading skeleton's shimmer sweeps slowly across it, in blue and orange.
 */
@Composable
fun StreakHero(streak: Streak, shape: Shape, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val today = remember { LocalDate.now() }
    val doneToday = streak.counts(today)
    val workedToday = streak.markOf(today) == DayMark.WORKOUT
    val alive = streak.current > 0
    // Tapping it explains the streak (see StreakDialog).
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        // While the streak is alive, the skeleton's shimmer sweeps across it, faint, in blue and
        // the fire's orange, slower than when loading.
        Box(
            if (alive) {
                Modifier.celebrationShimmer()
            } else {
                Modifier
            },
        ) {
            Row(
                Modifier.padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                // The flame at the left, as tall as both lines.
                Flame(alive = alive, lit = doneToday)
                Column(Modifier.weight(1f)) {
                    // The days count up to the streak, once when it shows and again when it grows.
                    val count = remember { Animatable(0f) }
                    LaunchedEffect(streak.current) {
                        count.animateTo(streak.current.toFloat(), tween(COUNT_MS, easing = FastOutSlowInEasing))
                    }
                    val shown = count.value.toInt()
                    Text(pluralStringResource(R.plurals.streak_days, shown, shown), style = MaterialTheme.typography.titleMedium)
                    AnimatedContent(
                        targetState = message(streak.current, doneToday, workedToday, restToday = streak.isRestDay(today)),
                        transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
                        label = "streak message",
                    ) { text ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The streak item while the streak is worked out: the same shape and size (the flame's circle, the
 * count and a two-line message), as skeletons, so nothing below it moves when it arrives.
 */
@Composable
fun StreakHeroSkeleton(shape: Shape, modifier: Modifier = Modifier) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            SkeletonBox(Modifier.size(FlameBox), CircleShape)
            Column(Modifier.weight(1f)) {
                // Invisible text in the real styles keeps the real heights; skeletons cover it.
                Text(" ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(0.4f).skeleton(MaterialTheme.shapes.extraSmall))
                Text(
                    " ",
                    style = MaterialTheme.typography.bodyMedium,
                    minLines = 2,
                    modifier = Modifier.padding(top = Dimens.SpaceXs).fillMaxWidth(0.9f).skeleton(MaterialTheme.shapes.extraSmall),
                )
            }
        }
    }
}

/**
 * A celebrating item's background: the loading skeleton's shimmer, faint, in [first] then
 * [second], three times slower. The streak's is the theme's blue and the fire's orange.
 */
@Composable
fun Modifier.celebrationShimmer(
    first: Color = MaterialTheme.colorScheme.primary,
    second: Color = dayMarkColor(DayMark.WORKOUT),
): Modifier = shimmer(
    colors = listOf(first.copy(alpha = SHIMMER_ALPHA), second.copy(alpha = SHIMMER_ALPHA)),
    durationMillis = SKELETON_SWEEP_MS * SHIMMER_SLOWDOWN,
)

/**
 * The fire: flickering (a gentle pulse and sway) while the streak is alive, bigger with a glow
 * behind it once today counts; dim and still when there's no streak.
 */
@Composable
private fun Flame(alive: Boolean, lit: Boolean) {
    val flicker = rememberInfiniteTransition(label = "flame")
    val pulse by flicker.animateFloat(
        initialValue = 1f,
        targetValue = if (lit) 1.14f else 1.06f,
        animationSpec = infiniteRepeatable(tween(if (lit) 650 else 1_100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val sway by flicker.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val glow by flicker.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    Box(Modifier.size(FlameBox), contentAlignment = Alignment.Center) {
        if (alive && lit) {
            // Read while drawing (in the layer), so the animation redraws without recomposing.
            Box(
                Modifier
                    .size(FlameBox)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = glow
                    }
                    .background(dayMarkColor(DayMark.WORKOUT), CircleShape),
            )
        }
        DayMarkIcon(
            DayMark.WORKOUT,
            size = FlameSize,
            alpha = if (alive) 1f else 0.35f,
            modifier = if (alive) {
                Modifier.graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    rotationZ = sway
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) // sways from its base
                }
            } else {
                Modifier
            },
        )
    }
}

/**
 * A line to keep going: start one, keep it alive today, or well done for today. On a rest day
 * ([restToday]) it says so: resting keeps the streak going, and a workout anyway is a bonus.
 */
@Composable
private fun message(current: Int, doneToday: Boolean, workedToday: Boolean, restToday: Boolean): String = when {
    restToday && workedToday -> stringResource(R.string.streak_msg_rest_worked)
    restToday && current == 0 -> stringResource(R.string.streak_msg_rest_no_streak)
    restToday -> stringResource(R.string.streak_msg_rest)
    current == 0 -> stringResource(R.string.streak_msg_start)
    doneToday && workedToday && current >= 7 && current % 7 == 0 ->
        pluralStringResource(R.plurals.streak_msg_weeks, current / 7, current / 7)
    doneToday && workedToday -> stringResource(R.string.streak_msg_done)
    else -> stringResource(R.string.streak_msg_keep_alive)
}

private val FlameBox = 44.dp
private val FlameSize = 34.dp
private const val COUNT_MS = 900
private const val FADE_MS = 300
private const val SHIMMER_ALPHA = 0.22f

/** The shimmer crosses this many times slower than the loading skeleton's. */
private const val SHIMMER_SLOWDOWN = 3
