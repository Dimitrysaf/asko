package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.segmentedShapes
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.lib.format.sameWeight
import io.github.dimitrysaf.asko.lib.format.weightValue
import io.github.dimitrysaf.asko.lib.notify.RoutineUpdate

/** One way a workout went differently from its routine. */
sealed interface RoutineChange {
    data class Added(val exerciseId: String) : RoutineChange
    data class Removed(val exerciseId: String) : RoutineChange
    data class SetCount(val exerciseId: String, val from: Int, val to: Int) : RoutineChange

    /** Its weights changed: [from] and [to] when every set has the one weight, else null. */
    data class Weight(val exerciseId: String, val from: Double?, val to: Double?) : RoutineChange

    /** Its reps changed: [from] and [to] when every set has the same reps, else null. */
    data class Reps(val exerciseId: String, val from: Int?, val to: Int?) : RoutineChange
    data object Order : RoutineChange

    /** Something else (supersets, a set's type…). */
    data object Other : RoutineChange
}

/** What changes from [before] to [after], exercise by exercise (matched by exercise). */
fun routineChanges(before: List<RoutineExercise>, after: List<RoutineExercise>): List<RoutineChange> {
    val beforeIds = before.map { it.exerciseId }
    val afterIds = after.map { it.exerciseId }
    val changes = mutableListOf<RoutineChange>()
    after.forEach { a ->
        val b = before.firstOrNull { it.exerciseId == a.exerciseId }
        if (b == null) {
            changes += RoutineChange.Added(a.exerciseId)
            return@forEach
        }
        if (b.sets.size != a.sets.size) {
            changes += RoutineChange.SetCount(a.exerciseId, b.sets.size, a.sets.size)
            return@forEach
        }
        val bw = b.sets.map { it.weightKg }
        val aw = a.sets.map { it.weightKg }
        if (bw.zip(aw).any { (x, y) -> !sameWeight(x, y) }) {
            changes += RoutineChange.Weight(a.exerciseId, bw.distinct().singleOrNull(), aw.distinct().singleOrNull())
        }
        val br = b.sets.map { it.reps }
        val ar = a.sets.map { it.reps }
        if (br != ar) changes += RoutineChange.Reps(a.exerciseId, br.distinct().singleOrNull(), ar.distinct().singleOrNull())
    }
    beforeIds.filterNot { it in afterIds }.forEach { changes += RoutineChange.Removed(it) }
    val kept = afterIds.filter { it in beforeIds }
    if (kept != beforeIds.filter { it in afterIds }) changes += RoutineChange.Order
    if (changes.isEmpty()) changes += RoutineChange.Other
    return changes
}

/**
 * On the finish screen, when the workout went differently from its routine: "Update Morning?",
 * what would change (added and removed exercises, sets, weights, reps, order), and Update
 * routine, which makes the routine match. Once updated, it says so.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineUpdateCard(update: RoutineUpdate, library: Map<String, Exercise>, updated: Boolean, onUpdate: () -> Unit) {
    val changes = routineChanges(update.before.exercises, update.after.exercises)
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
    ) {
        Column(Modifier.padding(Dimens.SpaceM), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                Text(stringResource(R.string.routine_update_title, update.before.name), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.routine_update_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                changes.forEachIndexed { index, change ->
                    val (icon, text) = describe(change, library)
                    ListItem(
                        modifier = Modifier.clip(segmentedShapes(index, changes.size).shape),
                        colors = ListItemDefaults.colors(containerColor = colors.surfaceContainerHigh),
                        leadingContent = { Icon(icon, contentDescription = null) },
                        headlineContent = { Text(text) },
                    )
                }
            }
            AnimatedContent(
                targetState = updated,
                transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.defaultEffectsSpec()) },
                label = "routine updated",
            ) { done ->
                if (done) {
                    Row(
                        Modifier.fillMaxWidth().height(ButtonDefaults.MinHeight),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Dimens.SpaceS))
                        Text(stringResource(R.string.routine_updated, update.before.name), color = colors.primary, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    FilledTonalButton(onClick = onUpdate, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.routine_update_action))
                    }
                }
            }
        }
    }
}

/** A change's icon and words: "Added Plank", "Bench Press: 60 kg → 62.5 kg". */
@Composable
private fun describe(change: RoutineChange, library: Map<String, Exercise>): Pair<Painter, String> {
    fun name(id: String) = library[id]?.name.orEmpty()
    val edit = rememberVectorPainter(Icons.Filled.Edit)
    return when (change) {
        is RoutineChange.Added -> rememberVectorPainter(Icons.Filled.Add) to stringResource(R.string.routine_change_added, name(change.exerciseId))
        is RoutineChange.Removed -> painterResource(R.drawable.ic_remove) to stringResource(R.string.routine_change_removed, name(change.exerciseId))
        is RoutineChange.SetCount -> edit to stringResource(R.string.routine_change_sets, name(change.exerciseId), change.from, change.to)
        is RoutineChange.Weight -> edit to if (change.from != null && change.to != null) {
            stringResource(R.string.routine_change_from_to, name(change.exerciseId), weightText(change.from), weightText(change.to))
        } else {
            stringResource(R.string.routine_change_weights, name(change.exerciseId))
        }
        is RoutineChange.Reps -> edit to if (change.from != null && change.to != null) {
            stringResource(R.string.routine_change_reps_from_to, name(change.exerciseId), change.from, change.to)
        } else {
            stringResource(R.string.routine_change_reps, name(change.exerciseId))
        }
        RoutineChange.Order -> painterResource(R.drawable.ic_swap) to stringResource(R.string.routine_change_order)
        RoutineChange.Other -> edit to stringResource(R.string.routine_change_other)
    }
}

/** "60 kg", or "BW" for body weight, "–" for none. */
@Composable
private fun weightText(kg: Double): String = if (kg == 0.0) stringResource(R.string.weight_bw) else weightValue(kg)
