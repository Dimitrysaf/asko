package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.lib.format.aboutDuration
import java.time.Duration

/**
 * One routine in a segmented list: its first exercise's thumbnail, name with about how long it
 * takes ([averageDuration], when it's been done) at the end of the line, and the exercises it
 * contains; under them, its main action, Start, spanning the item. [exercises] are the routine's
 * exercises looked up in the library (may be empty while it loads). [selected]: rounded evenly
 * and highlighted, like Material's selected list item (in progress, or being dragged).
 * [inProgress]: its workout is in progress, so Start reads Resume.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineListItem(
    routine: Routine,
    exercises: List<Exercise>,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    averageDuration: Duration? = null,
    selected: Boolean = false,
    inProgress: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = animatedSegmentedShape(index, count, rounded = selected),
        color = if (selected) colors.secondaryContainer else colors.surfaceContainer,
        modifier = modifier,
    ) {
        Column {
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
                    supportingColor = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                ),
                leadingContent = {
                    val first = exercises.firstOrNull()
                    if (first != null) {
                        ExerciseThumbnail(first, Modifier.size(Dimens.ExerciseThumbnailSize))
                    } else {
                        Box(
                            Modifier.size(Dimens.ExerciseThumbnailSize)
                                .background(colors.primaryContainer, MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = colors.onPrimaryContainer)
                        }
                    }
                },
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(routine.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (averageDuration != null) {
                            Spacer(Modifier.width(Dimens.SpaceS))
                            Text(
                                aboutDuration(averageDuration),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                supportingContent = {
                    Text(exercises.joinToString { it.name }, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
            )
            PillButton(
                icon = Icons.Filled.PlayArrow,
                label = stringResource(if (inProgress) R.string.workout_resume else R.string.routine_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(start = Dimens.SpaceM, end = Dimens.SpaceM, bottom = Dimens.SpaceM),
            )
        }
    }
}
