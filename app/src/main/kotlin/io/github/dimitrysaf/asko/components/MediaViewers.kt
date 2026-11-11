package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.exercisedb.ExerciseDbApi

/** Full-screen black viewer with a close button; Back also closes it. */
@Composable
private fun FullScreenViewer(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            content()
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(Dimens.SpaceS),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
    }
}

/**
 * Shows one image or animation scaled to fill the screen: full width for a portrait screen with a
 * square or wide asset, full height when the asset is the taller one (Fit, never cropped).
 */
@Composable
fun ImageViewer(url: String, contentDescription: String?, onDismiss: () -> Unit) {
    var loaded by remember(url) { mutableStateOf(false) }
    FullScreenViewer(onDismiss) {
        // A square skeleton until the asset arrives (it has no size before that).
        if (!loaded) SkeletonBox(Modifier.padding(Dimens.PageGutter).fillMaxWidth().aspectRatio(1f))
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High, // the asset is small; this is the smoothest enlargement
            onSuccess = { loaded = true },
            modifier = Modifier.sharpenEnlarged(ExerciseDbApi.GIF_SIZE_PX).fillMaxSize(),
        )
    }
}
