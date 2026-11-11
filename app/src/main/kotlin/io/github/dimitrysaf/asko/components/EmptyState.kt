package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.constants.Dimens

/**
 * An empty state: an icon in Material's expressive cookie shape with the [message] under it, both
 * centred in what it fills (the page, or with [compact] a card), and an optional button such as
 * "Try again". Every empty state in the app uses this.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyState(
    icon: Painter,
    message: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = (if (compact) modifier.fillMaxWidth() else modifier.fillMaxSize())
            .padding(if (compact) Dimens.SpaceM else Dimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(if (compact) 64.dp else 96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (compact) 32.dp else 44.dp),
            )
        }
        Text(
            text = message,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null) {
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
