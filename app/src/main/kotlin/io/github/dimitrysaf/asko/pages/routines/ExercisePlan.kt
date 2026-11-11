package io.github.dimitrysaf.asko.pages.routines

import io.github.dimitrysaf.asko.components.SubjectNotes
import io.github.dimitrysaf.asko.data.model.NoteSubject
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ExerciseThumbnail
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.data.model.supersetRuns
import androidx.compose.material3.ListItemDefaults

// The read-only plan of one exercise: rest, then the set table with values only. The routine page
// shows it, and the routine editor shows it for exercises that aren't open.

internal val ThumbnailSize = 48.dp
internal val CellHeight = 44.dp
internal val SetColumnWidth = 40.dp

/**
 * Where an exercise sits in the list's segmented groups. A superset is a group of its own, under
 * a "Superset" title ([supersetStart] on its first exercise); the exercises between supersets make
 * one group. [groupStart]: a group begins here, after another (more space above it).
 */
internal data class Segment(
    val index: Int,
    val count: Int,
    val superset: Boolean,
    val supersetStart: Boolean,
    val groupStart: Boolean,
    /** Which superset this is (0, 1, …), for its colour; null when not in one. */
    val supersetNumber: Int? = null,
) {
    /** In a superset but not its last exercise: its rest isn't used (the round's rest comes after the last). */
    val restHidden: Boolean get() = superset && index < count - 1
}

internal fun <T> List<T>.segments(linked: (T) -> Boolean): List<Segment> {
    // Supersets stay whole; single exercises next to each other merge into one group.
    val groups = mutableListOf<Pair<IntRange, Boolean>>()
    for (run in supersetRuns(linked)) {
        val superset = run.first != run.last
        val last = groups.lastOrNull()
        if (!superset && last != null && !last.second) {
            groups[groups.lastIndex] = (last.first.first..run.last) to false
        } else {
            groups += run to superset
        }
    }
    val segments = arrayOfNulls<Segment>(size)
    var supersets = 0
    groups.forEachIndexed { g, (range, superset) ->
        val number = if (superset) supersets++ else null
        range.forEachIndexed { i, index ->
            segments[index] = Segment(i, range.count(), superset, superset && i == 0, i == 0 && g > 0, number)
        }
    }
    return segments.map { it!! }
}

/**
 * The title over a superset's exercises: the link icon and "Superset", as a section title
 * ([start]: its indent, in line with the list items' content).
 */
