package io.github.dimitrysaf.asko.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.asko.data.model.Exercise

/**
 * Rounded-square list thumbnail: the first frame of the exercise's animation (rows don't all
 * animate at once), or its first photo.
 */
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier) {
    RemoteImage(exercise.stillUrl, contentDescription = null, modifier = modifier, still = true)
}
