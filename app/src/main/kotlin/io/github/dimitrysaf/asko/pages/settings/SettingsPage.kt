package io.github.dimitrysaf.asko.pages.settings

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import io.github.dimitrysaf.asko.components.IconCircle
import io.github.dimitrysaf.asko.data.settings.UserLibrary
import io.github.dimitrysaf.asko.components.WithTooltip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import io.github.dimitrysaf.asko.lib.format.distanceUnitSymbol

import io.github.dimitrysaf.asko.lib.format.DistanceUnit

import androidx.compose.ui.Alignment

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.dimitrysaf.asko.data.settings.RepCounterMode
import io.github.dimitrysaf.asko.components.selectableSegmentedShapes

import androidx.compose.material3.Checkbox
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import io.github.dimitrysaf.asko.lib.format.WeightUnit
import io.github.dimitrysaf.asko.lib.format.weightUnitSymbol
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.platform.LocalConfiguration
import io.github.dimitrysaf.asko.components.DayMarkIcon
import io.github.dimitrysaf.asko.data.model.DayMark
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.key
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.pages.routines.SetBadge
import sh.calvin.reorderable.ReorderableColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
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
import io.github.dimitrysaf.asko.components.HapticSwitch
import io.github.dimitrysaf.asko.components.PageScaffold
import io.github.dimitrysaf.asko.components.pageContentPadding
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.backup.BackupCounts
import io.github.dimitrysaf.asko.AskoApplication
import io.github.dimitrysaf.asko.lib.health.HealthConnectSync
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import io.github.dimitrysaf.asko.data.importing.ImportCounts
import io.github.dimitrysaf.asko.data.importing.ImportSource
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.pages.routines.DurationSheet
import io.github.dimitrysaf.asko.pages.routines.RestOptions
import io.github.dimitrysaf.asko.pages.routines.restLabel
import java.time.LocalDate

