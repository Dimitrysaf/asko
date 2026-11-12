package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens

/**
 * A share image's page: on the app's background, the title and the lines under it as the page
 * shows them, then [content], and the app's name at the foot.
 */
@Composable
fun ShareFrame(title: String, lines: List<String>, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.PageGutter, vertical = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            content()
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
