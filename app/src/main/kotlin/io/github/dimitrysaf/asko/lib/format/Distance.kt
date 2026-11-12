package io.github.dimitrysaf.asko.lib.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import kotlin.math.roundToInt

// Distances are stored in metres and shown in kilometres, or in miles with the weight unit in pounds.

private const val METERS_PER_KM = 1000.0
private const val METERS_PER_MILE = 1609.344

/** Metres in one of this unit's distance units: a kilometre, or a mile for pounds. */
val WeightUnit.metersPerDistanceUnit: Double get() = if (this == WeightUnit.LB) METERS_PER_MILE else METERS_PER_KM

/** "5", "5.25": [meters] in km (or mi), two decimals at most. */
fun WeightUnit.distanceNumber(meters: Double): String = formatAmount(meters / metersPerDistanceUnit)

/** A typed distance in km (or mi), in metres. */
fun WeightUnit.metersOf(value: Double): Double = value * metersPerDistanceUnit

/** "km" or "mi". */
@Composable
fun distanceUnitSymbol(unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(if (unit == WeightUnit.LB) R.string.unit_mi else R.string.unit_km)

/** "5.2 km" or "3.1 mi". */
@Composable
fun distanceValue(meters: Double, unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(if (unit == WeightUnit.LB) R.string.distance_mi_value else R.string.distance_km_value, unit.distanceNumber(meters))

/** Seconds per km (or mi) for [meters] in [seconds]; null when either is missing or zero. */
fun WeightUnit.paceSeconds(meters: Double?, seconds: Int?): Int? {
    if (meters == null || seconds == null || meters <= 0 || seconds <= 0) return null
    return (seconds / (meters / metersPerDistanceUnit)).roundToInt()
}

/** "5:12 /km" or "8:22 /mi": the time per km (or mi). */
@Composable
fun paceValue(secondsPerUnit: Int, unit: WeightUnit = LocalWeightUnit.current): String {
    val minutes = secondsPerUnit / 60
    val seconds = secondsPerUnit % 60
    return stringResource(if (unit == WeightUnit.LB) R.string.pace_mi_value else R.string.pace_km_value, minutes, seconds)
}
