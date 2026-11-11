package io.github.dimitrysaf.asko.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import io.github.dimitrysaf.asko.data.exercisedb.ExerciseDbApi
import io.github.dimitrysaf.asko.data.model.Exercise
import kotlinx.coroutines.delay

/**
 * How the exercise is done: ExerciseDB's animation (sharpened, the free GIFs are small), or
 * free-exercise-db's photos shown in turn (start, end, start…), so they read as the movement.
 */
@Composable
fun ExerciseDemo(exercise: Exercise, shape: Shape, modifier: Modifier = Modifier) {
    val photos = exercise.photos
    if (exercise.animationUrl.isNotEmpty() || photos.isEmpty()) {
        RemoteImage(
            url = exercise.animationUrl,
            contentDescription = exercise.name,
            shape = shape,
            modifier = modifier.sharpenEnlarged(ExerciseDbApi.GIF_SIZE_PX),
        )
        return
    }
    var shown by remember(photos) { mutableIntStateOf(0) }
    LaunchedEffect(photos) {
        while (true) {
            delay(PHOTO_MS)
            shown = (shown + 1) % photos.size
        }
    }
    Crossfade(targetState = shown, modifier = modifier, animationSpec = tween(FADE_MS), label = "photos") { i ->
        RemoteImage(url = photos[i], contentDescription = exercise.name, shape = shape, modifier = Modifier.fillMaxSize())
    }
}

/** How long each photo shows, and the fade between them. */
private const val PHOTO_MS = 1_000L
private const val FADE_MS = 250
