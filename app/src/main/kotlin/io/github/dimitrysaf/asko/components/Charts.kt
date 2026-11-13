package io.github.dimitrysaf.asko.components

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.stats.BucketSize
import io.github.dimitrysaf.asko.data.stats.Timeline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * One measure over a [Timeline], the way Google Health draws it: a big number on top, its unit
 * small, and what it's about under it ([headline] and [headlineLabel] while nothing is tapped;
 * "No data" when there's nothing), then [values] (one per bucket, null for none) as capsules in
 * [color] on a bare chart, values on the right. With [line], a line with dots instead, for
 * something that's always there (body weight), its axis fitted to the values.
 *
 * Tap a bar for its own value ([format]: "12 sets") and its date in the headline, marked with a
 * line down to a ring; tap again to go back. The whole range fits the card, nothing scrolls.
 */
@Composable
fun MetricChart(
    values: List<Double?>,
    timeline: Timeline,
    color: Color,
    format: (Double) -> String,
    headline: String?,
    headlineLabel: String,
    modifier: Modifier = Modifier,
    line: Boolean = false,
) {
    val hasData = if (line) values.any { it != null } else values.any { (it ?: 0.0) > 0 }
    var selected by remember(values, timeline) { mutableStateOf<Int?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    val picked = selected?.let { i -> values.getOrNull(i)?.let { i to it } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Column {
            ChartValue(
                when {
                    picked != null -> format(picked.second)
                    hasData && headline != null -> headline
                    else -> stringResource(R.string.chart_no_data)
                },
            )
            Text(
                text = if (picked != null) bucketTitle(timeline, picked.first, locale) else headlineLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BareChart(values, timeline, color, hasData, line && hasData) { selected = it }
    }
}

/**
 * A big value with its words small: in "60 kg" or "12 sets" the number is [style], the unit
 * [unitStyle]. Text with no number in it ("No data") is all [style].
 */
@Composable
fun ChartValue(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    unitStyle: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val annotated = remember(text, unitStyle) {
        val numbers = Number.findAll(text).toList()
        if (numbers.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                val unit = SpanStyle(fontSize = unitStyle.fontSize, fontWeight = unitStyle.fontWeight)
                var at = 0
                numbers.forEach { match ->
                    if (match.range.first > at) addStyle(unit, at, match.range.first)
                    at = match.range.last + 1
                }
                if (at < text.length) addStyle(unit, at, text.length)
            }
        }
    }
    Text(annotated, modifier, style = style, color = color, maxLines = 1)
}

/** A number as people write it: "12", "1,240", "62.5", "1:30", "1 240". */
private val Number = Regex("""\d+(?:[.,:'  ]\d+)*""")

/**
 * The chart itself, drawn with Vico: capsule bars (or a dotted line), no grid, a few values on
 * the right and a few dates under it. Tapping reports the bar picked to [onSelect] (null: none).
 */
@Composable
private fun BareChart(
    values: List<Double?>,
    timeline: Timeline,
    color: Color,
    hasData: Boolean,
    line: Boolean,
    onSelect: (Int?) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val (labels, every) = remember(timeline, locale) { axisLabels(timeline, locale) }
    val offset = (labels.size - 1).coerceAtLeast(0) % every
    val faint = MaterialTheme.colorScheme.onSurfaceVariant
    val ring = MaterialTheme.colorScheme.surfaceContainer
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = faint)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Every bar gets an equal slot across the card; a bar is about half its slot, capped.
        val slot = (maxWidth - AxisWidth) / values.size.coerceAtLeast(1)
        val thickness = (slot * BarShare).coerceIn(MinBar, MaxBar)
        val spacing = (slot - thickness).coerceAtLeast(1.dp)
        ProvideVicoTheme(rememberM3VicoTheme()) {
            val layer: CartesianLayer<*> = if (line) {
                val (low, high) = remember(values) {
                    val shown = values.filterNotNull()
                    val pad = ((shown.max() - shown.min()) * 0.2).coerceAtLeast(1.0)
                    floor(shown.min() - pad).coerceAtLeast(0.0) to ceil(shown.max() + pad)
                }
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = remember(color) { LineCartesianLayer.LineFill.single(Fill(color)) },
                            areaFill = null,
                            pointProvider = remember(color) {
                                LineCartesianLayer.PointProvider.single(LineCartesianLayer.Point(ShapeComponent(Fill(color), CircleShape), PointSize))
                            },
                        ),
                    ),
                    pointSpacing = spacing + thickness,
                    rangeProvider = remember(low, high) { CartesianLayerRangeProvider.fixed(minY = low, maxY = high) },
                )
            } else {
                rememberColumnCartesianLayer(
                    columnProvider = remember(color, thickness) {
                        ColumnCartesianLayer.ColumnProvider.series(LineComponent(Fill(color), thickness, CircleShape))
                    },
                    columnCollectionSpacing = spacing,
                    // With nothing to show, the frame stays: an empty chart of zeros.
                    rangeProvider = remember(hasData) { CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = if (hasData) null else 1.0) },
                )
            }
            val model = remember(values, line) {
                CartesianChartModel(
                    if (line) {
                        val points = values.withIndex().mapNotNull { (x, y) -> y?.let { x to it } }
                        LineCartesianLayerModel.build { series(points.map { it.first }, points.map { it.second }) }
                    } else {
                        ColumnCartesianLayerModel.build { series(values.map { it ?: 0.0 }) }
                    },
                )
            }
            val compact = remember(locale) { CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT) }
            val endAxis = VerticalAxis.rememberEnd(
                line = null,
                tick = null,
                guideline = null,
                label = rememberTextComponent(style = labelStyle),
                valueFormatter = remember(compact) { CartesianValueFormatter { _, value, _ -> compact.format(value) } },
                itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { ValueLabels }) },
            )
            val bottomAxis = HorizontalAxis.rememberBottom(
                line = null,
                tick = null,
                guideline = null,
                label = rememberTextComponent(style = labelStyle),
                valueFormatter = remember(labels) {
                    CartesianValueFormatter { _, value, _ -> labels.getOrNull(value.roundToInt()) ?: "·" }
                },
                itemPlacer = remember(every, offset) { HorizontalAxis.ItemPlacer.aligned(spacing = { every }, offset = { offset }) },
            )
            // The value shows in the headline, so the marker's own label stays invisible: only
            // its line down the chart and a ring on the bar.
            val marker = rememberDefaultCartesianMarker(
                label = rememberTextComponent(style = labelStyle.copy(color = Color.Transparent)),
                indicator = remember(color, ring) {
                    { _: Color -> ShapeComponent(Fill(ring), CircleShape, strokeFill = Fill(color), strokeThickness = RingStroke) }
                },
                indicatorSize = RingSize,
                guideline = remember(faint) { LineComponent(Fill(faint), 1.dp) },
            )
            val listener = remember(onSelect) {
                object : CartesianMarkerVisibilityListener {
                    override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                        onSelect(targets.firstOrNull()?.x?.roundToInt())
                    }

                    override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                        onSelect(targets.firstOrNull()?.x?.roundToInt())
                    }

                    override fun onHidden(marker: CartesianMarker) = onSelect(null)
                }
            }
            CartesianChartHost(
                chart = rememberCartesianChart(
                    layer,
                    endAxis = if (hasData) endAxis else null,
                    bottomAxis = bottomAxis,
                    marker = if (hasData) marker else null,
                    markerVisibilityListener = listener,
                    // A tap shows a bar's value until the next tap.
                    markerController = CartesianMarkerController.rememberToggleOnTap(),
                ),
                model = model,
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content, minZoom = Zoom.Content, maxZoom = Zoom.Content),
                chartAreaHeight = ChartHeight,
            )
        }
    }
}

