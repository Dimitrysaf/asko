package io.github.dimitrysaf.asko.pages.routines

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.asko.components.ShareFrame
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.RoutineExercise

/** A routine as a picture to share: its name, about how long it takes, and its plan as the routine page shows it. */
@Composable
internal fun RoutineShareImage(name: String, about: String?, rows: List<Pair<Exercise, RoutineExercise>>) {
    ShareFrame(title = name, lines = listOfNotNull(about)) {
        ExercisePlanList(rows, onExerciseClick = null)
    }
}
