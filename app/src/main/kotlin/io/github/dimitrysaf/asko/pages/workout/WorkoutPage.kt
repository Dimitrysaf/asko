package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.Icons
import io.github.dimitrysaf.asko.components.pageSkeletonPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import io.github.dimitrysaf.asko.components.pageContentPadding
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ExerciseListSkeleton
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.lib.format.formatClock
import io.github.dimitrysaf.asko.lib.format.formatDate
import io.github.dimitrysaf.asko.pages.routines.ExercisePlanList
import java.time.Instant
import java.util.Date

/**
 * A workout from History, shown like a routine's page: when it was, then each exercise with its
 * rest and its sets as done (badge, reps or time, weight), read-only. Tap an exercise for its page;
 * Edit and Delete in the top bar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutPage(workoutId: Long, onBack: () -> Unit, onEdit: () -> Unit, onExerciseClick: (exerciseId: String) -> Unit) {
    val viewModel = appViewModel { WorkoutViewModel(workoutId, it.workoutRepository, it.exerciseRepository) }
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    val done = workout
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    PageScaffold(
        title = done?.let { it.routineName ?: stringResource(R.string.history_workout) }.orEmpty(),
        onBack = onBack,
        actions = {
            if (done != null) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.action_edit)) }
                IconButton(onClick = { confirmingDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete)) }
            }
        },
    ) { padding ->
        val rows = done?.exercises.orEmpty().mapNotNull { planned -> viewModel.library[planned.exerciseId]?.let { it to planned } }
        if (done == null || (rows.isEmpty() && done.exercises.isNotEmpty())) {
            Column(Modifier.pageSkeletonPadding(padding)) { ExerciseListSkeleton(count = 4) }
        } else {
            LazyColumn(
                modifier = Modifier.consumeWindowInsets(padding),
                contentPadding = pageContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                item(key = "when") {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                        Text(done.startedAt.formatDate(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            workoutTimes(done),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // One block, so rows sit at the segmented gap rather than the page's spacing.
                item(key = "exercises") {
                    ExercisePlanList(rows, onExerciseClick)
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.workout_delete_title)) },
            text = { Text(stringResource(R.string.workout_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        viewModel.delete(onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** "Started 18:30 · Took 52:10 · Finished 19:22"; without a known duration (logged afterwards), "Started 18:30". */
@Composable
fun workoutTimes(workout: Workout): String {
    val format = DateFormat.getTimeFormat(LocalContext.current)
    fun clock(at: Instant): String = format.format(Date.from(at))
    val started = stringResource(R.string.history_started, clock(workout.startedAt))
    val took = workout.duration
    val ended = workout.endedAt
    return if (took == null || ended == null) {
        started
    } else {
        listOf(
            started,
            stringResource(R.string.history_took, took.formatClock()),
            stringResource(R.string.history_finished, clock(ended)),
        ).joinToString(" · ")
    }
}
