package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import io.github.dimitrysaf.asko.data.model.Muscle

/** Which side of the body a figure shows. */
enum class BodySide { FRONT, BACK }

/**
 * wger's muscular system (bundled, see assets/muscles/LICENSE.txt), from the front and/or the back
 * ([sides], side by side): the body, and each muscle in [shade] laid over it in the theme's colours,
 * from the primary container (a little, 0) to the primary colour (most, 1). With [focus], each figure
 * is enlarged to its shaded muscles (for a small round preview of where they are).
 */
@Composable
fun BodyMap(
    shade: Map<Muscle, Float>,
    modifier: Modifier = Modifier,
    sides: List<BodySide> = BodySide.entries,
    focus: Boolean = false,
    description: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.semantics { if (description != null) contentDescription = description }) {
        sides.forEach { side ->
            val shaded = shade.filterKeys { it.front == (side == BodySide.FRONT) }
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clipToBounds(), contentAlignment = Alignment.Center) {
                val box = if (focus) focusOf(shaded.keys) else null
                if (box == null) {
                    // The whole figure, as big as fits.
                    Figure(side, shaded, colors, Modifier.aspectRatio(FIGURE_WIDTH / FIGURE_HEIGHT, matchHeightConstraintsFirst = true))
                } else {
                    // Enlarged so the box fills the space, centred on it.
                    val slot = minOf(maxWidth, maxHeight)
                    val scale = FIGURE_WIDTH / maxOf(box.width, box.height)
                    val boxWidth = maxWidth
                    val boxHeight = maxHeight
                    Box(Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart, unbounded = true)) {
                        Figure(
                            side,
                            shaded,
                            colors,
                            Modifier
                                .requiredSize(slot, slot * (FIGURE_HEIGHT / FIGURE_WIDTH))
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = scale
                                    scaleY = scale
                                    val unit = slot.toPx() / FIGURE_WIDTH
                                    translationX = boxWidth.toPx() / 2 - scale * box.center.x * unit
                                    translationY = boxHeight.toPx() / 2 - scale * box.center.y * unit
                                },
                        )
                    }
                }
            }
        }
    }
}

/** One side: wger's body, then each shaded muscle's image over it, tinted. */
@Composable
private fun Figure(side: BodySide, shade: Map<Muscle, Float>, colors: ColorScheme, modifier: Modifier) {
    Box(modifier) {
        val layer = Modifier.fillMaxWidth()
        AsyncImage(
            model = if (side == BodySide.FRONT) FRONT else BACK,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = layer,
        )
        shade.forEach { (muscle, amount) ->
            AsyncImage(
                model = "$ASSETS/muscle-${muscle.wgerId}.svg",
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                colorFilter = ColorFilter.tint(lerp(colors.primaryContainer, colors.primary, amount.coerceIn(0f, 1f))),
                modifier = layer,
            )
        }
    }
}

/**
 * The part of the figure to enlarge for [muscles] (wger's own coordinates, 200 wide): around
 * them, with room to see where on the body they are, about square; null when there are none.
 */
private fun focusOf(muscles: Set<Muscle>): Rect? {
    val boxes = muscles.mapNotNull { BOUNDS[it] }
    if (boxes.isEmpty()) return null
    val union = boxes.reduce { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
    val side = maxOf(union.width, union.height) + FOCUS_ROOM * 2
    return Rect(union.center.x - side / 2, union.center.y - side / 2, union.center.x + side / 2, union.center.y + side / 2)
}

/** The one side that shows more of [muscles] (the front when even), for a small preview. */
fun bestSideOf(muscles: Set<Muscle>): BodySide =
    if (muscles.count { !it.front } > muscles.count { it.front }) BodySide.BACK else BodySide.FRONT

/** The sides a set of muscles shows on: the front, the back, or both. */
fun sidesOf(muscles: Set<Muscle>): List<BodySide> = when {
    muscles.isNotEmpty() && muscles.all { it.front } -> listOf(BodySide.FRONT)
    muscles.isNotEmpty() && muscles.none { it.front } -> listOf(BodySide.BACK)
    else -> BodySide.entries
}

private const val ASSETS = "file:///android_asset/muscles"
private const val FRONT = "$ASSETS/front.svg"
private const val BACK = "$ASSETS/back.svg"

/** wger's images are 200 wide and 369 tall. */
private const val FIGURE_WIDTH = 200f
private const val FIGURE_HEIGHT = 369f

/** Room around the muscles in a focused figure, in wger's units. */
private const val FOCUS_ROOM = 18f

/** Where each muscle's image is drawn, in wger's units (measured from its SVG). */
private val BOUNDS = mapOf(
    Muscle.BICEPS to Rect(46.3f, 92.7f, 150.5f, 131.6f),
    Muscle.DELTOIDS to Rect(49.4f, 64.9f, 147.9f, 98.8f),
    Muscle.SERRATUS to Rect(60.8f, 97.8f, 133.7f, 131.3f),
    Muscle.PECTORALS to Rect(58.4f, 67.3f, 139.1f, 102.9f),
    Muscle.TRICEPS to Rect(43.1f, 86.5f, 156.9f, 132.2f),
    Muscle.ABDOMINALS to Rect(82.5f, 115.9f, 114.7f, 182.7f),
    Muscle.CALVES to Rect(71.5f, 260.3f, 129.2f, 351.3f),
    Muscle.GLUTES to Rect(60.7f, 156.7f, 138.9f, 237.4f),
    Muscle.TRAPEZIUS to Rect(66.5f, 42.1f, 134.9f, 115.8f),
    Muscle.QUADRICEPS to Rect(63.2f, 184.6f, 135.4f, 251.7f),
    Muscle.HAMSTRINGS to Rect(73.7f, 206.4f, 126.5f, 276.1f),
    Muscle.LATS to Rect(62.3f, 97.2f, 137.7f, 148.2f),
    Muscle.BRACHIALIS to Rect(44.1f, 98.4f, 152.2f, 128.8f),
    Muscle.OBLIQUES to Rect(66.0f, 110.6f, 132.3f, 178.4f),
    Muscle.SOLEUS to Rect(74.0f, 311.7f, 126.5f, 351.8f),
)
