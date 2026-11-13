package io.github.dimitrysaf.asko.pages.settings

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
 * Settings, from the Routines tab's top bar. Workouts: kg or lb, the rest new exercises start
 * with, buzzing or a sound when a rest is over, keeping the screen on. Your data: back up routines and history to a file,
 * or restore them from one. At the bottom, centred: the app, its version and where the exercises come from.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPage(onBack: () -> Unit) {
    val viewModel = appViewModel { SettingsViewModel(it.settings, it.backupRepository, it.workoutImporter) }
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
            item(key = "workouts") {
                Section(stringResource(R.string.settings_workouts)) {
                    // kg | lb and km | mi, right in their rows: two choices need no sheet; tapping
                    // the row switches too.
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setWeightUnit(WeightUnit.entries.first { it != weightUnit }) },
                        shapes = segmentedShapes(0, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_weight_unit_hint)) },
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
                        shapes = segmentedShapes(1, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_distance_unit_hint)) },
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
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { choosingRest = true },
                        shapes = segmentedShapes(2, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_default_rest_hint)) },
                        trailingContent = { Text(restLabel(defaultRest), style = MaterialTheme.typography.labelLarge) },
                        content = { Text(stringResource(R.string.settings_default_rest)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setRestBuzz(!buzz) },
                        shapes = segmentedShapes(3, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_rest_buzz_hint)) },
                        trailingContent = { HapticSwitch(checked = buzz) },
                        content = { Text(stringResource(R.string.settings_rest_buzz)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setRestSound(!sound) },
                        shapes = segmentedShapes(4, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_rest_sound_hint)) },
                        trailingContent = { HapticSwitch(checked = sound) },
                        content = { Text(stringResource(R.string.settings_rest_sound)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setKeepScreenOn(!keepScreenOn) },
                        shapes = segmentedShapes(5, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_keep_screen_on_hint)) },
                        trailingContent = { HapticSwitch(checked = keepScreenOn) },
                        content = { Text(stringResource(R.string.settings_keep_screen_on)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setEditableWorkoutRows(!editableRows) },
                        shapes = segmentedShapes(6, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_editable_rows_hint)) },
                        trailingContent = { HapticSwitch(checked = editableRows) },
                        content = { Text(stringResource(R.string.settings_editable_rows)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { choosingCounter = true },
                        shapes = segmentedShapes(7, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_rep_counter_hint)) },
                        trailingContent = { Text(stringResource(repCounterName(repCounter)), style = MaterialTheme.typography.labelLarge) },
                        content = { Text(stringResource(R.string.settings_rep_counter)) },
                    )
                    val sayReps by settings.sayReps.collectAsStateWithLifecycle()
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setSayReps(!sayReps) },
                        shapes = segmentedShapes(8, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_say_reps_hint)) },
                        trailingContent = { HapticSwitch(checked = sayReps) },
                        content = { Text(stringResource(R.string.settings_say_reps)) },
                    )
                    val pauseOutOfView by settings.pauseOutOfView.collectAsStateWithLifecycle()
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setPauseOutOfView(!pauseOutOfView) },
                        shapes = segmentedShapes(9, 10),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_pause_out_of_view_hint)) },
                        trailingContent = { HapticSwitch(checked = pauseOutOfView) },
                        content = { Text(stringResource(R.string.settings_pause_out_of_view)) },
                    )
                }
            }
            item(key = "streaks") {
                Section(stringResource(R.string.settings_streaks)) {
                    RestDays(settings)
                }
            }
            item(key = "sources") {
                Section(stringResource(R.string.settings_sources)) {
                    ExerciseSources(settings)
                    val showMuscles by settings.showMuscles.collectAsStateWithLifecycle()
                    Spacer(Modifier.height(Dimens.SpaceM))
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setShowMuscles(!showMuscles) },
                        shapes = segmentedShapes(0, 1),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_show_muscles_hint)) },
                        trailingContent = { HapticSwitch(checked = showMuscles) },
                        content = { Text(stringResource(R.string.settings_show_muscles)) },
                    )
                }
            }
            item(key = "health") {
                Section(stringResource(R.string.settings_health_section)) {
                    HealthConnectSetting(settings, (context.applicationContext as AskoApplication).container.healthConnect)
                }
            }
            item(key = "data") {
                Section(stringResource(R.string.settings_data)) {
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { saveTo.launch("asko-backup-${LocalDate.now()}.json") },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(0, 3),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_back_up_hint)) },
                        content = { Text(stringResource(R.string.settings_back_up)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { openFrom.launch(arrayOf(BACKUP_TYPE, "*/*")) },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(1, 3),
                        colors = itemColors(),
                        supportingContent = { Text(stringResource(R.string.settings_restore_hint)) },
                        content = { Text(stringResource(R.string.settings_restore)) },
                    )
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { importFrom.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                        enabled = !viewModel.busy,
                        shapes = segmentedShapes(2, 3),
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
 * Rest days: which weekdays you rest (they count towards the streak). Tap for a sheet to pick
 * them; changes apply from today on, so streaks already made stay as they were.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RestDays(settings: SettingsRepository) {
    val schedule by settings.restSchedule.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val week = remember(locale) {
        val first = WeekFields.of(locale).firstDayOfWeek
        List(7) { first.plus(it.toLong()) }
    }
    val current = schedule.current
    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = { picking = true },
        shapes = segmentedShapes(0, 1),
        colors = itemColors(),
        leadingContent = { DayMarkIcon(DayMark.REST, size = 24.dp) },
        supportingContent = {
            Text(
                week.filter { it in current }.joinToString { it.getDisplayName(TextStyle.SHORT, locale) }
                    .ifEmpty { stringResource(R.string.settings_rest_days_none) },
            )
        },
        content = { Text(stringResource(R.string.settings_rest_days)) },
    )
    Text(
        stringResource(R.string.settings_rest_days_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Dimens.SpaceM, end = Dimens.SpaceM, top = Dimens.SpaceS),
    )
    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }) {
            Column(
                Modifier.padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                Text(
                    stringResource(R.string.settings_rest_days),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = Dimens.SpaceM),
                )
                week.forEachIndexed { index, day ->
                    val resting = day in current
                    SegmentedListItem(
                        verticalAlignment = Alignment.CenterVertically,
                        onClick = { settings.setRestDays(if (resting) current - day else current + day) },
                        shapes = segmentedShapes(index, week.size),
                        trailingContent = { Checkbox(checked = resting, onCheckedChange = null) },
                        content = { Text(day.getDisplayName(TextStyle.FULL, locale)) },
                    )
                }
            }
        }
    }
}

