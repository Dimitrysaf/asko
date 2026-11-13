package io.github.dimitrysaf.asko.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier

/**
 * Material's plain tooltip on an icon button (or anything without a label): holding it (or
 * hovering with a mouse) shows [text], the same words a screen reader says, under it. No tooltip
 * when [text] is null. A button placed in its parent (aligned in a Box) takes that [modifier] here,
 * on an outer box, not on the button inside it, where the parent can't see it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (text == null) {
        Box(modifier) { content() }
        return
    }
    // Material's tooltip box wraps its anchor in a box of its own that takes no modifier, so the
    // placement goes on an outer box here, which the parent does see.
    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
            tooltip = { PlainTooltip { Text(text) } },
            state = rememberTooltipState(),
        ) {
            content()
        }
    }
}
