package io.github.dimitrysaf.asko.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

/**
 * The shrug emoji (🤷), for something logged with nothing in it. Material's icons have no shrug,
 * so it's the emoji, drawn about as big as an icon of [size].
 */
@Composable
fun Shrug(size: Dp, description: String?, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { (size * 0.8f).toSp() }
    Text(
        "\uD83E\uDD37",
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center,
        modifier = modifier.semantics { if (description != null) contentDescription = description },
    )
}
