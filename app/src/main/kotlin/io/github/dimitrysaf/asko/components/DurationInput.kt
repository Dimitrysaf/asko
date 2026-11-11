/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified for Asko: Material 3's TimeInput (androidx.compose.material3 1.5.0-alpha22,
 * TimePicker.kt) copied and changed to enter a duration instead of a time of day. The fields are
 * minutes (0–99) and seconds (0–59) with their own labels; the AM/PM toggle and the clock-dial
 * syncing are removed. Layout, sizes, colors, typography and input handling are TimeInput's.
 */
package io.github.dimitrysaf.asko.components

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import java.util.Locale

/** Which box of a [DurationInput] is being typed in. */
enum class DurationField { Minutes, Seconds }

/** What a [DurationInput] holds; read [totalSeconds] when the user confirms. */
@Stable
class DurationInputState(minutes: Int, seconds: Int) {
    var minutes by mutableIntStateOf(minutes)
    var seconds by mutableIntStateOf(seconds)
    var selection by mutableStateOf(DurationField.Minutes)

    val isSecondsValid: Boolean get() = seconds in 0..MaxSeconds

    /** The duration, or null while the seconds are out of range. */
    val totalSeconds: Int? get() = if (isSecondsValid) minutes * 60 + seconds else null

    companion object {
        val Saver: Saver<DurationInputState, *> = listSaver(
            save = { listOf(it.minutes, it.seconds, it.selection.ordinal) },
            restore = { DurationInputState(it[0], it[1]).apply { selection = DurationField.entries[it[2]] } },
        )
    }
}

@Composable
fun rememberDurationInputState(initialSeconds: Int): DurationInputState =
    rememberSaveable(saver = DurationInputState.Saver) {
        val total = initialSeconds.coerceIn(0, MaxMinutes * 60 + MaxSeconds)
        DurationInputState(total / 60, total % 60)
    }

/**
 * Material 3's TimeInput, for a duration: two boxes, minutes and seconds, as in a
 * TimePickerDialog. Put it in a [androidx.compose.material3.TimePickerDialog].
 */
@Composable
fun DurationInput(
    state: DurationInputState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
) {
    var minutesValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(state.minutes.twoDigits()))
    }
    var secondsValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(state.seconds.twoDigits()))
    }

    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        val textStyle = MaterialTheme.typography.displayMedium.copy(
            textAlign = TextAlign.Center,
            color = colors.timeSelectorSelectedContentColor,
        )
        val errorHandler = rememberDurationErrorHandler()

        CompositionLocalProvider(
            LocalTextStyle provides textStyle,
            // Always display the input from left to right.
            LocalLayoutDirection provides LayoutDirection.Ltr,
        ) {
            Row {
                DurationTextField(
                    modifier = Modifier.onKeyEvent { event ->
                        // A digit typed after two minute digits moves on to the seconds.
                        val switchFocus = event.utf16CodePoint in '0'.code..'9'.code &&
                            minutesValue.selection.start == 2 &&
                            minutesValue.text.length == 2
                        if (switchFocus) state.selection = DurationField.Seconds
                        false
                    },
                    value = minutesValue,
                    onValueChange = { newValue ->
                        durationInputOnChange(DurationField.Minutes, state, newValue, minutesValue, errorHandler) {
                            minutesValue = it
                        }
                    },
                    state = state,
                    field = DurationField.Minutes,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onNext = { state.selection = DurationField.Seconds }),
                    colors = colors,
                )
                DisplaySeparator(Modifier.size(DisplaySeparatorWidth, FieldHeight))
                DurationTextField(
                    modifier = Modifier,
                    value = secondsValue,
                    onValueChange = { newValue ->
                        durationInputOnChange(DurationField.Seconds, state, newValue, secondsValue, errorHandler) {
                            secondsValue = it
                        }
                    },
                    state = state,
                    field = DurationField.Seconds,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(),
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun DisplaySeparator(modifier: Modifier) {
    val style = LocalTextStyle.current.copy(
        textAlign = TextAlign.Center,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
    Box(modifier = modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Text(text = ":", color = MaterialTheme.colorScheme.onSurface, style = style)
    }
}

/** The box shown while the other field is being typed in; tapping it switches to this one. */
@Composable
private fun DurationSelector(
    modifier: Modifier,
    value: Int,
    state: DurationInputState,
    field: DurationField,
    colors: TimePickerColors,
    isValid: Boolean,
) {
    val selected = state.selection == field
    val selectorDescription = stringResource(
        if (field == DurationField.Minutes) R.string.duration_select_minutes else R.string.duration_select_seconds,
    )
    val containerColor = when {
        !isValid -> MaterialTheme.colorScheme.errorContainer
        selected -> colors.timeSelectorSelectedContainerColor
        else -> colors.timeSelectorUnselectedContainerColor
    }
    val contentColor = when {
        !isValid -> MaterialTheme.colorScheme.onErrorContainer
        selected -> colors.timeSelectorSelectedContentColor
        else -> colors.timeSelectorUnselectedContentColor
    }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            role = Role.RadioButton
            contentDescription = selectorDescription
        },
        onClick = { if (field != state.selection) state.selection = field },
        selected = selected,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        val valueDescription = stringResource(
            if (field == DurationField.Minutes) R.string.duration_minutes_count else R.string.duration_seconds_count,
            value,
        )
        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.semantics { contentDescription = valueDescription },
                text = value.twoDigits(),
                color = contentColor,
            )
        }
    }
}