@Composable
internal fun SupersetTitle(modifier: Modifier = Modifier, start: Dp = Dimens.SpaceM) {
    Row(
        modifier.padding(start = start, bottom = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        Icon(painterResource(R.drawable.ic_link), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.superset), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * A superset's background: its exercises share it, and supersets one after another alternate
 * between the tertiary and primary containers (the secondary marks an open or selected exercise).
 */
@Composable
internal fun supersetColor(number: Int): Color =
    if (number % 2 == 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer

/** Space above a group that follows another, setting it apart. */
internal val GroupSpacing = Dimens.SpaceL

/**
 * A routine's (or a done workout's) exercises, read-only, in their segmented groups: supersets
 * under their title. Tapping an exercise's title row opens its page.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExercisePlanList(rows: List<Pair<Exercise, RoutineExercise>>, onExerciseClick: (exerciseId: String) -> Unit) {
    val segments = rows.segments { it.second.supersetWithNext }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { i, (exercise, planned) ->
            val segment = segments[i]
            Column(if (segment.groupStart) Modifier.padding(top = GroupSpacing) else Modifier) {
                if (segment.supersetStart) SupersetTitle()
                ExercisePlanBlock(
                    exercise = exercise,
                    planned = planned,
                    index = segment.index,
                    count = segment.count,
                    showRest = !segment.restHidden,
                    color = segment.supersetNumber?.let { supersetColor(it) } ?: MaterialTheme.colorScheme.surfaceContainer,
                    onClick = { onExerciseClick(exercise.id) },
                )
            }
        }
    }
}

/**
 * A routine page exercise: thumbnail and name, then its plan. Only the title row (thumbnail and
 * name) opens the exercise's page; the plan below isn't a button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExercisePlanBlock(
    exercise: Exercise,
    planned: RoutineExercise,
    index: Int,
    count: Int,
    showRest: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: () -> Unit,
) {
    Surface(shape = segmentedShapes(index, count).shape, color = color) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dimens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                ExerciseThumbnail(exercise, Modifier.size(ThumbnailSize))
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SubjectNotes(NoteSubject.EXERCISE, exercise.id, tonal = true)
            }
            Column(Modifier.padding(start = Dimens.SpaceM, end = Dimens.SpaceM, bottom = Dimens.SpaceM)) {
                if (showRest) RestPreview(planned.restSeconds)
                Column(Modifier.padding(top = if (showRest) Dimens.SpaceS else 0.dp)) {
                    SetTableTitles(planned.timed, planned.repRange)
                    val numbers = planned.sets.setNumbers()
                    planned.sets.forEachIndexed { i, set ->
                        SetPreviewRow(set, numbers[i], planned.timed, planned.repRange, exercise.isBodyweight)
                    }
                }
            }
        }
    }
}

/** The rest, as the rest button reads but without its background. */
@Composable
internal fun RestPreview(restSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MinHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = restContent(restSeconds),
    )
}

/** Timer icon and "Rest · 1:30": the rest button's content, shared with its read-only form. */
@Composable
internal fun restContent(restSeconds: Int): @Composable RowScope.() -> Unit {
    val label = stringResource(R.string.routine_rest_button, restLabel(restSeconds))
    return {
        Icon(painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Dimens.SpaceS))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun restLabel(seconds: Int): String =
    if (seconds == 0) stringResource(R.string.routine_rest_off) else formatSeconds(seconds)

/** The set table's column titles, read-only. */
@Composable
internal fun SetTableTitles(timed: Boolean, repRange: Boolean) {
    Row(
        modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        ColumnLabel(stringResource(R.string.routine_col_set), Modifier.width(SetColumnWidth))
        ColumnLabel(stringResource(repsTitle(timed, repRange)), Modifier.weight(1f))
        ColumnLabel(stringResource(R.string.routine_col_weight), Modifier.weight(1f))
    }
}

internal fun repsTitle(timed: Boolean, repRange: Boolean): Int = when {
    timed -> R.string.routine_col_time
    repRange -> R.string.routine_col_rep_range
    else -> R.string.routine_col_reps
}

@Composable
internal fun ColumnLabel(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** One set row, read-only: the badge, then the values where the pills would be. */
@Composable
internal fun SetPreviewRow(set: PlannedSet, number: Int?, timed: Boolean, repRange: Boolean, bodyweight: Boolean) {
    Row(
        modifier = Modifier.padding(top = Dimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        SetBadge(set.type, number)
        val cell = Modifier.weight(1f)
        when {
            set.type == SetType.FAILURE -> PreviewCell(stringResource(R.string.set_failure_target), cell, muted = true)
            timed -> PreviewCell(set.durationSeconds?.let(::formatSeconds).orEmpty(), cell)
            repRange -> Row(cell, verticalAlignment = Alignment.CenterVertically) {
                PreviewCell(set.reps?.toString().orEmpty(), Modifier.weight(1f))
                RangeDash()
                PreviewCell(set.repsMax?.toString().orEmpty(), Modifier.weight(1f))
            }
            else -> PreviewCell(set.reps?.toString().orEmpty(), cell)
        }
        PreviewCell(weightText(set.weightKg, bodyweight), Modifier.weight(1f))
    }
}

/** The "-" between the two ends of a rep range. */
@Composable
internal fun RangeDash() {
    Text("-", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Dimens.SpaceXs))
}

/** A set cell, read-only: the value alone, where the pill would be ("-" when empty). */
@Composable
internal fun PreviewCell(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Box(modifier.height(CellHeight), contentAlignment = Alignment.Center) {
        Text(
            text.ifEmpty { "-" },
            style = MaterialTheme.typography.titleMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current,
            maxLines = 1,
        )
    }
}

/** "8 kg", "+8 kg" (added to body weight), "BW", or "" for no target. */
@Composable
internal fun weightText(weightKg: Double?, bodyweight: Boolean): String = when (weightKg) {
    null -> ""
    0.0 -> stringResource(R.string.weight_bw)
    else -> stringResource(if (bodyweight) R.string.routine_added_kg_value else R.string.routine_kg_value, formatKg(weightKg))
}