/**
 * Settings, from the Routines tab's top bar: Units, Workouts (rest, vibrations, sounds, the rep
 * counter, workout days…), Libraries (their order, which are shown) and Data (Health Connect,
 * export and import). At the bottom, centred: the app, its version and where the exercises come from.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPage(onBack: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings, it.backupRepository, it.workoutImporter, it.exerciseRepository) }
    val settings = viewModel.settings
    val defaultRest by settings.defaultRestSeconds.collectAsStateWithLifecycle()
    val buzz by settings.restBuzz.collectAsStateWithLifecycle()
    val sound by settings.restSound.collectAsStateWithLifecycle()
    val weightUnit by settings.weightUnit.collectAsStateWithLifecycle()
    val keepScreenOn by settings.keepScreenOn.collectAsStateWithLifecycle()
    val editableRows by settings.editableWorkoutRows.collectAsStateWithLifecycle()
    var choosingRest by rememberSaveable { mutableStateOf(false) }
    var choosingCounter by rememberSaveable { mutableStateOf(false) }
    val repCounter by settings.repCounter.collectAsStateWithLifecycle()
    // The camera counter needs the camera: asked when it's picked, kept off if refused.
    val askCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) settings.setRepCounter(RepCounterMode.CAMERA)
    }
    var restoreFrom by rememberSaveable { mutableStateOf<String?>(null) }
    var addingLibrary by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
    // Adds to History, replacing nothing: no need to ask first.
    val importFrom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importWorkouts(resolver, uri)
    }

    val message = viewModel.result?.let { result ->
        when (result) {
            is BackupResult.BackedUp -> stringResource(R.string.settings_backed_up, counts(result.counts))
            is BackupResult.Restored -> stringResource(R.string.settings_restored, counts(result.counts))
            is BackupResult.Imported -> imported(result.counts)
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
            item(key = "units") {
                Section(stringResource(R.string.settings_units)) {
                    // kg | lb and km | mi, right in their rows: two choices need no sheet; tapping
                    // the row switches too.
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setWeightUnit(WeightUnit.entries.first { it != weightUnit }) },
                        shapes = segmentedShapes(0, 2),
                        colors = itemColors(),
                        trailingContent = {
                            SingleChoiceSegmentedButtonRow {
                                WeightUnit.entries.forEachIndexed { index, unit ->
                                    SegmentedButton(
                                        selected = unit == weightUnit,
                                        onClick = { settings.setWeightUnit(unit) },
                                        shape = SegmentedButtonDefaults.itemShape(index, WeightUnit.entries.size),
                                        icon = {},
                                        label = { Text(weightUnitSymbol(unit)) },
                                    )
                                }
                            }
                        },
                        content = { Text(stringResource(R.string.settings_weight_unit)) },
                    )
                    val distanceUnit by settings.distanceUnit.collectAsStateWithLifecycle()
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setDistanceUnit(DistanceUnit.entries.first { it != distanceUnit }) },
                        shapes = segmentedShapes(1, 2),
                        colors = itemColors(),
                        trailingContent = {
                            SingleChoiceSegmentedButtonRow {
                                DistanceUnit.entries.forEachIndexed { index, unit ->
                                    SegmentedButton(
                                        selected = unit == distanceUnit,
                                        onClick = { settings.setDistanceUnit(unit) },
                                        shape = SegmentedButtonDefaults.itemShape(index, DistanceUnit.entries.size),
                                        icon = {},
                                        label = { Text(distanceUnitSymbol(unit)) },
                                    )
                                }
                            }
                        },
                        content = { Text(stringResource(R.string.settings_distance_unit)) },
                    )
                }
            }
            item(key = "workouts") {
                Section(stringResource(R.string.settings_workouts)) {
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { choosingRest = true },
                        shapes = segmentedShapes(0, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_default_rest_hint)) },
                        trailingContent = { Text(restLabel(defaultRest), style = MaterialTheme.typography.labelLarge) },
                        content = { Text(stringResource(R.string.settings_default_rest)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setRestBuzz(!buzz) },
                        shapes = segmentedShapes(1, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_rest_buzz_hint)) },
                        trailingContent = { HapticSwitch(checked = buzz) },
                        content = { Text(stringResource(R.string.settings_rest_buzz)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setRestSound(!sound) },
                        shapes = segmentedShapes(2, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_rest_sound_hint)) },
                        trailingContent = { HapticSwitch(checked = sound) },
                        content = { Text(stringResource(R.string.settings_rest_sound)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setKeepScreenOn(!keepScreenOn) },
                        shapes = segmentedShapes(3, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_keep_screen_on_hint)) },
                        trailingContent = { HapticSwitch(checked = keepScreenOn) },
                        content = { Text(stringResource(R.string.settings_keep_screen_on)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setEditableWorkoutRows(!editableRows) },
                        shapes = segmentedShapes(4, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_editable_rows_hint)) },
                        trailingContent = { HapticSwitch(checked = editableRows) },
                        content = { Text(stringResource(R.string.settings_editable_rows)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { choosingCounter = true },
                        shapes = segmentedShapes(5, WORKOUT_ROWS),
                        colors = itemColors(),
                        trailingContent = { Text(stringResource(repCounterName(repCounter)), style = MaterialTheme.typography.labelLarge) },
                        content = { Text(stringResource(R.string.settings_rep_counter)) },
                    )
                    val sayReps by settings.sayReps.collectAsStateWithLifecycle()
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setSayReps(!sayReps) },
                        shapes = segmentedShapes(6, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_say_reps_hint)) },
                        trailingContent = { HapticSwitch(checked = sayReps) },
                        content = { Text(stringResource(R.string.settings_say_reps)) },
                    )
                    val pauseOutOfView by settings.pauseOutOfView.collectAsStateWithLifecycle()
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setPauseOutOfView(!pauseOutOfView) },
                        shapes = segmentedShapes(7, WORKOUT_ROWS),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_pause_out_of_view_hint)) },
                        trailingContent = { HapticSwitch(checked = pauseOutOfView) },
                        content = { Text(stringResource(R.string.settings_pause_out_of_view)) },
                    )
                    WorkoutDays(settings, index = 8, count = WORKOUT_ROWS)
                }
            }
            item(key = "sources") {
                Section(stringResource(R.string.settings_sources)) {
                    val removedMessage = stringResource(R.string.library_removed)
                    val undoLabel = stringResource(R.string.action_undo)
                    ExerciseSources(
                        settings = settings,
                        onAdd = { addingLibrary = true },
                        // Removed at once; Undo downloads it again.
                        onRemove = { library ->
                            viewModel.removeLibrary(library) { undo ->
                                scope.launch {
                                    val result = snackbar.showSnackbar(removedMessage.format(library.name), undoLabel, duration = SnackbarDuration.Long)
                                    if (result == SnackbarResult.ActionPerformed) undo()
                                }
                            }
                        },
                    )
                    val showMuscles by settings.showMuscles.collectAsStateWithLifecycle()
                    val hideDuplicates by settings.hideDuplicates.collectAsStateWithLifecycle()
                    Spacer(Modifier.height(Dimens.SpaceM))
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setShowMuscles(!showMuscles) },
                        shapes = segmentedShapes(0, 2),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_show_muscles_hint)) },
                        trailingContent = { HapticSwitch(checked = showMuscles) },
                        content = { Text(stringResource(R.string.settings_show_muscles)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setHideDuplicates(!hideDuplicates) },
                        shapes = segmentedShapes(1, 2),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_hide_duplicates_hint)) },
                        trailingContent = { HapticSwitch(checked = hideDuplicates) },
                        content = { Text(stringResource(R.string.settings_hide_duplicates)) },
                    )
                }
            }
            item(key = "data") {
                Section(stringResource(R.string.settings_data)) {
                    val health = (context.applicationContext as AskoApplication).container.healthConnect
                    // Sync with Health Connect, and Manage Health Connect where it's on the phone.
                    val healthRows = if (health.available) 2 else 1
                    val dataRows = healthRows + 3
                    HealthConnectSetting(settings, health, count = dataRows)
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { saveTo.launch("asko-backup-${LocalDate.now()}.json") },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(healthRows, dataRows),
                        colors = itemColors(),
                        content = { Text(stringResource(R.string.settings_back_up)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { openFrom.launch(arrayOf(BACKUP_TYPE, "*/*")) },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(healthRows + 1, dataRows),
                        colors = itemColors(),
                        content = { Text(stringResource(R.string.settings_restore)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { importFrom.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(healthRows + 2, dataRows),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_import_hint)) },
                        content = { Text(stringResource(R.string.settings_import)) },
                    )
                }
            }
            // About: a few centred lines at the bottom of the page, no section or list.
            item(key = "about") {
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = Dimens.SpaceL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                ) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.settings_version_line, version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.credit_source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (choosingCounter) {
        RepCounterSheet(
            current = repCounter,
            onPick = { mode ->
                choosingCounter = false
                val cameraAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (mode == RepCounterMode.CAMERA && !cameraAllowed) askCamera.launch(Manifest.permission.CAMERA) else settings.setRepCounter(mode)
            },
            onDismiss = { choosingCounter = false },
        )
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
    if (addingLibrary) {
        AddLibraryDialog(
            busy = viewModel.addingLibrary,
            failed = viewModel.libraryFailed,
            onEdit = viewModel::libraryErrorShown,
            onAdd = { name, url -> viewModel.addLibrary(name, url) { addingLibrary = false } },
            onDismiss = {
                addingLibrary = false
                viewModel.libraryErrorShown()
            },
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

/**
 * Workout days: the weekdays you train, ticked in a sheet. The days left unticked are your rest
 * days (they count towards the streak); changes apply from today on, so past streaks stay.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkoutDays(settings: SettingsRepository, index: Int, count: Int) {
    val schedule by settings.restSchedule.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val week = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        List(7) { first.plus(it.toLong()) }
    }
    // Stored as rest days; shown the other way round.
    val rest = schedule.current
    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = { picking = true },
        shapes = segmentedShapes(index, count),
        colors = itemColors(),
        supportingContent = { Text(stringResource(R.string.settings_workout_days_hint)) },
        content = { Text(stringResource(R.string.settings_workout_days)) },
    )
    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }) {
            Column(
                Modifier.padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                Text(
                    stringResource(R.string.settings_workout_days),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = Dimens.SpaceM),
                )
                week.forEachIndexed { i, day ->
                    val working = day !in rest
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setRestDays(if (working) rest + day else rest - day) },
                        shapes = segmentedShapes(i, week.size),
                        trailingContent = { Checkbox(checked = working, onCheckedChange = null) },
                        content = { Text(day.getDisplayName(TextStyle.FULL, locale)) },
                    )
                }
            }
        }
    }
}

/** How many rows the Workouts section has. */
private const val WORKOUT_ROWS = 9

/**
 * The exercise libraries in their order (hold and drag one to move it), each with a switch to
 * show or hide it (the last one shown can't be hidden); ones you added also have Remove. Add a
 * library comes first.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseSources(settings: SettingsRepository, onAdd: () -> Unit, onRemove: (UserLibrary) -> Unit) {
    val order by settings.sourceOrder.collectAsStateWithLifecycle()
    val hidden by settings.hiddenSources.collectAsStateWithLifecycle()
    val added by settings.userLibraries.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    // The Add row, then the libraries: one segmented list.
    val rows = order.size + 1
    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = onAdd,
        shapes = segmentedShapes(0, rows),
        colors = itemColors(),
        // The + in a circle the size of the libraries' numbers, so the row lines up with theirs.
        leadingContent = { IconCircle(rememberVectorPainter(Icons.Filled.Add), size = 40.dp) },
        content = { Text(stringResource(R.string.settings_add_library)) },
    )
    // (The section already spaces its rows at the segmented gap.)
    ReorderableColumn(
        list = order,
        onSettle = { from, to -> settings.setSourceOrder(order.toMutableList().apply { add(to, removeAt(from)) }) },
        onMove = { haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) { index, source, dragging ->
        key(source.name) {
            // The library being dragged shows selected (no other way to show it's held).
            ReorderableItem {
                val shown = source !in hidden
                val lastShown = shown && order.count { it !in hidden } == 1
                val mine = added.firstOrNull { it.source == source }
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    selected = dragging,
                    onClick = { settings.setSourceHidden(source, hidden = shown) },
                    enabled = !lastShown,
                    shapes = selectableSegmentedShapes(index + 1, rows),
                    colors = itemColors(),
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    ),
                    // Its place in the order: 1 is first.
                    leadingContent = { SetBadge(SetType.NORMAL, index + 1) },
                    supportingContent = { Text(mine?.let { linkHost(it.url) } ?: stringResource(sourceDescription(source))) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mine != null) {
                                WithTooltip(stringResource(R.string.settings_remove_library)) {
                                    IconButton(onClick = { onRemove(mine) }) {
                                        Icon(Icons.Filled.Delete, stringResource(R.string.settings_remove_library))
                                    }
                                }
                            }
                            HapticSwitch(checked = shown, enabled = !lastShown)
                        }
                    },
                    content = { Text(mine?.name ?: stringResource(sourceName(source))) },
                )
            }
        }
    }
    Text(
        stringResource(R.string.settings_sources_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Dimens.SpaceM, end = Dimens.SpaceM, top = Dimens.SpaceS),
    )
}

/** Where a library you added comes from, for its row: the link's site ("github.com"). */
private fun linkHost(url: String): String = runCatching { java.net.URI(url).host }.getOrNull() ?: url

private fun sourceName(source: ExerciseSource) = when (source) {
    ExerciseSource.EXERCISEDB -> R.string.source_exercisedb
    ExerciseSource.FREE_EXERCISE_DB -> R.string.source_free_exercise_db
    ExerciseSource.WGER -> R.string.source_wger
    else -> R.string.source_custom
}

private fun sourceDescription(source: ExerciseSource) = when (source) {
    ExerciseSource.EXERCISEDB -> R.string.source_exercisedb_desc
    ExerciseSource.FREE_EXERCISE_DB -> R.string.source_free_exercise_db_desc
    ExerciseSource.WGER -> R.string.source_wger_desc
    else -> R.string.source_custom_desc
}

/**
 * Add a library: its name and the link to its JSON or CSV file (see LinkedLibraryApi). Add
 * downloads it first ([busy] meanwhile); a link that can't be read says so ([failed]) and the
 * dialog stays.
 */
@Composable
private fun AddLibraryDialog(
    busy: Boolean,
    failed: Boolean,
    onEdit: () -> Unit,
    onAdd: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val ready = name.isNotBlank() && url.trim().let { it.startsWith("https://") || it.startsWith("http://") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_add_library)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onEdit()
                    },
                    label = { Text(stringResource(R.string.add_library_name)) },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        onEdit()
                    },
                    label = { Text(stringResource(R.string.add_library_link)) },
                    singleLine = true,
                    enabled = !busy,
                    isError = failed,
                    supportingText = if (failed) {
                        { Text(stringResource(R.string.add_library_failed)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (busy) LinearWavyProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, url) }, enabled = ready && !busy) { Text(stringResource(R.string.add_library_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Health Connect: a switch to send finished workouts and body weight there. Turning it on asks
 * Health Connect's permissions first; permissions taken back there show it as off. Greyed out
 * where Health Connect isn't.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HealthConnectSetting(settings: SettingsRepository, health: HealthConnectSync, count: Int) {
    val on by settings.healthConnect.collectAsStateWithLifecycle()
    val available = remember { health.available }
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(true) }
    val ask = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { given ->
        granted = given.containsAll(HealthConnectSync.PERMISSIONS)
        if (granted) settings.setHealthConnect(true)
    }
    LaunchedEffect(on) { if (on && available) granted = health.hasPermissions() }
    val checked = on && available && granted
    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = {
            val turnOn = !checked
            if (!available) {
                // Greyed-out switch: nothing to turn on here.
            } else if (!turnOn) {
                settings.setHealthConnect(false)
            } else {
                scope.launch { if (health.hasPermissions()) settings.setHealthConnect(true) else ask.launch(HealthConnectSync.PERMISSIONS) }
            }
        },
        shapes = segmentedShapes(0, count),
        colors = itemColors(),
        // Only says something when Health Connect isn't on the phone.
        supportingContent = if (available) null else {
            { Text(stringResource(R.string.settings_health_missing)) }
        },
        trailingContent = { HapticSwitch(checked = checked, enabled = available) },
        content = { Text(stringResource(R.string.settings_health)) },
    )
    // Health Connect itself, to manage what Asko may write and the data there.
    if (available) {
        val context = LocalContext.current
        SegmentedListItem(
            verticalAlignment = Alignment.CenterVertically,
            // Its main screen: app permissions, and the data each app wrote (an app's own page there
            // is for the system only).
            onClick = { context.startActivity(android.content.Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) },
            shapes = segmentedShapes(1, count),
            colors = itemColors(),
            content = { Text(stringResource(R.string.settings_health_manage)) },
        )
    }
}

/**
 * How reps are counted, picked from one selectable list: Off, Metronome (aloud, at a pace) or
 * Camera (watches you), each with a line on what it does.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RepCounterSheet(current: RepCounterMode, onPick: (RepCounterMode) -> Unit, onDismiss: () -> Unit) {
    val modes = RepCounterMode.entries
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.settings_rep_counter),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            modes.forEachIndexed { index, mode ->
                val selected = mode == current
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    onClick = { onPick(mode) },
                    shapes = selectableSegmentedShapes(index, modes.size),
                    supportingContent = repCounterDescription(mode)?.let { { Text(stringResource(it)) } },
                    trailingContent = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    content = { Text(stringResource(repCounterName(mode))) },
                )
            }
        }
    }
}

private fun repCounterName(mode: RepCounterMode) = when (mode) {
    RepCounterMode.OFF -> R.string.rep_counter_off
    RepCounterMode.METRONOME -> R.string.rep_counter_metronome
    RepCounterMode.CAMERA -> R.string.rep_counter_camera
}

private fun repCounterDescription(mode: RepCounterMode): Int? = when (mode) {
    RepCounterMode.OFF -> null
    RepCounterMode.METRONOME -> R.string.rep_counter_metronome_desc
    RepCounterMode.CAMERA -> R.string.rep_counter_camera_desc
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

/** "Added 52 workouts from Strong · 3 already there · 12 new exercises". */
@Composable
private fun imported(counts: ImportCounts): String = listOfNotNull(
    stringResource(
        R.string.settings_imported,
        pluralStringResource(R.plurals.settings_workout_count, counts.workouts, counts.workouts),
        stringResource(if (counts.source == ImportSource.STRONG) R.string.import_strong else R.string.import_hevy),
    ),
    counts.skipped.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.settings_import_skipped, it, it) },
    counts.newExercises.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.settings_import_new_exercises, it, it) },
).joinToString(" · ")
