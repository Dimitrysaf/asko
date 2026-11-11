package io.github.dimitrysaf.asko.pages.workout

import java.time.Instant
import io.github.dimitrysaf.asko.lib.format.formatDate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.stats.PersonalRecord
import io.github.dimitrysaf.asko.data.stats.RecordKind
import io.github.dimitrysaf.asko.pages.routines.formatKg
import io.github.dimitrysaf.asko.pages.routines.formatSeconds
import kotlin.math.roundToInt

/**
 * Records, as a segmented list: a trophy, the exercise, and what the record is ("Heaviest: 100 kg,
 * was 95 kg"), with when it was set if [dates] are given (one per record).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecordList(
    records: List<PersonalRecord>,
    library: Map<String, Exercise>,
    modifier: Modifier = Modifier,
    dates: List<Instant>? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        records.forEachIndexed { index, record ->
            ListItem(
                modifier = Modifier.clip(segmentedShapes(index, records.size).shape),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                leadingContent = {
                    Box(
                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_trophy),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                headlineContent = { Text(library[record.exerciseId]?.name.orEmpty()) },
                supportingContent = {
                    val line = stringResource(
                        R.string.record_line,
                        recordName(record.kind),
                        recordValue(record.kind, record.value),
                        recordValue(record.kind, record.previous),
                    )
                    Text(dates?.getOrNull(index)?.let { "$line · ${it.formatDate()}" } ?: line)
                },
            )
        }
    }
}

/** "Heaviest", "Most reps", "Estimated 1RM", "Longest hold". */
@Composable
fun recordName(kind: RecordKind): String = stringResource(
    when (kind) {
        RecordKind.HEAVIEST -> R.string.record_heaviest
        RecordKind.MOST_REPS -> R.string.record_most_reps
        RecordKind.ONE_REP_MAX -> R.string.record_one_rep_max
        RecordKind.LONGEST_HOLD -> R.string.record_longest_hold
    },
)

/** "100 kg", "12 reps", "112.5 kg" (estimates to the nearest half kilo), "1:30". */
@Composable
fun recordValue(kind: RecordKind, value: Double): String = when (kind) {
    RecordKind.HEAVIEST -> stringResource(R.string.routine_kg_value, formatKg(value))
    RecordKind.ONE_REP_MAX -> stringResource(R.string.routine_kg_value, formatKg((value * 2).roundToInt() / 2.0))
    RecordKind.MOST_REPS -> pluralStringResource(R.plurals.record_reps, value.toInt(), value.toInt())
    RecordKind.LONGEST_HOLD -> formatSeconds(value.toInt())
}