private fun durationInputOnChange(
    field: DurationField,
    state: DurationInputState,
    value: TextFieldValue,
    prevValue: TextFieldValue,
    errorHandler: () -> Unit,
    onNewValue: (TextFieldValue) -> Unit,
) {
    if (value.text == prevValue.text) {
        // just a selection change
        onNewValue(value)
        return
    }

    if (value.text.isEmpty()) {
        if (field == DurationField.Minutes) state.minutes = 0 else state.seconds = 0
        onNewValue(value.copy(text = ""))
        return
    }

    try {
        val newValue = if (value.text.length == 3 && value.selection.start == 1) {
            value.text[0].digitToInt()
        } else {
            value.text.toInt()
        }

        if (newValue <= MaxValueForTextField) {
            if (field == DurationField.Minutes) state.minutes = newValue else state.seconds = newValue
            onNewValue(if (value.text.length <= 2) value else value.copy(text = value.text[0].toString()))
        } else {
            errorHandler()
        }
    } catch (_: NumberFormatException) {
    } catch (_: IllegalArgumentException) {
        errorHandler()
    }
}

@Composable
private fun SupportingText(modifier: Modifier, field: DurationField, isValid: Boolean) {
    val text = stringResource(
        when {
            !isValid -> R.string.duration_seconds_error
            field == DurationField.Minutes -> R.string.duration_minutes
            else -> R.string.duration_seconds
        },
    )
    val color = if (!isValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        modifier = modifier
            .padding(top = SupportLabelTop)
            .then(
                if (isValid) Modifier.clearAndSetSemantics {}
                else Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ),
        text = text,
        color = color,
        minLines = 2,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun DurationTextField(
    modifier: Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    state: DurationInputState,
    field: DurationField,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    colors: TimePickerColors,
) {
    val focusRequester = remember { FocusRequester() }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.timeSelectorSelectedContainerColor,
        unfocusedContainerColor = colors.timeSelectorSelectedContainerColor,
        focusedTextColor = colors.timeSelectorSelectedContentColor,
        errorLabelColor = MaterialTheme.colorScheme.onErrorContainer,
        errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    )
    val selected = field == state.selection
    val isValid = field == DurationField.Minutes || state.isSecondsValid
    val textColor = if (isValid) colors.timeSelectorSelectedContentColor else MaterialTheme.colorScheme.error

    Column(modifier = modifier.width(IntrinsicSize.Min)) {
        if (!selected) {
            DurationSelector(
                modifier = Modifier.size(FieldWidth, FieldHeight),
                value = if (field == DurationField.Minutes) state.minutes else state.seconds,
                state = state,
                field = field,
                colors = colors,
                isValid = isValid,
            )
        }

        val fieldDescription = stringResource(
            if (field == DurationField.Minutes) R.string.duration_minutes_field else R.string.duration_seconds_field,
        )
        val interactionSource = remember { MutableInteractionSource() }

        Box(Modifier.visible(selected)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .size(FieldWidth, FieldHeight)
                    .semantics {
                        contentDescription = fieldDescription
                        maxTextLength = 2
                    },
                interactionSource = interactionSource,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = LocalTextStyle.current.copy(color = textColor),
                enabled = true,
                singleLine = true,
                cursorBrush = Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.10f to Color.Transparent,
                    0.10f to MaterialTheme.colorScheme.primary,
                    0.90f to MaterialTheme.colorScheme.primary,
                    0.90f to Color.Transparent,
                    1.00f to Color.Transparent,
                ),
            ) {
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value.text,
                    visualTransformation = VisualTransformation.None,
                    innerTextField = it,
                    isError = !isValid,
                    singleLine = true,
                    colors = textFieldColors,
                    enabled = true,
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(0.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = !isValid,
                            interactionSource = interactionSource,
                            shape = MaterialTheme.shapes.small,
                            colors = textFieldColors,
                        )
                    },
                )
            }
        }

        SupportingText(
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            field = field,
            isValid = isValid,
        )
    }

    LaunchedEffect(state.selection) {
        if (state.selection == field) focusRequester.requestFocus()
    }
}

/** Keeps the box's state (and focus) while taking no space, like TimeInput's hidden field. */
private fun Modifier.visible(visible: Boolean) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    if (!visible) layout(0, 0) {} else layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** A rejected keystroke: a haptic and the "invalid key" click, as in TimeInput. */
@Composable
private fun rememberDurationErrorHandler(): () -> Unit {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    return remember(haptics, audioManager) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_INVALID, 0.5f)
        }
    }
}

private fun Int.twoDigits() = String.format(Locale.getDefault(), "%02d", this)

private const val MaxMinutes = 99
private const val MaxSeconds = 59
private const val MaxValueForTextField = 99
private val FieldWidth = 96.dp
private val FieldHeight = 72.dp
private val DisplaySeparatorWidth = 24.dp
private val SupportLabelTop = 7.dp
