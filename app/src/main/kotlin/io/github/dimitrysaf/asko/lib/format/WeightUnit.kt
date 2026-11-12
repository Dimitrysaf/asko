package io.github.dimitrysaf.asko.lib.format

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * The unit weights are shown and typed in. Everything is stored in kilos; only what's on screen
 * changes, so switching units never changes a logged weight.
 */
enum class WeightUnit {
    KG,
    LB;

    /** [kg] in this unit. */
    fun fromKg(kg: Double): Double = if (this == LB) kg / KG_PER_LB else kg

    /** A [value] in this unit, in kilos (for storing). */
    fun toKg(value: Double): Double = if (this == LB) value * KG_PER_LB else value

    /** "8", "8.5", "135": [kg] in this unit, two decimals at most, no trailing zeros. */
    fun number(kg: Double): String = formatAmount(fromKg(kg))

    /** Common weights for the weight sheet, in kilos: dumbbell pairs, plate steps, bars. */
    val presetsKg: List<Double>
        get() = when (this) {
            KG -> KgPresets
            LB -> LbPresets.map(::toKg)
        }

    /** An empty barbell. */
    val barKg: Double get() = if (this == LB) toKg(45.0) else 20.0

    companion object {
        const val KG_PER_LB = 0.45359237
    }
}

private val KgPresets = listOf(
    1.0, 2.0, 2.5, 4.0, 5.0, 6.0, 7.5, 8.0, 10.0, 12.0, 12.5, 14.0, 15.0, 16.0, 17.5, 20.0,
    22.5, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0,
)

private val LbPresets = listOf(
    2.5, 5.0, 10.0, 12.5, 15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0,
    75.0, 80.0, 90.0, 95.0, 100.0, 115.0, 135.0, 155.0, 185.0, 225.0,
)

/** The same weight, give or take the rounding a unit conversion leaves. */
fun sameWeight(a: Double?, b: Double?): Boolean =
    if (a == null || b == null) a == b else abs(a - b) < 0.001

/** 8, 8.5, 61.23 (two decimals at most, no trailing zeros). */
fun formatAmount(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** The unit the app shows weights in, provided at the top of the app from Settings. */
val LocalWeightUnit = staticCompositionLocalOf { WeightUnit.KG }

/** "kg" or "lb". */
@Composable
fun weightUnitSymbol(unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(if (unit == WeightUnit.LB) R.string.unit_lb else R.string.unit_kg)

/** "8 kg" or "18 lb"; with [added], "+8 kg" (added to body weight). */
@Composable
fun weightValue(kg: Double, added: Boolean = false, unit: WeightUnit = LocalWeightUnit.current): String =
    stringResource(valueRes(unit, added), unit.number(kg))

/** [weightValue] outside composition (chart labels), with [shown] the number already formatted. */
fun Resources.weightValue(shown: String, unit: WeightUnit): String = getString(valueRes(unit, added = false), shown)

private fun valueRes(unit: WeightUnit, added: Boolean): Int = when (unit) {
    WeightUnit.KG -> if (added) R.string.routine_added_kg_value else R.string.routine_kg_value
    WeightUnit.LB -> if (added) R.string.routine_added_lb_value else R.string.routine_lb_value
}
