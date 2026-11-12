package io.github.dimitrysaf.asko.pages.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.lib.format.formatClock
import java.time.Duration

/** Each set's number: 1, 2, 3… for normal sets, null for warm-ups, drops, failure and finisher sets. */
fun List<PlannedSet>.setNumbers(): List<Int?> {
    var n = 0
    return map { if (it.type == SetType.NORMAL) ++n else null }
}

/** Bodyweight moves log only the added weight ("+kg"); everything else the total weight moved. */
val Exercise.isBodyweight: Boolean get() = equipment.any { it.replace(" ", "").equals("Bodyweight", ignoreCase = true) }

@Composable
fun setTypeName(type: SetType): String = stringResource(
    when (type) {
        SetType.NORMAL -> R.string.set_type_normal
        SetType.WARMUP -> R.string.set_type_warmup
        SetType.DROP -> R.string.set_type_drop
        SetType.FAILURE -> R.string.set_type_failure
        SetType.FINISHER -> R.string.set_type_finisher
    },
)

/** What a set type is for, shown in the set type sheet. */
@Composable
fun setTypeDescription(type: SetType): String = stringResource(
    when (type) {
        SetType.NORMAL -> R.string.set_type_normal_desc
        SetType.WARMUP -> R.string.set_type_warmup_desc
        SetType.DROP -> R.string.set_type_drop_desc
        SetType.FAILURE -> R.string.set_type_failure_desc
        SetType.FINISHER -> R.string.set_type_finisher_desc
    },
)

/**
 * A set's badge: its number for normal sets, a letter (W, D, F, Fn) in its own expressive shape and
 * colour for the others. Tappable when [onClick] is given (opens the set type menu).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SetBadge(type: SetType, number: Int?, modifier: Modifier = Modifier, size: Dp = 40.dp, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val (shape: Shape, container: Color, content: Color) = when (type) {
        SetType.NORMAL -> Triple(CircleShape, colors.surfaceContainerHighest, colors.onSurface)
        SetType.WARMUP -> Triple(MaterialShapes.Sunny.toShape(), colors.tertiaryContainer, colors.onTertiaryContainer)
        SetType.DROP -> Triple(MaterialShapes.Cookie4Sided.toShape(), colors.secondaryContainer, colors.onSecondaryContainer)
        SetType.FAILURE -> Triple(MaterialShapes.SoftBurst.toShape(), colors.errorContainer, colors.onErrorContainer)
        SetType.FINISHER -> Triple(MaterialShapes.Clover4Leaf.toShape(), colors.primaryContainer, colors.onPrimaryContainer)
    }
    val label = when (type) {
        SetType.NORMAL -> number?.toString().orEmpty()
        SetType.WARMUP -> stringResource(R.string.set_badge_warmup)
        SetType.DROP -> stringResource(R.string.set_badge_drop)
        SetType.FAILURE -> stringResource(R.string.set_badge_failure)
        SetType.FINISHER -> stringResource(R.string.set_badge_finisher)
    }
    Box(
        modifier = modifier
            .size(size)
            .background(container, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (size < 32.dp) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

/** "1:30" for a rest or a timed set. */
fun formatSeconds(seconds: Int): String = Duration.ofSeconds(seconds.toLong()).formatClock()
