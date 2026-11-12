package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ShareFrame
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.stats.PersonalRecord
import io.github.dimitrysaf.asko.lib.format.formatDate
import io.github.dimitrysaf.asko.pages.routines.ExercisePlanList

/**
 * A done workout as a picture to share: its name, when, the numbers as Workout done lists them,
 * the records it set, and each exercise as History shows it.
 */
@Composable
internal fun WorkoutShareImage(
    workout: Workout,
    rows: List<Pair<Exercise, RoutineExercise>>,
    records: List<PersonalRecord>,
    library: Map<String, Exercise>,
) {
    ShareFrame(
        title = workout.routineName ?: stringResource(R.string.history_workout),
        lines = listOf(workout.startedAt.formatDate(), workoutTimes(workout)),
    ) {
        WorkoutStats(workout, first = 0, count = workoutStatCount(workout), animate = false)
        if (records.isNotEmpty()) {
            Text(stringResource(R.string.stats_review_records), style = MaterialTheme.typography.titleLarge)
            RecordList(records, library)
        }
        ExercisePlanList(rows, onExerciseClick = null)
    }
}
