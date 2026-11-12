package io.github.dimitrysaf.asko.pages.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import io.github.dimitrysaf.asko.lib.format.distanceUnitSymbol
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlin.math.abs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.WeightUnit
import io.github.dimitrysaf.asko.lib.format.distanceNumber
import io.github.dimitrysaf.asko.lib.format.distanceValue
import io.github.dimitrysaf.asko.lib.format.metersOf
import io.github.dimitrysaf.asko.lib.format.paceSeconds
import io.github.dimitrysaf.asko.lib.format.paceValue

// Sets of a distance exercise (running, rowing): a distance and the time it took, and, read-only,
// the pace. Laid out like the other set tables: the badge, then a column each.

/**
 * Which columns a distance table shows. Read-only, as in the other previews, a column no set has
 * a value in is left out, and Pace only shows once a set has both a distance and a time. Being
 * edited, Distance and Time are always there.
 */
internal data class DistanceColumns(val distance: Boolean = true, val time: Boolean = true, val pace: Boolean = false)

internal fun distanceColumns(sets: List<PlannedSet>) = DistanceColumns(
    distance = sets.any { it.distanceMeters != null },
    time = sets.any { it.durationSeconds != null },
    pace = sets.any { it.distanceMeters != null && it.durationSeconds != null },
)

/** The column titles: Set, then those of [columns] shown. */
@Composable
internal fun DistanceTitles(columns: DistanceColumns) {
    Row(
        modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        ColumnLabel(stringResource(R.string.routine_col_set), Modifier.width(SetColumnWidth))
        if (columns.distance) ColumnLabel(stringResource(R.string.routine_col_distance), Modifier.weight(1f))
        if (columns.time) ColumnLabel(stringResource(R.string.routine_col_time), Modifier.weight(1f))
        if (columns.pace) ColumnLabel(stringResource(R.string.routine_col_pace), Modifier.weight(1f))
        // With no value columns, the set badges keep to the left.
        if (!columns.distance && !columns.time) Spacer(Modifier.weight(1f))
    }
}

/** One distance set, read-only: "5 km" · "25:00" · "5:00 /km", only the [columns] shown. */
@Composable
internal fun DistancePreviewRow(set: PlannedSet, number: Int?, columns: DistanceColumns, badge: @Composable () -> Unit = { SetBadge(set.type, number) }) {
    val unit = LocalWeightUnit.current
    Row(
        modifier = Modifier.padding(top = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        badge()
        DistanceCells(set.distanceMeters, set.durationSeconds, columns, unit)
        if (!columns.distance && !columns.time) Spacer(Modifier.weight(1f))
    }
}

/** The read-only value cells of a distance set, in a row: only the [columns] shown. */
@Composable
internal fun RowScope.DistanceCells(meters: Double?, seconds: Int?, columns: DistanceColumns, unit: WeightUnit = LocalWeightUnit.current) {
    if (columns.distance) PreviewCell(meters?.let { distanceValue(it) }.orEmpty(), Modifier.weight(1f))
    if (columns.time) PreviewCell(seconds?.let(::formatSeconds).orEmpty(), Modifier.weight(1f))
    if (columns.pace) PreviewCell(unit.paceSeconds(meters, seconds)?.let { paceValue(it) }.orEmpty(), Modifier.weight(1f))
}

/**
 * A distance to type, in km (or mi): decimals allowed; what's typed is kept as typed (so "5." can
 * become "5.2"), and the metres it means are reported as it changes.
 */
@Composable
internal fun DistanceField(meters: Double?, onMeters: (Double?) -> Unit, modifier: Modifier = Modifier) {
    val unit = LocalWeightUnit.current
    var text by remember(unit) { mutableStateOf(meters?.let(unit::distanceNumber).orEmpty()) }
    // Changed from elsewhere (the set's sheet): show that instead of what was typed.
    LaunchedEffect(meters) {
        val typed = text.toDoubleOrNull()?.takeIf { it > 0 }?.let(unit::metersOf)
        if (typed == null && meters == null) return@LaunchedEffect
        if (typed == null || meters == null || abs(meters - typed) > 0.5) text = meters?.let(unit::distanceNumber).orEmpty()
    }
    SetCell(
        value = text,
        keyboardType = KeyboardType.Decimal,
        onValueChange = { input ->
            val cleaned = input.replace(',', '.')
            if (cleaned.length <= 6 && cleaned.all { it.isDigit() || it == '.' } && cleaned.count { it == '.' } <= 1) {
                text = cleaned
                onMeters(cleaned.toDoubleOrNull()?.takeIf { it > 0 }?.let(unit::metersOf))
            }
        },
        modifier = modifier,
    )
}

/** How far, typed into a standard dialog in km (or mi), like the reps and weight dialogs; Log returns metres. */
@Composable
internal fun DistanceDialog(initialMeters: Double?, title: String, onLog: (Double) -> Unit, onDismiss: () -> Unit) {
    val unit = LocalWeightUnit.current
    var text by rememberSaveable { mutableStateOf(initialMeters?.takeIf { it > 0 }?.let(unit::distanceNumber).orEmpty()) }
    val meters = text.toDoubleOrNull()?.takeIf { it > 0 }?.let(unit::metersOf)
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val cleaned = input.replace(',', '.')
                    if (cleaned.length <= 6 && cleaned.all { it.isDigit() || it == '.' } && cleaned.count { it == '.' } <= 1) text = cleaned
                },
                label = { Text(distanceUnitSymbol(unit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { meters?.let(onLog) }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { meters?.let(onLog) }, enabled = meters != null) { Text(stringResource(R.string.action_log)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}
