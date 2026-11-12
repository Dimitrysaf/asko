package io.github.dimitrysaf.asko.components

import io.github.dimitrysaf.asko.data.model.mainMuscles
import io.github.dimitrysaf.asko.data.model.otherMuscles
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.constants.Dimens
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
 * How the exercise is done, swiped through (dots underneath say where you are): your own photos
 * and videos of it first, then the library's demo, then (with Settings' Show muscles on) the
 * muscles it works on wger's body, its main ones strong and the others lighter. The library's demo
 * is ExerciseDB's animation (sharpened, the free GIFs are small), free-exercise-db's photos shown
 * in turn (start, end, start…), so they read as the movement, or, for an exercise of yours, that
 * same muscle picture. With [onRemoveMedia], one of yours shows Remove in its corner.
 */
@Composable
fun ExerciseDemo(
    exercise: Exercise,
    shape: Shape,
    modifier: Modifier = Modifier,
    fit: Boolean = false,
    onRemoveMedia: ((ExerciseMedia) -> Unit)? = null,
) {
    val own = ownMediaOf(exercise)
    // The muscles as a page of their own, unless the demo already is them (an exercise of yours).
    val muscles = LocalShowMuscles.current && !exercise.isDrawn && (exercise.mainMuscles + exercise.otherMuscles).isNotEmpty()
    val pages = own.size + 1 + (if (muscles) 1 else 0)
    if (pages == 1) {
        LibraryDemo(exercise, shape, modifier)
        return
    }
    val pager = rememberPagerState { pages }
    Box(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
            when {
                page < own.size -> OwnMedia(own[page], shape, Modifier.fillMaxSize(), if (fit) ContentScale.Fit else ContentScale.Crop)
                page == own.size -> LibraryDemo(exercise, shape, Modifier.fillMaxSize())
                else -> ExerciseArtwork(exercise, shape, Modifier.fillMaxSize())
            }
        }
        // On one of yours, Remove sits in its top-right corner.
        val current = own.getOrNull(pager.currentPage)
        if (onRemoveMedia != null && current != null) {
            FilledTonalIconButton(
                onClick = { onRemoveMedia(current) },
                modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.SpaceS),
            ) { Icon(Icons.Filled.Delete, stringResource(R.string.action_remove)) }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(Dimens.SpaceS)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(pages) { i ->
                Box(Modifier.size(6.dp).background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.45f), CircleShape))
            }
        }
    }
}

/**
 * The library's demo on its own (see [ExerciseDemo]), moving: ExerciseDB's animation playing,
 * free-exercise-db's photos in turn, an exercise of yours drawn from its muscles.
 */
@Composable
fun LibraryDemo(exercise: Exercise, shape: Shape, modifier: Modifier = Modifier) {
    if (exercise.isDrawn) {
        ExerciseArtwork(exercise, shape, modifier)
        return
    }
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
    // All of them loaded once and stacked, each fading in on its turn: no loading flash between them.
    Box(modifier) {
        photos.forEachIndexed { i, url ->
            val alpha by animateFloatAsState(if (i == shown) 1f else 0f, tween(FADE_MS), label = "photo")
            RemoteImage(
                url = url,
                contentDescription = exercise.name.takeIf { i == shown },
                shape = shape,
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

/** How long each photo shows, and the fade between them. */
private const val PHOTO_MS = 1_000L
private const val FADE_MS = 250
