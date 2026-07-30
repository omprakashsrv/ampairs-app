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
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Lightweight, dependency-free Compose charts for the analytics dashboard (feature 022). All marks
 * are drawn on a KMP `Canvas` (no chart library — keeps iOS/Desktop/Android identical) and coloured
 * from `MaterialTheme.colorScheme` tokens so they stay theme-aware in light/dark (project rule: no
 * hardcoded colours). Axis/value labels are composed as `Text` around the canvas rather than drawn
 * into it — simpler and more reliable across targets than canvas text measurement.
 *
 * Design follows the standard charting method: form chosen by the data's job (trend→line, magnitude
 * across ordered buckets→bars, part-to-whole→donut), one axis, recessive gridlines, thin marks, and a
 * legend whenever identity would otherwise be colour-alone.
 */

/** One labelled datum for the bar chart / donut. */
data class ChartBar(val label: String, val value: Double)

data class ChartSlice(val label: String, val value: Double, val color: Color)

// ───────────────────────── Line / area (change over time) ─────────────────────────

/**
 * Area+line chart of an ordered [values] series (e.g. daily sales). Draws three recessive gridlines
 * (baseline / mid / max) and a 2px primary line over a translucent fill. Empty/among-zero series
 * render a flat baseline rather than dividing by zero.
 */
@Composable
fun LineChart(
    values: List<Double>,
    modifier: Modifier = Modifier.fillMaxWidth().height(140.dp),
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    if (values.size < 2) {
        Box(modifier) {}
        return
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val max = values.max().coerceAtLeast(1.0)
        val stepX = w / (values.size - 1)

        // Recessive gridlines at 0 / 50% / 100%.
        listOf(0f, 0.5f, 1f).forEach { f ->
            val y = h - f * h
            drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        val points = values.mapIndexed { i, v ->
            Offset(x = stepX * i, y = h - (v / max).toFloat().coerceIn(0f, 1f) * h)
        }
        val area = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(area, color = lineColor.copy(alpha = 0.15f))

        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, color = lineColor, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

// ───────────────────────── Vertical bars (magnitude across buckets) ─────────────────────────

/**
 * Vertical bar chart over a small ordered set of [bars] (e.g. aging buckets). Bars scale to the max
 * value; [valueFormatter] renders the per-bar value label above each bar, and the bucket label sits
 * below. Kept to a handful of bars — this is not a histogram.
 */
@Composable
fun BarChart(
    bars: List<ChartBar>,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (bars.isEmpty()) return
    val max = bars.maxOf { it.value }.coerceAtLeast(1.0)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    valueFormatter(bar.value),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                    val barH = ((bar.value / max).toFloat().coerceIn(0f, 1f)) * size.height
                    val barW = size.width * 0.6f
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset((size.width - barW) / 2f, size.height - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
                Text(
                    bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
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
