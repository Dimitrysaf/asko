package io.github.dimitrysaf.asko.components

import io.github.dimitrysaf.asko.data.model.mainMuscles
import io.github.dimitrysaf.asko.data.model.otherMuscles
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
 * same muscle picture. With [onRemoveMedia], one of yours shows Remove in its corner. With
 * [onAddMedia], a first page, left of your own (where new ones land), offers to add photos and
 * videos (as many as you like); it opens one page in, on the first picture.
 */
@Composable
fun ExerciseDemo(
    exercise: Exercise,
    shape: Shape,
    modifier: Modifier = Modifier,
    fit: Boolean = false,
    onRemoveMedia: ((ExerciseMedia) -> Unit)? = null,
    onAddMedia: (() -> Unit)? = null,
) {
    val own = ownMediaOf(exercise)
    // The muscles as a page of their own, unless the demo already is them (an exercise of yours).
    val muscles = LocalShowMuscles.current && !exercise.isDrawn && (exercise.mainMuscles + exercise.otherMuscles).isNotEmpty()
    val adding = onAddMedia != null
    val pages = own.size + 1 + (if (muscles) 1 else 0) + (if (adding) 1 else 0)
    if (pages == 1) {
        LibraryDemo(exercise, shape, modifier)
        return
    }
    // With the Add page first, everything else is one page further on.
    val first = if (adding) 1 else 0
    val pager = rememberPagerState(initialPage = first) { pages }
    // Photos or videos just added come first: show the first of them.
    var ownCount by remember { mutableIntStateOf(own.size) }
    LaunchedEffect(own.size) {
        if (own.size > ownCount) pager.animateScrollToPage(first)
        ownCount = own.size
    }
    Box(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
            val shown = page - first
            when {
                shown < 0 -> AddMediaPage(shape, Modifier.fillMaxSize(), onAddMedia ?: {})
                shown < own.size -> OwnMedia(own[shown], shape, Modifier.fillMaxSize(), if (fit) ContentScale.Fit else ContentScale.Crop)
                shown == own.size -> LibraryDemo(exercise, shape, Modifier.fillMaxSize())
                else -> ExerciseArtwork(exercise, shape, Modifier.fillMaxSize())
            }
        }
        // On one of yours, Remove sits in its top-right corner.
        val current = own.getOrNull(pager.currentPage - first)
        if (onRemoveMedia != null && current != null) {
            WithTooltip(stringResource(R.string.action_remove), Modifier.align(Alignment.TopEnd)) {
                FilledTonalIconButton(
                    onClick = { onRemoveMedia(current) },
                    modifier = Modifier.padding(Dimens.SpaceS),
                ) { Icon(Icons.Filled.Delete, stringResource(R.string.action_remove)) }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(Dimens.SpaceS)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages) { i ->
                val tint = if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.45f)
                if (adding && i == 0) {
                    // The Add page isn't a picture: a small plus instead of its dot.
                    Icon(Icons.Filled.Add, contentDescription = null, tint = tint, modifier = Modifier.size(8.dp))
                } else {
                    Box(Modifier.size(6.dp).background(tint, CircleShape))
                }
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

/** The last page of an [ExerciseDemo] you can add to: a big image icon and its words; tap to pick. */
@Composable
private fun AddMediaPage(shape: Shape, modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onAdd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS, Alignment.CenterVertically),
    ) {
        Icon(
            painterResource(R.drawable.ic_image),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(stringResource(R.string.media_add), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
