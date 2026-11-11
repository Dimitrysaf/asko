package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.components.SkeletonBox
import io.github.dimitrysaf.asko.components.SkeletonLine
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import io.github.dimitrysaf.asko.pages.routines.isBodyweight
import io.github.dimitrysaf.asko.pages.routines.weightText

/**
 * What was done in a workout, as one segmented list (Material list items in segmented shapes):
 * each exercise with its picture, name and its sets as done ("8 × 20 kg, 8 × 20 kg, 6 × 20 kg").
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoggedWorkoutList(
    workout: Workout,
    library: Map<String, Exercise>,
    modifier: Modifier = Modifier,
) {
    val count = workout.exercises.size
    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        workout.exercises.forEachIndexed { index, done ->
            val exercise = library[done.exerciseId]
            val bodyweight = exercise?.isBodyweight == true
            ListItem(
                modifier = Modifier.clip(segmentedShapes(index, count).shape),
                colors = colors,
                leadingContent = {
                    if (exercise != null) {
                        ExerciseThumbnail(exercise, Modifier.size(Dimens.ExerciseThumbnailSize))
                    } else {
                        SkeletonBox(Modifier.size(Dimens.ExerciseThumbnailSize))
                    }
                },
                headlineContent = {
                    if (exercise != null) Text(exercise.name) else SkeletonLine(0.6f)
                },
                supportingContent = {
                    Text(done.sets.map { setDone(it, done.timed, bodyweight) }.joinToString(", "))
                },
            )
        }
    }
}

/** "8 × 20 kg", "8", "1:00", or "-". */
@Composable
private fun setDone(set: PlannedSet, timed: Boolean, bodyweight: Boolean): String {
    val amount = if (timed) set.durationSeconds?.let(::formatSeconds) else set.reps?.toString()
    val weight = weightText(set.weightKg, bodyweight).takeIf { it.isNotEmpty() }
    return listOfNotNull(amount, weight).joinToString(" × ").ifEmpty { "-" }
}
