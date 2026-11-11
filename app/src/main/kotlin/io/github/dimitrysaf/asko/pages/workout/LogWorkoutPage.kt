package io.github.dimitrysaf.asko.pages.workout

import io.github.dimitrysaf.asko.components.NotesButton
import io.github.dimitrysaf.asko.components.NotesSheet
import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.PinnedPillBar
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.lib.di.appViewModel
import io.github.dimitrysaf.asko.pages.routines.EditableTitle
import io.github.dimitrysaf.asko.pages.routines.PlanEditorScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/**
 * Log a workout you did (or, with a [workoutId], edit one from History): the routine editor, a new
 * log opening on the exercise picker. The title is when it was (tap ✎ for Material's date picker, then its time picker), Log at the top right stores it,
 * and Add exercises is pinned at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutPage(onBack: () -> Unit, onLogged: (day: LocalDate) -> Unit, workoutId: Long = 0) {
    val viewModel = appViewModel { LogWorkoutViewModel(it.workoutRepository, it.noteRepository, it.exerciseRepository, it.settings, workoutId) }
    var notesOpen by rememberSaveable { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    // The day chosen in the date picker (ISO), waiting for its time.
    var pickedDay by rememberSaveable { mutableStateOf<String?>(null) }

    PlanEditorScaffold(
        viewModel = viewModel,
        onBack = onBack,
        discardMessage = stringResource(if (viewModel.editing) R.string.workout_edit_discard_message else R.string.log_discard_message),
        emptyMessage = stringResource(R.string.log_empty_message),
        title = { EditableTitle(whenLabel(viewModel.at), stringResource(R.string.log_change_time)) { pickingDate = true } },
        actions = {
            // The workout's notes, before Log.
            NotesButton(count = viewModel.notes.size, onClick = { notesOpen = true })
            Button(
                onClick = { viewModel.log(onLogged) },
                enabled = viewModel.canLog,
                modifier = Modifier.padding(end = Dimens.SpaceS),
            ) { Text(stringResource(if (viewModel.editing) R.string.action_save else R.string.action_log)) }
        },
        bottomBar = { openPicker ->
            PinnedPillBar(Icons.Filled.Add, stringResource(R.string.routine_add_exercises), onClick = openPicker)
        },
        // A new log opens on the exercise picker; an edit opens on what was done.
        startPicking = !viewModel.editing,
    )

    if (notesOpen) {
        NotesSheet(
            notes = viewModel.notes,
            onAdd = viewModel::addNote,
            onEdit = viewModel::editNote,
            onDelete = viewModel::deleteNote,
            onDismiss = { notesOpen = false },
        )
    }
    if (pickingDate) {
        // DatePicker works in UTC midnights.
        fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.at.toLocalDate().utcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today.utcMillis()
                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { pickedDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                        pickingDate = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text(stringResource(R.string.log_next)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state) }
    }

    pickedDay?.let { day ->
        val state = rememberTimePickerState(
            initialHour = viewModel.at.hour,
            initialMinute = viewModel.at.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        TimePickerDialog(
            onDismissRequest = { pickedDay = null },
            title = { Text(stringResource(R.string.log_time_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.changeTime(LocalDate.parse(day).atTime(state.hour, state.minute))
                    pickedDay = null
                }) { Text(stringResource(R.string.weight_set)) }
            },
            dismissButton = { TextButton(onClick = { pickedDay = null }) { Text(stringResource(R.string.action_cancel)) } },
        ) { TimePicker(state) }
    }
}

/** "Today, 18:30", "Yesterday, 18:30" or "Nov 9, 2026, 18:30", in the phone's 12/24-hour style. */
@Composable
private fun whenLabel(at: LocalDateTime): String {
    val today = LocalDate.now()
    val day = when (at.toLocalDate()) {
        today -> stringResource(R.string.log_today)
        today.minusDays(1) -> stringResource(R.string.log_yesterday)
        else -> at.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val time = DateFormat.getTimeFormat(LocalContext.current).format(Date.from(at.atZone(ZoneId.systemDefault()).toInstant()))
    return stringResource(R.string.log_when, day, time)
}
