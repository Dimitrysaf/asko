package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.pages.routines.CustomWeightDialog
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.weightValue

/**
 * The plate calculator: build the bar, read the total. The total is on top, with Add a plate
 * beside it (asks a plate's weight and puts one on each side); below, a large barbell. Tap the bar
 * for its weight; tap a plate to take that pair off. Set, at the bottom, uses the total for the set. Two barbells doubles the total (still
 * one barbell drawn).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlateCalculatorSheet(onUse: (Double) -> Unit, onDismiss: () -> Unit) {
    val unit = LocalWeightUnit.current
    var barKg by rememberSaveable { mutableStateOf(unit.barKg) }
    val plates = rememberSaveable(saver = PlatesSaver) { mutableStateListOf<Double>() }
    var twoBars by rememberSaveable { mutableStateOf(false) }
    var editingBar by rememberSaveable { mutableStateOf(false) }
    var addingPlate by rememberSaveable { mutableStateOf(false) }
    var removing by rememberSaveable { mutableStateOf<Double?>(null) }
    val total = (barKg + 2 * plates.sum()) * if (twoBars) 2 else 1

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.calculator_total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(kgText(total), style = MaterialTheme.typography.displayMedium)
                }
                FilledTonalButton(onClick = { addingPlate = true }, shapes = ButtonDefaults.shapes()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(Dimens.SpaceS))
                    Text(stringResource(R.string.calculator_add_plate))
                }
            }

            // One barbell, even with Two barbells on: that only doubles the total.
            Barbell(
                barKg = barKg,
                plates = plates.sortedDescending(),
                onBar = { editingBar = true },
                onPlate = { removing = it },
            )

            SegmentedListItem(
                checked = twoBars,
                onCheckedChange = { twoBars = it },
                shapes = segmentedShapes(0, 1),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                supportingContent = { Text(stringResource(R.string.calculator_two_bars_hint)) },
                trailingContent = { Switch(checked = twoBars, onCheckedChange = null) },
                content = { Text(stringResource(R.string.calculator_two_bars)) },
            )

            Button(
                onClick = { onUse(total) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.weight_set))
            }
        }
    }

    if (editingBar) {
        CustomWeightDialog(
            initialKg = barKg,
            title = stringResource(R.string.calculator_bar_title),
            onPick = {
                barKg = it
                editingBar = false
            },
            onDismiss = { editingBar = false },
        )
    }
    if (addingPlate) {
        CustomWeightDialog(
            initialKg = null,
            title = stringResource(R.string.calculator_plate_title),
            onPick = {
                plates += it
                addingPlate = false
            },
            onDismiss = { addingPlate = false },
        )
    }
    removing?.let { plate ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.calculator_remove_title, weightValue(plate))) },
            text = { Text(stringResource(R.string.calculator_remove_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        plates.remove(plate)
                        removing = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/**
 * A barbell from the side, large: the thin bar in the middle (tap it for its weight), a collar on
 * each side, and the thicker sleeves at the ends with the plates on them, the same on each side,
 * heaviest against the collar, sized by weight (tap one to take that pair off). Everything sits
 * on one centre line; the bar's weight is written under it all.
 */
@Composable
private fun Barbell(barKg: Double, plates: List<Double>, onBar: () -> Unit, onPlate: (Double) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val sizes = plates.distinct()
    val plateColors = listOf(colors.primary, colors.tertiary, colors.secondary, colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer)
    fun colorOf(kg: Double) = plateColors[sizes.indexOf(kg).coerceIn(0, plateColors.lastIndex)]
    // The sleeves fit their plates, with a bit of sleeve showing past them.
    val sleeveWidth = maxOf(MinSleeveWidth, plates.fold(0.dp) { w, kg -> w + plateWidth(kg) + 2.dp } + SleeveTip)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().height(BarbellHeight), verticalAlignment = Alignment.CenterVertically) {
            Sleeve(sleeveWidth, plates, Alignment.CenterEnd, ::colorOf, onPlate)
            Collar()
            // The bar: thin, but the whole height is the place to tap.
            Box(
                Modifier
                    .weight(1f)
                    .height(BarbellHeight)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onBar),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxWidth().height(BarThickness).background(colors.outline))
            }
            Collar()
            Sleeve(sleeveWidth, plates, Alignment.CenterStart, ::colorOf, onPlate)
        }
        Text(
            stringResource(R.string.calculator_bar_weight, kgText(barKg)),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.SpaceXs),
        )
    }
}

/**
 * One end of the bar: the sleeve, thicker than the bar, with the plates on it against the collar
 * ([collarSide] is where the collar is), heaviest nearest it.
 */
@Composable
private fun Sleeve(
    width: Dp,
    plates: List<Double>,
    collarSide: Alignment,
    colorOf: (Double) -> androidx.compose.ui.graphics.Color,
    onPlate: (Double) -> Unit,
) {
    Box(Modifier.width(width).height(BarbellHeight), contentAlignment = collarSide) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(SleeveThickness)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.shapes.extraSmall),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val outwards = if (collarSide == Alignment.CenterEnd) plates.reversed() else plates
            outwards.forEach { Plate(it, colorOf(it), onPlate) }
        }
    }
}

/** The collar between a sleeve and the bar, which the plates rest against. */
@Composable
private fun Collar() {
    Box(Modifier.size(width = 8.dp, height = 36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.shapes.extraSmall))
}

/** A plate: taller and thicker the heavier it is, its weight written on it. */
@Composable
private fun Plate(kg: Double, color: androidx.compose.ui.graphics.Color, onPlate: (Double) -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 1.dp)
            .width(plateWidth(kg))
            .height(plateHeight(kg))
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .clickable(role = Role.Button) { onPlate(kg) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            LocalWeightUnit.current.number(kg),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.surface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private val BarThickness = 10.dp
private val SleeveThickness = 20.dp
private val MinSleeveWidth = 72.dp
private val SleeveTip = 14.dp

@Composable
private fun kgText(kg: Double) = weightValue(kg)

private val BarbellHeight = 160.dp

/** A 25 kg plate is near full height, a small change plate a short disc. */
private fun plateHeight(kg: Double): Dp = (56 + (kg.coerceIn(0.0, 25.0) / 25.0) * 96).dp

/** Wide enough for its weight written on it ("1.25"), thicker for heavier plates. */
private fun plateWidth(kg: Double): Dp = when {
    kg >= 15 -> 34.dp
    kg >= 5 -> 30.dp
    else -> 28.dp
}

private val PlatesSaver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<Double>, Double>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)
