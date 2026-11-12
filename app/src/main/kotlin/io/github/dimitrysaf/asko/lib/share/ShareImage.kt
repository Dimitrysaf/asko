package io.github.dimitrysaf.asko.lib.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** How wide a share image is drawn: about a phone's width, so it reads like the app. */
private val ShareWidth = 400.dp

/**
 * While shown, draws [content] out of sight at a phone's width, waits for its pictures, saves it
 * as a PNG named [name] and opens Android's share sheet with it; then [onDone]. Nothing of it
 * shows on screen: it's recorded, never drawn.
 */
@Composable
fun ShareImage(name: String, onDone: () -> Unit, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val context = LocalContext.current
    val done by rememberUpdatedState(onDone)
    Box(
        Modifier
            // Its full height at the share width, taking no room on the page.
            .layout { measurable, _ ->
                val width = ShareWidth.roundToPx()
                val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width))
                layout(0, 0) { placeable.place(0, 0) }
            }
            .drawWithContent { layer.record { this@drawWithContent.drawContent() } },
    ) { content() }
    LaunchedEffect(Unit) {
        // Laid out and its pictures asked for; then drawn with them in.
        repeat(2) { withFrameNanos {} }
        ImageLoads.awaitIdle()
        repeat(2) { withFrameNanos {} }
        val bitmap = layer.toImageBitmap().asAndroidBitmap()
        val uri = withContext(Dispatchers.IO) { save(context, bitmap, name) }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
        done()
    }
}

/** Writes [bitmap] to the app's cache (replacing earlier share images), for the share sheet. */
private fun save(context: Context, bitmap: Bitmap, name: String): Uri {
    val folder = File(context.cacheDir, "shared").apply {
        deleteRecursively()
        mkdirs()
    }
    // A safe file name: letters, digits, spaces and dashes.
    val file = File(folder, name.replace(Regex("[^\\p{L}\\p{N} -]"), "").trim().ifEmpty { "Asko" } + ".png")
    // Drawn on the GPU; compressing needs an ordinary copy.
    val plain = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
    file.outputStream().use { plain.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.share", file)
}
