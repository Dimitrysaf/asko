package io.github.dimitrysaf.asko.pages.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.selectableSegmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import androidx.compose.foundation.layout.Row

/**
 * Which folder a routine is kept in, picked from one selectable list: No folder, each folder
 * there is, and New folder… last (asks its name). [current]'s row is checked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolderSheet(current: String?, folders: List<String>, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    var naming by rememberSaveable { mutableStateOf(false) }
    // null: no folder; then the folders; the last row makes a new one.
    val choices: List<String?> = listOf(null) + folders
    val count = choices.size + 1
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.folder_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            choices.forEachIndexed { index, folder ->
                val selected = folder.equals(current, ignoreCase = true) || (folder == null && current == null)
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    onClick = { onPick(folder) },
                    shapes = selectableSegmentedShapes(index, count),
                    leadingContent = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
                    trailingContent = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    content = { Text(folder ?: stringResource(R.string.folder_none)) },
                )
            }
            SegmentedListItem(
                verticalAlignment = Alignment.CenterVertically,
                onClick = { naming = true },
                shapes = selectableSegmentedShapes(count - 1, count),
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                content = { Text(stringResource(R.string.folder_new)) },
            )
        }
    }
    if (naming) {
        RoutineNameDialog(
            initialName = "",
            confirmLabel = stringResource(R.string.action_create),
            title = stringResource(R.string.folder_new_title),
            placeholder = stringResource(R.string.folder_name_hint),
            onConfirm = { name ->
                naming = false
                // A folder already there by that name (in any case) is used rather than doubled.
                onPick(folders.firstOrNull { it.equals(name, ignoreCase = true) } ?: name)
            },
            onDismiss = { naming = false },
        )
    }
}

/** The title over a folder's routines on the Routines tab: the folder icon and its name, as a section title. */
@Composable
internal fun FolderTitle(name: String, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = Dimens.SpaceM, bottom = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Icon(painterResource(R.drawable.ic_folder), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
