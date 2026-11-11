package io.github.dimitrysaf.asko.pages.settings

import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.pageContentPadding
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.backup.BackupCounts
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.pages.routines.DurationSheet
import io.github.dimitrysaf.asko.pages.routines.RestOptions
import io.github.dimitrysaf.asko.pages.routines.restLabel
import java.time.LocalDate

/**
 * Settings, from the Routines tab's top bar. Workouts: the rest new exercises start with, buzzing
 * when a rest is over, keeping the screen on. Your data: back up routines and history to a file,
 * or restore them from one. About: the version and where the exercises come from.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPage(onBack: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings, it.backupRepository) }
    val settings = viewModel.settings
    val defaultRest by settings.defaultRestSeconds.collectAsStateWithLifecycle()
    val buzz by settings.restBuzz.collectAsStateWithLifecycle()
    val keepScreenOn by settings.keepScreenOn.collectAsStateWithLifecycle()
    var choosingRest by rememberSaveable { mutableStateOf(false) }
    var restoreFrom by rememberSaveable { mutableStateOf<String?>(null) }
    val resolver = LocalContext.current.contentResolver
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // The system's file picker: where to save a backup, and which backup to restore.
    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_TYPE)) { uri ->
        if (uri != null) viewModel.backUp(resolver, uri)
    }
    val openFrom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreFrom = uri.toString()
    }

    val message = viewModel.result?.let { result ->
        when (result) {
            is BackupResult.BackedUp -> stringResource(R.string.settings_backed_up, counts(result.counts))
            is BackupResult.Restored -> stringResource(R.string.settings_restored, counts(result.counts))
            BackupResult.Failed -> stringResource(R.string.settings_backup_failed)
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            viewModel.resultShown()
            snackbar.showSnackbar(message)
        }
    }

    PageScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding),
            contentPadding = pageContentPadding(padding),
        ) {
            item(key = "workouts") {
                Section(stringResource(R.string.settings_workouts)) {
                    SegmentedListItem(
                        onClick = { choosingRest = true },
                        shapes = segmentedShapes(0, 3),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_default_rest_hint)) },
                        trailingContent = { Text(restLabel(defaultRest), style = MaterialTheme.typography.labelLarge) },
                        content = { Text(stringResource(R.string.settings_default_rest)) },
                    )
                    SegmentedListItem(
                        checked = buzz,
                        onCheckedChange = settings::setRestBuzz,
                        shapes = segmentedShapes(1, 3),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_rest_buzz_hint)) },
                        trailingContent = { Switch(checked = buzz, onCheckedChange = null) },
                        content = { Text(stringResource(R.string.settings_rest_buzz)) },
                    )
                    SegmentedListItem(
                        checked = keepScreenOn,
                        onCheckedChange = settings::setKeepScreenOn,
                        shapes = segmentedShapes(2, 3),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_keep_screen_on_hint)) },
                        trailingContent = { Switch(checked = keepScreenOn, onCheckedChange = null) },
                        content = { Text(stringResource(R.string.settings_keep_screen_on)) },
                    )
                }
            }
            item(key = "data") {
                Section(stringResource(R.string.settings_data)) {
                    SegmentedListItem(
                        onClick = { saveTo.launch("asko-backup-${LocalDate.now()}.json") },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(0, 2),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_back_up_hint)) },
                        content = { Text(stringResource(R.string.settings_back_up)) },
                    )
                    SegmentedListItem(
                        onClick = { openFrom.launch(arrayOf(BACKUP_TYPE, "*/*")) },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(1, 2),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_restore_hint)) },
                        content = { Text(stringResource(R.string.settings_restore)) },
                    )
                }
            }
            item(key = "about") {
                Section(stringResource(R.string.settings_about)) {
                    val version = remember {
                        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                    }
                    ListItem(
                        modifier = Modifier.clip(segmentedShapes(0, 2).shape),
                        colors = itemColors(),
                        supportingContent = { Text(version) },
                        headlineContent = { Text(stringResource(R.string.settings_version)) },
                    )
                    ListItem(
                        modifier = Modifier.clip(segmentedShapes(1, 2).shape),
                        colors = itemColors(),
                        headlineContent = { Text(stringResource(R.string.credit_source)) },
                    )
                }
            }
        }
    }

    if (choosingRest) {
        DurationSheet(
            title = stringResource(R.string.settings_default_rest),
            customTitle = stringResource(R.string.rest_custom_title),
            options = RestOptions,
            current = defaultRest,
            label = { restLabel(it) },
            onPick = {
                settings.setDefaultRestSeconds(it)
                choosingRest = false
            },
            onDismiss = { choosingRest = false },
        )
    }
    restoreFrom?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreFrom = null },
            title = { Text(stringResource(R.string.settings_restore_title)) },
            text = { Text(stringResource(R.string.settings_restore_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreFrom = null
                        viewModel.restore(resolver, android.net.Uri.parse(uri))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.settings_restore)) }
            },
            dismissButton = { TextButton(onClick = { restoreFrom = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** A titled group of settings, as one segmented list. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = Dimens.SpaceL), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.SpaceM, bottom = Dimens.SpaceS),
        )
        content()
    }
}

@Composable
private fun itemColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

/** "3 routines and 42 workouts". */
@Composable
private fun counts(counts: BackupCounts): String = stringResource(
    R.string.settings_counts,
    pluralStringResource(R.plurals.settings_routine_count, counts.routines, counts.routines),
    pluralStringResource(R.plurals.settings_workout_count, counts.workouts, counts.workouts),
)

private const val BACKUP_TYPE = "application/json"
