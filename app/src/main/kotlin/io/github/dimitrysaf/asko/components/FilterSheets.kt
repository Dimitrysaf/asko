package io.github.dimitrysaf.asko.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Muscle

/**
 * A filter chip that opens a sheet to choose from: shows [label] until something is chosen, then
 * the choice. [sheet] is the sheet, given a way to close itself.
 */
@Composable
fun SheetFilterChip(label: String, selected: String?, sheet: @Composable (close: () -> Unit) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    FilterChip(
        selected = selected != null,
        onClick = { open = true },
        label = { Text(selected ?: label) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
    )
    if (open) sheet { open = false }
}

/**
 * The muscle groups to filter by, in a sheet: Any first, then each group with a round preview of
 * where it is on the body.
 */
@Composable
fun MuscleGroupSheet(groups: List<String>, selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    ChoiceSheet(
        title = stringResource(R.string.discovery_muscle_group),
        anyLabel = stringResource(R.string.discovery_any_muscle_group),
        options = groups,
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
        // Any: the whole body.
        anyPreview = { MusclePreview(emptySet(), whole = true) },
        preview = { group -> MuscleGroupPreview(group) },
    )
}

/**
 * The equipment to filter by, in a sheet: Any first, then each with a round picture of an
 * exercise done with it ([exercises]), or an icon when there's none.
 */
@Composable
fun EquipmentSheet(
    equipment: List<String>,
    exercises: List<Exercise>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ChoiceSheet(
        title = stringResource(R.string.discovery_equipment),
        anyLabel = stringResource(R.string.discovery_any_equipment),
        options = equipment,
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
        // Any: an asterisk.
        anyPreview = { IconCircle(painterResource(R.drawable.ic_asterisk)) },
        preview = { item -> EquipmentPicture(item, exercises) },
    )
}

/**
 * A muscle group's place on wger's body, round: the side that shows more of it, enlarged to it.
 * A group wger has no muscle image for (Cardio, Lower Arms) shows the whole front.
 */
@Composable
fun MuscleGroupPreview(group: String, modifier: Modifier = Modifier) {
    val muscles = remember(group) { Muscle.ofBodyPart(group) }
    MusclePreview(muscles, modifier, whole = muscles.isEmpty())
}

/**
 * Where [muscles] are, round: the side that shows more of them, enlarged to them ([whole]: the
 * whole front instead); none, the plain body.
 */
@Composable
fun MusclePreview(muscles: Set<Muscle>, modifier: Modifier = Modifier, size: Dp = PreviewSize, shade: Float = 1f, whole: Boolean = false) {
    Box(modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        BodyMap(
            shade = muscles.associateWith { shade },
            sides = listOf(if (whole) BodySide.FRONT else bestSideOf(muscles)),
            focus = !whole && muscles.isNotEmpty(),
            modifier = Modifier.size(size),
        )
    }
}

/** A round picture of an exercise done with [item] ([exercises] to pick from), or an icon. */
@Composable
fun EquipmentPicture(item: String?, exercises: List<Exercise>, size: Dp = PreviewSize) {
    val url = remember(item, exercises) { item?.let { pictureFor(it, exercises) } }
    if (url != null) {
        RemoteImage(url, contentDescription = null, shape = CircleShape, contentScale = ContentScale.Crop, still = true, modifier = Modifier.size(size))
    } else {
        PlaceholderCircle(size)
    }
}

/** An exercise done with just [item], photos first (a clearer look at it); its picture's address. */
private fun pictureFor(item: String, exercises: List<Exercise>): String? {
    val with = exercises.filter { item in it.equipment && it.stillUrl.isNotEmpty() }
    return (with.firstOrNull { it.equipment.size == 1 && it.photos.isNotEmpty() } ?: with.firstOrNull { it.equipment.size == 1 } ?: with.firstOrNull())
        ?.stillUrl
}

/**
 * Choosing any number of [options], in a sheet: a segmented list, each with its round [preview],
 * its [label] and a checkbox; it stays open while you tick.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> MultiChoiceSheet(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    onDismiss: () -> Unit,
    preview: @Composable (T) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = Dimens.SpaceM))
            options.forEachIndexed { index, option ->
                val ticked = option in selected
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    onClick = { onToggle(option) },
                    shapes = segmentedShapes(index, options.size),
                    leadingContent = { preview(option) },
                    trailingContent = { Checkbox(checked = ticked, onCheckedChange = null) },
                    content = { Text(label(option)) },
                )
            }
        }
    }
}

/** A round icon where there's no picture. */
@Composable
private fun PlaceholderCircle(size: Dp = PreviewSize) = IconCircle(painterResource(R.drawable.ic_fitness), size)

/** An icon in a round, in the preview's place. */
@Composable
internal fun IconCircle(icon: Painter, size: Dp = PreviewSize) {
    Box(
        Modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/**
 * A sheet of choices as one selectable segmented list: [anyLabel] (no filter) first, then each
 * option with its round [preview]; the chosen one is selected and checked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChoiceSheet(
    title: String,
    anyLabel: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    anyPreview: @Composable () -> Unit,
    preview: @Composable (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = Dimens.SpaceM))
            val all = listOf<String?>(null) + options
            all.forEachIndexed { index, option ->
                val chosen = option == selected
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    },
                    shapes = selectableSegmentedShapes(index, all.size),
                    leadingContent = {
                        if (option == null) {
                            anyPreview()
                        } else {
                            preview(option)
                        }
                    },
                    trailingContent = if (chosen) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    content = { Text(option ?: anyLabel) },
                )
            }
        }
    }
}

private val PreviewSize = 56.dp
