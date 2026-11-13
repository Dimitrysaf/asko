package io.github.dimitrysaf.asko.lib.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import kotlin.math.roundToInt

// Distances are stored in metres and shown in kilometres or miles, as Settings › Distance unit says.

/** The unit distances are shown and typed in. */
enum class DistanceUnit(val meters: Double) {
    KM(1000.0),
    MI(1609.344),
}

/** The distance unit in Settings, for the whole app. */
val LocalDistanceUnit = staticCompositionLocalOf { DistanceUnit.KM }

/** "5", "5.25": [meters] in this unit, two decimals at most. */
fun DistanceUnit.distanceNumber(meters: Double): String = formatAmount(meters / this.meters)

/** A typed distance in this unit, in metres. */
fun DistanceUnit.metersOf(value: Double): Double = value * meters

/** "km" or "mi". */
@Composable
fun distanceUnitSymbol(unit: DistanceUnit = LocalDistanceUnit.current): String =
    stringResource(if (unit == DistanceUnit.MI) R.string.unit_mi else R.string.unit_km)

/** "5.2 km" or "3.1 mi". */
@Composable
fun distanceValue(meters: Double, unit: DistanceUnit = LocalDistanceUnit.current): String =
    stringResource(distanceValueRes(unit), unit.distanceNumber(meters))

/** The "5.2 km" / "3.1 mi" string for [unit], for places outside composition. */
fun distanceValueRes(unit: DistanceUnit): Int = if (unit == DistanceUnit.MI) R.string.distance_mi_value else R.string.distance_km_value

/** Seconds per km (or mi) for [meters] in [seconds]; null when either is missing or zero. */
fun DistanceUnit.paceSeconds(meters: Double?, seconds: Int?): Int? {
    if (meters == null || seconds == null || meters <= 0 || seconds <= 0) return null
    return (seconds / (meters / this.meters)).roundToInt()
}

/** "5:12 /km" or "8:22 /mi": the time per km (or mi). */
@Composable
fun paceValue(secondsPerUnit: Int, unit: DistanceUnit = LocalDistanceUnit.current): String {
    val minutes = secondsPerUnit / 60
    val seconds = secondsPerUnit % 60
    return stringResource(if (unit == DistanceUnit.MI) R.string.pace_mi_value else R.string.pace_km_value, minutes, seconds)
}
