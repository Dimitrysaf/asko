package io.github.dimitrysaf.asko.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Loading placeholder: a muted block in [shape] with a light band sweeping across it (shimmer).
 * Put it where content will appear, at the content's size, so nothing jumps when it arrives.
 */
@Composable
fun Modifier.skeleton(shape: Shape = MaterialTheme.shapes.small): Modifier {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sweep = rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_200, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton sweep",
    )
    // The sweep is read while drawing, so the wave redraws without recomposing.
    return clip(shape).drawBehind {
        drawRect(base)
        val band = size.width * 0.6f
        // From fully off the left edge to fully off the right edge, slightly diagonal.
        val x = -band + (size.width + 2 * band) * sweep.value
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(x - band, 0f),
                end = Offset(x + band, size.height),
            ),
        )
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    Box(modifier.skeleton(shape))
}

/** Stand-in for a line of text: [widthFraction] of the available width. */
@Composable
fun SkeletonLine(widthFraction: Float, modifier: Modifier = Modifier, height: Dp = 16.dp) {
    SkeletonBox(modifier.fillMaxWidth(widthFraction).height(height), MaterialTheme.shapes.extraSmall)
}
