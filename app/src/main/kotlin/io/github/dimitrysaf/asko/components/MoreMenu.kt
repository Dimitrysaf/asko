package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R

/**
 * A page's ⋮ button in its top bar, opening Material's dropdown menu of the less used actions.
 * Each [MoreItem] closes the menu as it's chosen.
 */
@Composable
fun MoreMenu(items: @Composable ColumnScope.(close: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) { items { open = false } }
    }
}

/** One action in a [MoreMenu]: its icon and label; [destructive] ones (Delete) in red. */
@Composable
fun MoreItem(label: String, icon: Painter, close: () -> Unit, destructive: Boolean = false, onClick: () -> Unit) {
    val colors = if (destructive) {
        MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error, leadingIconColor = MaterialTheme.colorScheme.error)
    } else {
        MenuDefaults.itemColors()
    }
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = colors,
        onClick = {
            close()
            onClick()
        },
    )
}
