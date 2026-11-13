package io.github.dimitrysaf.asko.components

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * A light tap when [value] changes (not when first shown), of [type]; with [type] null, Android's
 * own on and off taps for a true or false [value]. Changes from anywhere count, so a toggle feels
 * the same whichever way it was flipped.
 */
@Composable
fun HapticOnChange(value: Any?, type: HapticFeedbackType? = null) {
    val haptics = LocalHapticFeedback.current
    var last by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != last) {
            haptics.performHapticFeedback(type ?: if (value == true) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            last = value
        }
    }
}

/** Material's switch, for a row that flips it, with Android's on and off taps when it flips. */
@Composable
fun HapticSwitch(checked: Boolean, enabled: Boolean = true) {
    HapticOnChange(checked)
    Switch(checked = checked, onCheckedChange = null, enabled = enabled)
}
