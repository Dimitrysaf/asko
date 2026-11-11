package io.github.dimitrysaf.asko.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import io.github.dimitrysaf.asko.lib.format.formatClock
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** Live "time since [since]" clock that ticks every second. */
@Composable
fun ElapsedTimer(
    since: Instant,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val elapsed by produceState(Duration.between(since, Instant.now()), since) {
        while (true) {
            value = Duration.between(since, Instant.now())
            delay(1_000)
        }
    }
    Text(elapsed.formatClock(), modifier = modifier, style = style)
}
