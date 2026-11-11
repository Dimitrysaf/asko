package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.constants.Dimens

/**
 * Large rounded action button, in the style of Google Health's Log / Start buttons. Fixed height
 * and a one-line label keep a row of them even.
 */
@Composable
fun PillButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceM),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(Dimens.SpaceS))
        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A [PillButton] pinned at the bottom of a page (a Scaffold's bottomBar), full width. */
@Composable
fun PinnedPillBar(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        PillButton(
            icon = icon,
            label = label,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.PageGutter, vertical = Dimens.SpaceS),
        )
    }
}
