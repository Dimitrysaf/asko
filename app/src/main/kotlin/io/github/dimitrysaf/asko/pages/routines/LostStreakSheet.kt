package io.github.dimitrysaf.asko.pages.routines

import androidx.compose.ui.Alignment
import androidx.compose.material3.RadioButton
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.DayMarkIcon
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.DayExcuse
import io.github.dimitrysaf.asko.data.model.DayMark
import io.github.dimitrysaf.asko.data.model.LostStreak

/**
 * The streak was lost to missed days: why? Fell ill or Travelling (for every missed day) pauses
 * it instead, after "Are you lying?" (No keeps the streak, Yes lets it go); I just missed it lets
 * it go.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LostStreakSheet(
    lost: LostStreak,
    onExcuse: (DayExcuse) -> Unit,
    onLetGo: () -> Unit,
    onDismiss: () -> Unit,
) {
    var checking by rememberSaveable { mutableStateOf<DayExcuse?>(null) }
    val days = lost.missed.size
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                pluralStringResource(R.plurals.streak_lost_title, lost.length, lost.length),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                pluralStringResource(R.plurals.streak_lost_why, days, days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.SpaceXs, bottom = Dimens.SpaceM),
            )
            SegmentedListItem(
                verticalAlignment = Alignment.CenterVertically,
                onClick = { checking = DayExcuse.SICK },
                shapes = segmentedShapes(0, 3),
                leadingContent = { DayMarkIcon(DayMark.SICK, size = 24.dp) },
                supportingContent = { Text(pluralStringResource(R.plurals.streak_sick_days, days, days)) },
                content = { Text(stringResource(R.string.streak_fell_ill)) },
            )
            SegmentedListItem(
                verticalAlignment = Alignment.CenterVertically,
                onClick = { checking = DayExcuse.TRAVEL },
                shapes = segmentedShapes(1, 3),
                leadingContent = { DayMarkIcon(DayMark.TRAVEL, size = 24.dp) },
                supportingContent = { Text(pluralStringResource(R.plurals.streak_travel_days, days, days)) },
                content = { Text(stringResource(R.string.streak_travelling)) },
            )
            SegmentedListItem(
                verticalAlignment = Alignment.CenterVertically,
                onClick = onLetGo,
                shapes = segmentedShapes(2, 3),
                leadingContent = { Icon(Icons.Filled.Close, contentDescription = null) },
                supportingContent = { Text(stringResource(R.string.streak_just_missed_hint)) },
                content = { Text(stringResource(R.string.streak_just_missed)) },
            )
        }
    }

    // Just to make you think twice: "No" keeps the streak, "Yes" lets it go.
    checking?.let { excuse ->
        AlertDialog(
            onDismissRequest = { checking = null },
            title = { Text(stringResource(R.string.streak_lying_title)) },
            text = { Text(stringResource(R.string.streak_lying_message)) },
            confirmButton = {
                TextButton(onClick = {
                    checking = null
                    onExcuse(excuse)
                }) { Text(stringResource(R.string.streak_lying_no)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    checking = null
                    onLetGo()
                }) { Text(stringResource(R.string.streak_lying_yes)) }
            },
        )
    }
}

/**
 * Why [day] was missed, from holding it in a calendar: Fell ill, Travelling (both keep the streak
 * going) or I just missed it, the [current] reason's button on. Picking one calls [onPick] (null
 * for just missed).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DayReasonSheet(day: LocalDate, current: DayExcuse?, onPick: (DayExcuse?) -> Unit, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.day_reason_title, day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            val options = listOf(DayExcuse.SICK, DayExcuse.TRAVEL, null)
            options.forEachIndexed { index, option ->
                SegmentedListItem(
                    verticalAlignment = Alignment.CenterVertically,
                    onClick = { onPick(option) },
                    shapes = segmentedShapes(index, options.size),
                    leadingContent = {
                        when (option) {
                            DayExcuse.SICK -> DayMarkIcon(DayMark.SICK, size = 24.dp)
                            DayExcuse.TRAVEL -> DayMarkIcon(DayMark.TRAVEL, size = 24.dp)
                            null -> Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    },
                    trailingContent = { RadioButton(selected = option == current, onClick = null) },
                    content = {
                        Text(
                            stringResource(
                                when (option) {
                                    DayExcuse.SICK -> R.string.streak_fell_ill
                                    DayExcuse.TRAVEL -> R.string.streak_travelling
                                    null -> R.string.streak_just_missed
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

/** A day that can be given a reason: up to today, with no workout on it ([workoutDays]). */
fun canGiveReason(day: LocalDate, workoutDays: Set<LocalDate>?): Boolean =
    workoutDays != null && day !in workoutDays && !day.isAfter(LocalDate.now())

/** The reason a day already has, from its streak mark. */
fun DayMark?.excuse(): DayExcuse? = when (this) {
    DayMark.SICK -> DayExcuse.SICK
    DayMark.TRAVEL -> DayExcuse.TRAVEL
    else -> null
}
