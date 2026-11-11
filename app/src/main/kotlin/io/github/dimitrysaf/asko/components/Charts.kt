package io.github.dimitrysaf.asko.components

import androidx.compose.ui.graphics.lerp
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import androidx.compose.runtime.key
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import io.github.dimitrysaf.asko.data.stats.BucketSize
import io.github.dimitrysaf.asko.data.stats.Timeline
import java.time.format.DateTimeFormatter

/**
 * One line of an [AreaChart]: a value per point (null skips the point), in [color]. [key] tells
 * the lines apart when some are shown and some hidden; [format] writes a value for the label a
 * tap shows ("12 sets", "0:45", "60 kg").
 */
class ChartSeries(val key: String, val values: List<Double?>, val color: Color, val format: (Double) -> String)

/**
 * A stacked area chart, drawn with Vico and themed from Material 3: each of the [series] is a band
 * on top of the ones before it (the first at the bottom), so the top edge is their total. Bands are
 * solid, in each series' colour; tap a point for each series' own value (tap again to hide it). No
 * guide lines. Points are spaced evenly and the chart scrolls, opening on the latest point at the
 * right; about four of the [labels] name points on the bottom axis. One series is a plain area.
 */
@Composable
fun AreaChart(series: List<ChartSeries>, labels: List<String>, modifier: Modifier = Modifier) {
    val shown = series.filter { s -> s.values.any { it != null } }
    if (shown.isEmpty()) return
    // Remembered before the bands, whose number changes as series are hidden or shown. Whenever
    // the data changes (another range, a series shown), it goes back to the latest point.
    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End,
        autoScroll = Scroll.Absolute.End,
        autoScrollCondition = remember { AutoScrollCondition { _, _ -> true } },
    )
    val background = MaterialTheme.colorScheme.surfaceContainer
    ProvideVicoTheme(rememberM3VicoTheme()) {
        // Each band's top edge is the running total up to it. The topmost is drawn first, so each
        // band below covers the part of it that belongs to the ones beneath.
        val stacked = remember(shown) { stack(shown.map { it.values }) }
        val layers = shown.indices.reversed().map { i ->
            val s = shown[i]
            key(s.key) {
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = remember(s.color) { LineCartesianLayer.LineFill.single(Fill(s.color)) },
                            areaFill = remember(s.color, background) {
                                LineCartesianLayer.AreaFill.single(Fill(lerp(background, s.color, BAND_STRENGTH)))
                            },
                            interpolator = remember { LineCartesianLayer.Interpolator.cubic() },
                        ),
                    ),
                )
            }
        }
        val model = remember(stacked) {
            CartesianChartModel(
                stacked.reversed().map { values ->
                    val points = values.withIndex().mapNotNull { (x, y) -> y?.let { x to it } }
                    LineCartesianLayerModel.build { series(points.map { it.first }, points.map { it.second }) }
                },
            )
        }
        CartesianChartHost(
            chart = rememberCartesianChart(
                *layers.toTypedArray(),
                bottomAxis = rememberBottomAxis(labels),
                marker = rememberMarker(shown),
                // A tap shows the values until the next tap (by default they only show while pressed).
                markerController = CartesianMarkerController.rememberToggleOnTap(),
            ),
            model = model,
            modifier = modifier,
            scrollState = scrollState,
            chartAreaHeight = ChartHeight,
        )
    }
}

/** Running totals: each series' values added to the ones before it (missing values count as 0). */
private fun stack(series: List<List<Double?>>): List<List<Double?>> {
    var below: List<Double?> = emptyList()
    return series.map { values ->
        values.mapIndexed { x, y ->
            val under = below.getOrNull(x)
            if (y == null && under == null) null else (under ?: 0.0) + (y ?: 0.0)
        }.also { below = it }
    }
}

/** How much of a series' colour its band takes (the rest is the card's), so bands stay solid. */
private const val BAND_STRENGTH = 0.55f

/**
 * The bottom axis, without its guide lines: about [LABELS] of the [labels] shown, evenly spaced
 * and ending on the last one. (Vico chooses which points get labels with an item placer; labels
 * themselves are never blank.)
 */
@Composable
private fun rememberBottomAxis(labels: List<String>): HorizontalAxis<Axis.Position.Horizontal.Bottom> {
    val every = (labels.size / LABELS).coerceAtLeast(1)
    val offset = (labels.size - 1).coerceAtLeast(0) % every
    return HorizontalAxis.rememberBottom(
        guideline = null,
        valueFormatter = remember(labels) {
            CartesianValueFormatter { _, value, _ -> labels.getOrNull(value.toInt()) ?: (value.toInt() + 1).toString() }
        },
        itemPlacer = remember(every, offset) { HorizontalAxis.ItemPlacer.aligned(spacing = { every }, offset = { offset }) },
    )
}

/**
 * The values at a tapped point, each series' own (not the running total) in its own words:
 * "12 sets · 140 reps · 45:00". The chart's bands are in reverse order (see [AreaChart]).
 */
@Composable
private fun rememberMarker(shown: List<ChartSeries>) = rememberDefaultCartesianMarker(
    label = rememberTextComponent(style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)),
    valueFormatter = remember(shown) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val x = targets.firstOrNull()?.x?.toInt() ?: return@ValueFormatter ""
            shown.mapNotNull { s -> s.values.getOrNull(x)?.let(s.format) }.joinToString(" · ")
        }
    },
)

private val ChartHeight = 200.dp

/**
 * Bottom-axis labels for a [Timeline], one per bucket: "3 Nov" for days and weeks, "Nov" for
 * months, with the year when the chart spans years. (The charts show only a few of them.)
 */
@Composable
fun timelineLabels(timeline: Timeline): List<String> {
    val locale = LocalConfiguration.current.locales[0]
    return remember(timeline, locale) {
        val starts = timeline.starts
        val spansYears = starts.firstOrNull()?.year != starts.lastOrNull()?.year
        val pattern = when (timeline.size) {
            BucketSize.DAY, BucketSize.WEEK -> "d MMM"
            BucketSize.MONTH -> if (spansYears) "MMM yy" else "MMM"
        }
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        starts.map { it.format(formatter) }
    }
}

/** About how many labels fit under a chart. */
private const val LABELS = 4