/**
 * The exercise libraries in their order (hold and drag one to move it), each with a switch to
 * show or hide it; the last one shown can't be hidden.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseSources(settings: SettingsRepository) {
    val order by settings.sourceOrder.collectAsStateWithLifecycle()
    val hidden by settings.hiddenSources.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    ReorderableColumn(
        list = order,
        onSettle = { from, to -> settings.setSourceOrder(order.toMutableList().apply { add(to, removeAt(from)) }) },
        onMove = { haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) { index, source, _ ->
        key(source) {
            ReorderableItem {
                val shown = source !in hidden
                val lastShown = shown && order.count { it !in hidden } == 1
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    onClick = { settings.setSourceHidden(source, hidden = shown) },
                    enabled = !lastShown,
                    shapes = segmentedShapes(index, order.size),
                    colors = itemColors(),
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    ),
                    // Its place in the order: 1 is first.
                    leadingContent = { SetBadge(SetType.NORMAL, index + 1) },
                    supportingContent = { Text(stringResource(sourceDescription(source))) },
                    trailingContent = { HapticSwitch(checked = shown, enabled = !lastShown) },
                    content = { Text(stringResource(sourceName(source))) },
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

private fun sourceName(source: ExerciseSource) = when (source) {
    ExerciseSource.EXERCISEDB -> R.string.source_exercisedb
    ExerciseSource.FREE_EXERCISE_DB -> R.string.source_free_exercise_db
    ExerciseSource.CUSTOM -> R.string.source_custom
}

private fun sourceDescription(source: ExerciseSource) = when (source) {
    ExerciseSource.EXERCISEDB -> R.string.source_exercisedb_desc
    ExerciseSource.FREE_EXERCISE_DB -> R.string.source_free_exercise_db_desc
    ExerciseSource.CUSTOM -> R.string.source_custom_desc
}

/**
 * Health Connect: a switch to send finished workouts and body weight there. Turning it on asks
 * Health Connect's permissions first; permissions taken back there show it as off. Greyed out
 * where Health Connect isn't.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HealthConnectSetting(settings: SettingsRepository, health: HealthConnectSync) {
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
        shapes = segmentedShapes(0, if (available) 2 else 1),
        colors = itemColors(),
        supportingContent = {
            Text(
                stringResource(
                    when {
                        !available -> R.string.settings_health_missing
                        else -> R.string.settings_health_hint
                    },
                ),
            )
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
            shapes = segmentedShapes(1, 2),
            colors = itemColors(),
            supportingContent = { Text(stringResource(R.string.settings_health_manage_hint)) },
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
                    supportingContent = { Text(stringResource(repCounterDescription(mode))) },
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

private fun repCounterDescription(mode: RepCounterMode) = when (mode) {
    RepCounterMode.OFF -> R.string.rep_counter_off_desc
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
