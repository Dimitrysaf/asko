package io.github.dimitrysaf.asko.pages.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.AreaChart
import io.github.dimitrysaf.asko.components.ChartSeries
import io.github.dimitrysaf.asko.components.ChoiceSheet
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.IconCircle
import io.github.dimitrysaf.asko.components.MusclePreview
import io.github.dimitrysaf.asko.components.SheetFilterChip
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.components.timelineLabels
import io.github.dimitrysaf.asko.data.model.Muscle
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.stats.StatsRange
import io.github.dimitrysaf.asko.data.stats.timeline
import io.github.dimitrysaf.asko.lib.format.formatDate
import io.github.dimitrysaf.asko.lib.time.localDate
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Your Pain notes over time: a Body part chip (Anywhere, or one you've written), the worst pain
 * noted each day, week or month over the range you pick (0 when none was), and the latest notes:
 * where, how bad, when, and what you wrote.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PainCard(notes: List<Note>) {
    var range by rememberSaveable { mutableStateOf(StatsRange.QUARTER) }
    var part by rememberSaveable { mutableStateOf<String?>(null) }
    val parts = remember(notes) { notes.mapNotNull { it.bodyPart?.trim()?.takeIf(String::isNotEmpty) }.distinctBy { it.lowercase() }.sorted() }
    val shown = remember(notes, part) { notes.filter { part == null || it.bodyPart?.trim().equals(part, ignoreCase = true) } }
    val resources = LocalResources.current
    ChartCard(stringResource(R.string.stats_pain)) {
        if (parts.isNotEmpty()) {
            SheetFilterChip(stringResource(R.string.pain_body_part), part) { close ->
                ChoiceSheet(
                    title = stringResource(R.string.pain_body_part),
                    anyLabel = stringResource(R.string.pain_any),
                    options = parts,
                    selected = part,
                    onSelect = { part = it },
                    onDismiss = close,
                    anyPreview = { IconCircle(painterResource(R.drawable.ic_asterisk)) },
                    // Where on wger's body, when it names a muscle; the whole body otherwise.
                    preview = { name -> Muscle.of(name).let { MusclePreview(setOfNotNull(it), whole = it == null) } },
                )
            }
        }
        RangePicker(range) { range = it }
        val (line, levels) = remember(shown, range) {
            val today = LocalDate.now()
            val line = timeline(range, today, shown.minOfOrNull { it.createdAt }?.localDate(), WeekFields.of(Locale.getDefault()).firstDayOfWeek)
            val worst = DoubleArray(line.starts.size)
            shown.forEach { note ->
                val i = line.indexOf(note.createdAt.localDate()) ?: return@forEach
                worst[i] = maxOf(worst[i], (note.painLevel ?: 0).toDouble())
            }
            line to worst.toList()
        }
        val color = MaterialTheme.colorScheme.error
        if (levels.all { it <= 0 }) {
            EmptyState(painterResource(R.drawable.ic_notes), stringResource(R.string.pain_none), compact = true)
        } else {
            AreaChart(
                series = listOf(ChartSeries("pain", levels, color) { resources.getString(R.string.note_pain_short, it.roundToInt()) }),
                labels = timelineLabels(line),
            )
        }
        // The latest notes, newest first.
        val latest = shown.asReversed().take(LATEST)
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            latest.forEachIndexed { index, note ->
                val where = note.bodyPart?.trim()?.takeIf(String::isNotEmpty) ?: stringResource(R.string.pain_somewhere)
                val level = note.painLevel?.let { stringResource(R.string.note_pain_short, it) } ?: stringResource(R.string.note_pain_not_rated)
                ListItem(
                    modifier = Modifier.clip(segmentedShapes(index, latest.size).shape),
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    headlineContent = { Text(stringResource(R.string.pain_line, where, level)) },
                    supportingContent = {
                        Text(listOf(note.createdAt.formatDate(), note.text.trim()).filter(String::isNotEmpty).joinToString(" · "), maxLines = 2)
                    },
                )
            }
        }
    }
}

/** How many of the latest notes are listed. */
private const val LATEST = 5
