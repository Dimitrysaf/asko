package io.github.dimitrysaf.asko.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.asko.data.model.Exercise

/**
 * Rounded-square list thumbnail: your own photo or video of it first; else the first frame of
 * the exercise's animation (rows don't all animate at once), or its first photo; an exercise of
 * yours without any is drawn from its muscles.
 */
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    val own = ownMediaOf(exercise).firstOrNull()
    when {
        own != null -> OwnMediaStill(own, shape, modifier)
        exercise.isDrawn -> ExerciseArtwork(exercise, shape, modifier, small = true)
        else -> RemoteImage(exercise.stillUrl, contentDescription = null, modifier = modifier, still = true)
    }
}
