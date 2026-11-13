package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.DayMark
import io.github.dimitrysaf.asko.data.model.Streak
import io.github.dimitrysaf.asko.lib.share.ShareImage
import java.time.LocalDate

/**
 * What the streak means, from tapping the streak item: how long it is, then what keeps it going
 * (workouts, rest days), what pauses it (sick, travelling) and what ends it. Share sends a picture
 * of it: the count and this week's days.
 */
@Composable
fun StreakDialog(streak: Streak, onDismiss: () -> Unit) {
    var sharing by remember { mutableStateOf(false) }
    val days = pluralStringResource(R.plurals.streak_days, streak.current, streak.current)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { DayMarkIcon(DayMark.WORKOUT, size = 32.dp) },
        title = { Text(days) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                Rule({ DayMarkIcon(DayMark.WORKOUT, size = RuleIcon) }, stringResource(R.string.streak_about_workout))
                Rule({ DayMarkIcon(DayMark.REST, size = RuleIcon) }, stringResource(R.string.streak_about_rest))
                Rule({ DayMarkIcon(DayMark.SICK, size = RuleIcon) }, stringResource(R.string.streak_about_pause))
                Rule(
                    { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(RuleIcon)) },
                    stringResource(R.string.streak_about_end),
                )
            }
        },
        confirmButton = { TextButton(onClick = { sharing = true }) { Text(stringResource(R.string.action_share)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
    // Share: a picture of the streak, drawn out of sight, to the share sheet.
    if (sharing) {
        ShareImage(name = days, onDone = { sharing = false }) {
            ShareFrame(title = days, lines = emptyList()) {
                // This week's days, as the Routines tab shows them.
                val today = LocalDate.now()
                val worked = remember(streak) {
                    (0L..13L).map { today.minusDays(it) }.filter { streak.markOf(it) == DayMark.WORKOUT }.toSet()
                }
                WeekStreak(
                    workoutDays = worked,
                    onDayClick = {},
                    marks = streak::markOf,
                    counts = { day -> streak.counts(day) },
                )
            }
        }
    }
}

/** One line of what the streak means: its icon, then the words. */
@Composable
private fun Rule(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        icon()
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private val RuleIcon = 24.dp
