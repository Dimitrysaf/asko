package io.github.dimitrysaf.asko.pages.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.dimitrysaf.asko.AskoApplication
import io.github.dimitrysaf.asko.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.components.ExerciseBrowser
import io.github.dimitrysaf.asko.components.ExerciseBrowserSkeleton
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise

/**
 * Full-screen exercise picker: Discovery's search, filters and list, with a checkbox per row.
 * Returns the ticked exercise ids in the order they were ticked. [exercises] null = still loading.
 * [single]: picks one, the first tapped (no checkboxes, no Add button), as for swapping.
 */
@Composable
fun ExercisePicker(
    exercises: List<Exercise>?,
    onAdd: (exerciseIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.routine_add_exercises),
    single: Boolean = false,
) {
    val picked = remember { mutableStateListOf<String>() }
    val container = (LocalContext.current.applicationContext as AskoApplication).container
    val favourites by container.favouritesRepository.ids.collectAsStateWithLifecycle()
    val recent by container.workoutRepository.recentExerciseIdsState.collectAsStateWithLifecycle()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // A Surface, so text and icons get the theme's content colour (a Dialog gives none).
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.fillMaxSize()
                    .systemBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceS, vertical = Dimens.SpaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (!single) {
                        Button(onClick = { onAdd(picked.toList()) }, enabled = picked.isNotEmpty()) {
                            Text(stringResource(R.string.routine_add_count, picked.size))
                        }
                    }
                }
                if (exercises == null) {
                    ExerciseBrowserSkeleton()
                } else {
                    ExerciseBrowser(
                        exercises = exercises,
                        favourites = favourites,
                        recent = recent.orEmpty(),
                        onExerciseClick = { exercise ->
                            if (single) onAdd(listOf(exercise.id)) else if (!picked.remove(exercise.id)) picked += exercise.id
                        },
                        trailing = if (single) null else { exercise -> Checkbox(checked = exercise.id in picked, onCheckedChange = null) },
                    )
                }
            }
        }
    }
}
