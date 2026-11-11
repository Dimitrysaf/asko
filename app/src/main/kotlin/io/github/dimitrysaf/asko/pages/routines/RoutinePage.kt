package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.ButtonDefaults
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import io.github.dimitrysaf.asko.components.pageContentPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.ExerciseListSkeleton
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.PinnedPillBar
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.lib.di.appViewModel

/** A routine's plan (each exercise with its rest and set rows), Edit and Delete, and Start pinned at the bottom. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutinePage(
    routineId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onResumeWorkout: (routineId: Long) -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit,
) {
    val viewModel = appViewModel { RoutineViewModel(routineId, it.routineRepository, it.exerciseRepository, it.activeWorkoutStore) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    var confirmingSwitch by rememberSaveable { mutableStateOf(false) }
    val resuming = activeWorkout?.routineId == routineId
    val loaded = state as? RoutineState.Loaded

    PageScaffold(
        title = loaded?.routine?.name.orEmpty(),
        onBack = onBack,
        actions = {
            if (loaded != null) {
                SubjectNotes(NoteSubject.ROUTINE, routineId.toString())
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.action_edit)) }
                IconButton(onClick = { confirmingDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
            }
        },
        bottomBar = {
            if (loaded != null) {
                // This routine's workout in progress: Resume. Another routine's: ask first.
                PinnedPillBar(
                    Icons.Filled.PlayArrow,
                    stringResource(if (resuming) R.string.workout_resume else R.string.routine_start),
                    onClick = { if (activeWorkout == null || resuming) onStart() else confirmingSwitch = true },
                )
            }
        },
    ) { padding ->
        when (val s = state) {
            RoutineState.Loading -> Column(Modifier.pageSkeletonPadding(padding)) {
                ExerciseListSkeleton(count = 4)
            }
            RoutineState.NotFound -> EmptyState(
                rememberVectorPainter(Icons.AutoMirrored.Filled.List),
                stringResource(R.string.routines_empty),
                Modifier.padding(padding),
            )
            is RoutineState.Loaded -> LazyColumn(
                modifier = Modifier.consumeWindowInsets(padding),
                contentPadding = pageContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                // One block, so rows sit at the segmented gap rather than the page's spacing.
                item(key = "exercises") {
                    val rows = s.routine.exercises.mapNotNull { planned -> s.library[planned.exerciseId]?.let { it to planned } }
                    ExercisePlanList(rows, onExerciseClick)
                }
            }
        }
    }

    val other = activeWorkout
    if (confirmingSwitch && other != null) {
        AlertDialog(
            onDismissRequest = { confirmingSwitch = false },
            title = { Text(stringResource(R.string.workout_in_progress)) },
            text = { Text(stringResource(R.string.workout_switch_message, other.routineName)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingSwitch = false
                    onResumeWorkout(other.routineId)
                }) { Text(stringResource(R.string.workout_resume)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmingSwitch = false
                        viewModel.discardActiveWorkout(onStart)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.workout_discard_and_start)) }
            },
        )
    }
    if (confirmingDelete && loaded != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.routine_delete_title)) },
            text = { Text(stringResource(R.string.routine_delete_message, loaded.routine.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
