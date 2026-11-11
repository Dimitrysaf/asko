package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest

/**
 * An image from the network: a skeleton until it arrives, then the image on white (ExerciseDB's
 * demos are drawn on white). Animated GIFs play; [still] shows just their first frame, which
 * keeps long lists light. Give it a size; it fills it.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    still: Boolean = false,
) {
    val context = LocalPlatformContext.current
    val model: Any = if (!still) url else remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .decoderFactory(BitmapFactoryDecoder.Factory()) // decodes frame one only
            .memoryCacheKey("$url#still") // keep it apart from the animated version in memory
            .build()
    }
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State?>(null) }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        // Best filtering when enlarging: ExerciseDB's free GIFs are only 180 px.
        filterQuality = FilterQuality.High,
        onState = { state = it },
        modifier = modifier.clip(shape).then(
            when (state) {
                is AsyncImagePainter.State.Success -> Modifier.background(Color.White)
                // Failed (offline, broken file): a still block, so it doesn't look like it's still loading.
                is AsyncImagePainter.State.Error -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                else -> Modifier.skeleton(shape)
            },
        ),
    )
}
