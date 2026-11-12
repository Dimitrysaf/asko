package io.github.dimitrysaf.asko.components

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.MediaType
import io.github.dimitrysaf.asko.data.model.mainMuscles
import io.github.dimitrysaf.asko.data.model.otherMuscles
import java.io.File

/**
 * Your photos, GIFs and videos on exercises, by exercise id, provided at the top of the app
 * (see MainActivity): every exercise picture shows them first.
 */
val LocalExerciseMedia = staticCompositionLocalOf<Map<String, List<ExerciseMedia>>> { emptyMap() }

/** Settings' Show muscles: each exercise's pictures end with the muscles it works (on by default). */
val LocalShowMuscles = staticCompositionLocalOf { true }

/** Your media on [exercise], oldest first; empty when none. */
@Composable
fun ownMediaOf(exercise: Exercise): List<ExerciseMedia> = LocalExerciseMedia.current[exercise.id].orEmpty()

/** One of your media, filling its box: a photo or GIF (playing), or a video looping without sound. */
@Composable
fun OwnMedia(media: ExerciseMedia, shape: Shape, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier.clip(shape).background(Color.Black), contentAlignment = Alignment.Center) {
        when (media.type) {
            MediaType.IMAGE -> AsyncImage(
                model = File(media.path),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            MediaType.VIDEO -> AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setOnPreparedListener { player ->
                            player.isLooping = true
                            player.setVolume(0f, 0f)
                            start()
                        }
                        setVideoPath(media.path)
                    }
                },
                onRelease = { it.stopPlayback() },
            )
        }
    }
}

/** A still of your media, for lists: the photo, or the video's first frame. */
@Composable
fun OwnMediaStill(media: ExerciseMedia, shape: Shape, modifier: Modifier = Modifier) {
    AsyncImage(
        model = File(media.stillPath),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

/** An exercise of yours without media is drawn from its muscles. */
val Exercise.isDrawn: Boolean get() = source == ExerciseSource.CUSTOM && animationUrl.isEmpty() && photos.isEmpty()

/**
 * An exercise of yours, drawn: the body with its main muscles in the primary colour and the others
 * it works lighter. [small]: one side, enlarged to the muscles (for list thumbnails).
 */
@Composable
fun ExerciseArtwork(exercise: Exercise, shape: Shape, modifier: Modifier = Modifier, small: Boolean = false) {
    val main = exercise.mainMuscles
    val shade = exercise.otherMuscles.associateWith { 0.25f } + main.associateWith { 1f }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        BodyMap(
            shade = shade,
            sides = if (small) listOf(bestSideOf(main.ifEmpty { shade.keys })) else BodySide.entries,
            focus = small && shade.isNotEmpty(),
            description = exercise.name,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
