package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Routine

/**
 * One routine in a segmented list: its first exercise's thumbnail, name, and the exercises it
 * contains. [exercises] are the routine's exercises looked up in the library (may be empty while
 * it loads). [selected]: it has a workout in progress (Material's selected list item: evenly
 * rounded, highlighted).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineListItem(
    routine: Routine,
    exercises: List<Exercise>,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = selectableSegmentedShapes(index, count),
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = {
            val first = exercises.firstOrNull()
            if (first != null) {
                ExerciseThumbnail(first, Modifier.size(Dimens.ExerciseThumbnailSize))
            } else {
                Box(
                    Modifier.size(Dimens.ExerciseThumbnailSize)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        supportingContent = {
            Text(exercises.joinToString { it.name }, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        content = { Text(routine.name) },
    )
}
