package com.ampairs.analytics.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.columnSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.component.rememberLineComponent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Compose charts for the analytics dashboard (feature 022). Cartesian charts — the [LineChart] (sales
 * trend) and [BarChart] (aging columns) — are rendered with **Vico** (`com.patrykandpatrick.vico`), a
 * Compose-Multiplatform chart library that renders on Android/iOS/Desktop/Wasm. The part-to-whole
 * [DonutChart] (GST split) and the ranked [HorizontalBarList] (top customers/products) stay on a KMP
 * `Canvas`: Vico is Cartesian-only (no pie/donut), and a horizontal ranked list reads better for long
 * named categories than Vico's vertical columns. Canvas marks are coloured from
 * `MaterialTheme.colorScheme` tokens so they stay theme-aware (project rule: no hardcoded colours).
 *
 * Form is chosen by the data's job: trend→line, magnitude across ordered buckets→columns,
 * part-to-whole→donut, ranked named items→horizontal bars.
 */

/** One labelled datum for the bar chart / donut. */
data class ChartBar(val label: String, val value: Double)

data class ChartSlice(val label: String, val value: Double, val color: Color)

// ───────────────────────── Line / area (change over time) ─────────────────────────

/**
 * Line chart of an ordered [values] series (e.g. daily sales), rendered with Vico
 * ([CartesianChartHost] + a line layer). The line + translucent area fill are themed from [lineColor]
 * ([MaterialTheme.colorScheme] primary by default). A `start` (value) axis is shown for scale unless
 * [showAxis] is false — pass false for compact sparklines. Scroll is disabled so the whole series
 * fits the width. A series shorter than two points renders an empty box.
 */
@Composable
fun LineChart(
    values: List<Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(140.dp),
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showAxis: Boolean = true,
) {
    if (values.size < 2) {
        Box(modifier) {}
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        producer.runTransaction { lineSeries { series(values) } }
    }
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
        areaFill = LineCartesianLayer.AreaFill.single(Fill(lineColor.copy(alpha = 0.15f))),
    )
    val startAxis = if (showAxis) VerticalAxis.rememberStart() else null
    CartesianChartHost(
        rememberCartesianChart(
            rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(line)),
            startAxis = startAxis,
        ),
        producer,
        modifier,
        rememberVicoScrollState(false),
    )
}

// ───────────────────────── Vertical bars (magnitude across buckets) ─────────────────────────

/**
 * Column chart over a small ordered set of [bars] (e.g. aging buckets), rendered with Vico
 * ([CartesianChartHost] + a column layer). Columns are themed from [barColor]
 * ([MaterialTheme.colorScheme] primary by default) and a `start` (value) axis is shown for scale
 * unless [showAxis] is false. The caller renders the per-bucket label/value list beside/under this
 * chart, so the x-axis is left off. Scroll is disabled so the columns fit the width. [valueFormatter]
 * is retained for source compatibility.
 */
@Composable
fun BarChart(
    bars: List<ChartBar>,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier.fillMaxWidth().height(120.dp),
    barColor: Color = MaterialTheme.colorScheme.primary,
    showAxis: Boolean = true,
) {
    if (bars.isEmpty()) return
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(bars) {
        producer.runTransaction { columnSeries { series(bars.map { it.value }) } }
    }
    val column = rememberLineComponent(Fill(barColor), 16.dp)
    val startAxis = if (showAxis) VerticalAxis.rememberStart() else null
    CartesianChartHost(
        rememberCartesianChart(
            rememberColumnCartesianLayer(ColumnCartesianLayer.ColumnProvider.series(column)),
            startAxis = startAxis,
        ),
        producer,
        modifier,
        rememberVicoScrollState(false),
    )
}

// ───────────────────────── Horizontal ranked bars (top-N named items) ─────────────────────────

/**
 * Ranked horizontal bar list for a small "top N" set of named items (e.g. top customers / products).
 * Each row is a label + value line over a proportional track-backed bar; bars scale to the largest
 * value so the leader fills the row. Horizontal (not the vertical [BarChart]) because the category
 * identity is a long name that reads far better beside the bar than rotated beneath it.
 */
@Composable
fun HorizontalBarList(
    bars: List<ChartBar>,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (bars.isEmpty()) return
    val max = bars.maxOf { it.value }.coerceAtLeast(1.0)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        bars.forEach { bar ->
            val fraction = (bar.value / max).toFloat().coerceIn(0f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        bar.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        valueFormatter(bar.value),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(track),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor),
                    )
                }
            }
        }
    }
}

// ───────────────────────── Donut (part-to-whole, few slices) ─────────────────────────

/**
 * Donut of a few [slices] (e.g. intra- vs inter-state GST) with a colour legend so identity is never
 * colour-alone. A zero total renders an empty ring.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.size(96.dp)) {
            val thickness = size.minDimension * 0.20f
            val diameter = size.minDimension - thickness
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            // Track ring underneath (also the whole ring when total == 0).
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(thickness))
            if (total > 0.0) {
                var start = -90f
                slices.forEach { slice ->
                    val sweep = (slice.value / total).toFloat() * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(thickness, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                    Text(slice.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
