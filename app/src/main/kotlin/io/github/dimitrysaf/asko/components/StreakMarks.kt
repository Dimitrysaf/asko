package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.DayMark

/** A day's streak icon: fire (worked out), bed (rest day), green slime (sick), plane (travelling). */
@Composable
fun DayMarkIcon(mark: DayMark, modifier: Modifier = Modifier, size: Dp = 24.dp, alpha: Float = 1f, tint: Color = dayMarkColor(mark)) {
    Icon(dayMarkPainter(mark), contentDescription = null, tint = tint.copy(alpha = tint.alpha * alpha), modifier = modifier.size(size))
}

@Composable
fun dayMarkPainter(mark: DayMark): Painter = painterResource(
    when (mark) {
        DayMark.WORKOUT -> R.drawable.ic_fire
        DayMark.REST -> R.drawable.ic_bed
        DayMark.SICK -> R.drawable.ic_slime
        DayMark.TRAVEL -> R.drawable.ic_travel
    },
)

/** Fire is orange and slime green, whatever the theme; the bed and the plane take the theme's colours. */
@Composable
fun dayMarkColor(mark: DayMark): Color = when (mark) {
    DayMark.WORKOUT -> FireOrange
    DayMark.REST -> MaterialTheme.colorScheme.secondary
    DayMark.SICK -> SlimeGreen
    DayMark.TRAVEL -> MaterialTheme.colorScheme.tertiary
}

/** "🔥 12 day streak": the fire and the streak's length. */
@Composable
fun StreakCount(days: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        DayMarkIcon(DayMark.WORKOUT, size = 20.dp)
        Text(pluralStringResource(R.plurals.streak_days, days, days), style = MaterialTheme.typography.titleSmall)
    }
}

private val FireOrange = Color(0xFFFF7043)
private val SlimeGreen = Color(0xFF66BB6A)
