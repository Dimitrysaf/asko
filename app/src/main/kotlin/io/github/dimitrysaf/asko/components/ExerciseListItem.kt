package io.github.dimitrysaf.asko.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise

/**
 * One row of a segmented exercise list: thumbnail, name, target muscles (or [supporting]),
 * optional [trailing] content such as a checkbox. [index] and [count]
 * give the first and last rows their rounder outer corners. Space rows by [ListItemDefaults.SegmentedGap].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseListItem(
    exercise: Exercise,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = segmentedShapes(index, count),
        modifier = modifier,
        // Visible container, as in Material's segmented list sample; the default blends into the page.
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            ExerciseThumbnail(exercise, Modifier.size(Dimens.ExerciseThumbnailSize))
        },
        supportingContent = {
            Text(supporting ?: exercise.primaryMuscles.ifEmpty { listOf(exercise.category) }.joinToString())
        },
        trailingContent = trailing,
        content = { Text(exercise.name) },
    )
}

/** Placeholder for a segmented exercise list while the library loads: same rows, same shapes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseListSkeleton(modifier: Modifier = Modifier, count: Int = 8) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        repeat(count) { index ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, segmentedShapes(index, count).shape)
                    .padding(Dimens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                SkeletonBox(Modifier.size(Dimens.ExerciseThumbnailSize))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    SkeletonLine(0.6f)
                    SkeletonLine(0.35f, height = 12.dp)
                }
            }
        }
    }
}

/**
 * Material's segmented shapes, except a group of one is both first and last: rounded top and
 * bottom (Material leaves a lone item with the small middle-of-list corners).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun segmentedShapes(index: Int, count: Int): ListItemShapes {
    if (count != 1) return ListItemDefaults.segmentedShapes(index = index, count = count)
    val first = ListItemDefaults.segmentedShapes(index = 0, count = 2)
    val top = first.shape as? CornerBasedShape
    val bottom = ListItemDefaults.segmentedShapes(index = 1, count = 2).shape as? CornerBasedShape
    if (top == null || bottom == null) return first
    return first.copy(shape = top.copy(bottomStart = bottom.bottomStart, bottomEnd = bottom.bottomEnd))
}

/**
 * [segmentedShapes] whose selected item is rounded evenly on all four corners (by default the
 * selected item keeps its position's mixed corners).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun selectableSegmentedShapes(index: Int, count: Int): ListItemShapes {
    val shapes = segmentedShapes(index, count)
    val outer = (ListItemDefaults.segmentedShapes(index = 0, count = 2).shape as? CornerBasedShape)?.topStart
        ?: return shapes
    return shapes.copy(selectedShape = RoundedCornerShape(outer))
}

/**
 * A block's shape in a segmented list: [segmentedShapes]' corners, rounded evenly (like a selected
 * SegmentedListItem) when [rounded]. The corners animate between the two with the theme's motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun animatedSegmentedShape(index: Int, count: Int, rounded: Boolean): Shape {
    val density = LocalDensity.current
    fun CornerSize.dp(): Dp = with(density) { toPx(Size(1000f, 1000f), density).toDp() }
    val base = segmentedShapes(index, count).shape as? CornerBasedShape ?: return segmentedShapes(index, count).shape
    val outer = (ListItemDefaults.segmentedShapes(index = 0, count = 2).shape as? CornerBasedShape)?.topStart?.dp()
        ?: base.topStart.dp()
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val top by animateDpAsState(if (rounded) outer else base.topStart.dp(), spec, label = "top corners")
    val bottom by animateDpAsState(if (rounded) outer else base.bottomStart.dp(), spec, label = "bottom corners")
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