private val ChartHeight = 180.dp

/** Room for the values on the right. */
private val AxisWidth = 40.dp

/** How much of its slot a bar fills, and how thin or thick it may get. */
private const val BarShare = 0.55f
private val MinBar = 3.dp
private val MaxBar = 28.dp

private val PointSize = 8.dp
private val RingSize = 14.dp
private val RingStroke = 3.dp

/** About how many values the right axis names. */
private const val ValueLabels = 3

/**
 * The labels under a chart, one per bucket, and how often one shows: the days of a week as
 * letters, every week's date in a month, every 4th week, a year's months as letters, or every
 * few months ("Nov 25") further back.
 */
private fun axisLabels(timeline: Timeline, locale: Locale): Pair<List<String>, Int> {
    val starts = timeline.starts
    val count = starts.size
    val (pattern, every) = when (timeline.size) {
        BucketSize.DAY -> if (count <= 7) "EEEEE" to 1 else "d" to 7
        BucketSize.WEEK -> "d MMM" to 4
        BucketSize.MONTH -> if (count <= 12) "LLLLL" to 1 else "MMM yy" to ceil(count / 5.0).toInt()
    }
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
    return starts.map { it.format(formatter) } to every
}

/** A bucket written out for the headline: "Thu 13 Nov", "3–9 Nov", "November 2026". */
fun bucketTitle(timeline: Timeline, index: Int, locale: Locale): String {
    val start = timeline.starts[index]
    fun LocalDate.as_(pattern: String) = format(DateTimeFormatter.ofPattern(pattern, locale))
    return when (timeline.size) {
        BucketSize.DAY -> start.as_("EEE d MMM")
        BucketSize.WEEK -> {
            val end = start.plusDays(6)
            if (end.month == start.month) "${start.dayOfMonth}–${end.as_("d MMM")}" else "${start.as_("d MMM")} – ${end.as_("d MMM")}"
        }
        BucketSize.MONTH -> start.as_("LLLL yyyy")
    }
}

/**
 * One of a few [options], as Material 3 Expressive's connected button group: equal buttons side
 * by side, the chosen one filled and fully round. For a chart's range or what it shows.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ChoiceGroup(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // A light tick as the choice moves.
    HapticOnChange(selected, HapticFeedbackType.SegmentTick)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = colors.surfaceContainerHighest,
                    contentColor = colors.onSurfaceVariant,
                    checkedContainerColor = colors.secondaryContainer,
                    checkedContentColor = colors.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = Dimens.SpaceXs),
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}
