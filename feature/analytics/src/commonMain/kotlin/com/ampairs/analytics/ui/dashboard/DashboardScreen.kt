@file:OptIn(ExperimentalMaterial3Api::class)

package com.ampairs.analytics.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import ampairsapp.feature.analytics.generated.resources.Res
import ampairsapp.feature.analytics.generated.resources.analytics_aging_empty
import ampairsapp.feature.analytics.generated.resources.analytics_aging_invoices
import ampairsapp.feature.analytics.generated.resources.analytics_dashboard_title
import ampairsapp.feature.analytics.generated.resources.analytics_error_generic
import ampairsapp.feature.analytics.generated.resources.analytics_export
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_confidence_high
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_confidence_low
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_confidence_medium
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_empty
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_estimated
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_expected
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_horizon
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_in_stock
import ampairsapp.feature.analytics.generated.resources.analytics_forecast_reorder
import ampairsapp.feature.analytics.generated.resources.analytics_gst_empty
import ampairsapp.feature.analytics.generated.resources.analytics_gst_inter
import ampairsapp.feature.analytics.generated.resources.analytics_gst_intra
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_avg_invoice
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_collections
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_gross_sales
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_inventory_turns
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_invoices
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_low_stock
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_net_sales
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_outstanding
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_stock_value
import ampairsapp.feature.analytics.generated.resources.analytics_kpi_tax
import ampairsapp.feature.analytics.generated.resources.analytics_period_all_time
import ampairsapp.feature.analytics.generated.resources.analytics_period_last_month
import ampairsapp.feature.analytics.generated.resources.analytics_period_this_month
import ampairsapp.feature.analytics.generated.resources.analytics_period_this_week
import ampairsapp.feature.analytics.generated.resources.analytics_period_this_year
import ampairsapp.feature.analytics.generated.resources.analytics_period_today
import ampairsapp.feature.analytics.generated.resources.analytics_refresh
import ampairsapp.feature.analytics.generated.resources.analytics_retry
import ampairsapp.feature.analytics.generated.resources.analytics_section_aging
import ampairsapp.feature.analytics.generated.resources.analytics_section_gst
import ampairsapp.feature.analytics.generated.resources.analytics_section_forecast
import ampairsapp.feature.analytics.generated.resources.analytics_section_kpis
import ampairsapp.feature.analytics.generated.resources.analytics_section_trend
import ampairsapp.feature.analytics.generated.resources.analytics_trend_empty
import com.ampairs.analytics.domain.AgingBucket
import com.ampairs.analytics.domain.DashboardKpis
import com.ampairs.analytics.domain.DashboardPeriod
import com.ampairs.analytics.domain.ForecastSource
import com.ampairs.analytics.domain.GstSummary
import com.ampairs.analytics.domain.ProductForecast
import com.ampairs.analytics.domain.SalesTrendPoint
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.currencySymbol
import com.ampairs.common.locale.formatMoney
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    val clipboard = LocalClipboardManager.current
    val periodLabel = state.period.label()
    val symbol = currencySymbol(locale.currencyCode)

    // Push the workspace business time zone into the VM so period bounds bucket correctly.
    LaunchedEffect(locale.timeZoneId) { viewModel.setLocale(locale.timeZoneId) }

    val expanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.analytics_dashboard_title)) },
                actions = {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(buildDashboardCsv(state.data, periodLabel, symbol)))
                        },
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(Res.string.analytics_export))
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(Res.string.analytics_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PeriodSelector(selected = state.period, onSelect = viewModel::selectPeriod)

            when {
                state.isLoading && state.data.kpis == DashboardKpis() -> {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Column(
                        Modifier.fillMaxWidth().height(220.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(Res.string.analytics_error_generic),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = viewModel::refresh, modifier = Modifier.padding(top = 12.dp)) {
                            Text(stringResource(Res.string.analytics_retry))
                        }
                    }
                }

                else -> {
                    KpiSection(state.data.kpis, locale, expanded)
                    TrendSection(state.data.trend, locale)
                    ForecastSection(state.data.forecasts)
                    GstSection(state.data.gst, locale)
                    AgingSection(state.data.aging.buckets, locale)
                }
            }
        }
    }
}

// ───────────────────────── Period selector ─────────────────────────

@Composable
private fun PeriodSelector(selected: DashboardPeriod, onSelect: (DashboardPeriod) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DashboardPeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label()) },
            )
        }
    }
}

@Composable
private fun DashboardPeriod.label(): String = stringResource(
    when (this) {
        DashboardPeriod.TODAY -> Res.string.analytics_period_today
        DashboardPeriod.THIS_WEEK -> Res.string.analytics_period_this_week
        DashboardPeriod.THIS_MONTH -> Res.string.analytics_period_this_month
        DashboardPeriod.LAST_MONTH -> Res.string.analytics_period_last_month
        DashboardPeriod.THIS_YEAR -> Res.string.analytics_period_this_year
        DashboardPeriod.ALL_TIME -> Res.string.analytics_period_all_time
    },
)

// ───────────────────────── KPI section ─────────────────────────

@Composable
private fun KpiSection(kpis: DashboardKpis, locale: com.ampairs.common.locale.AppLocale, expanded: Boolean) {
    SectionHeader(stringResource(Res.string.analytics_section_kpis))
    val cards: List<@Composable (Modifier) -> Unit> = listOf(
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_gross_sales), formatMoney(kpis.grossSales, locale), m) },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_net_sales), formatMoney(kpis.netSales, locale), m) },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_tax), formatMoney(kpis.totalTax, locale), m) },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_invoices), kpis.invoiceCount.toString(), m) },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_avg_invoice), formatMoney(kpis.averageInvoiceValue, locale), m) },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_collections), formatMoney(kpis.collectionsReceived, locale), m) },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_stock_value), formatMoney(kpis.stockValue, locale), m) },
        { m ->
            KpiCard(
                stringResource(Res.string.analytics_kpi_low_stock), kpis.lowStockCount.toString(), m,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        },
        { m ->
            KpiCard(
                stringResource(Res.string.analytics_kpi_outstanding), formatMoney(kpis.outstandingReceivable, locale), m,
                container = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        },
        { m -> KpiCard(stringResource(Res.string.analytics_kpi_inventory_turns), "${kpis.inventoryTurns.as2dp()}×", m) },
    )
    val perRow = if (expanded) 4 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { card -> card(Modifier.weight(1f)) }
                repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier.background(container, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.85f))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ───────────────────────── Sales trend (daily bars) ─────────────────────────

@Composable
private fun TrendSection(points: List<SalesTrendPoint>, locale: com.ampairs.common.locale.AppLocale) {
    SectionHeader(stringResource(Res.string.analytics_section_trend))
    SectionSurface {
        if (points.isEmpty()) {
            EmptyRow(stringResource(Res.string.analytics_trend_empty))
        } else {
            val max = points.maxOf { it.total }.coerceAtLeast(1.0)
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                points.takeLast(14).forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            p.bucket.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(44.dp),
                        )
                        Box(
                            Modifier.weight(1f).height(14.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp)),
                        ) {
                            Box(
                                Modifier.fillMaxWidth((p.total / max).toFloat().coerceIn(0f, 1f)).height(14.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp)),
                            )
                        }
                        Text(
                            formatMoney(p.total, locale),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── GST summary ─────────────────────────

@Composable
private fun GstSection(gst: GstSummary, locale: com.ampairs.common.locale.AppLocale) {
    SectionHeader(stringResource(Res.string.analytics_section_gst))
    SectionSurface {
        if (gst.totalTax <= 0.0 && gst.byRate.isEmpty()) {
            EmptyRow(stringResource(Res.string.analytics_gst_empty))
        } else {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabelValueRow(stringResource(Res.string.analytics_gst_intra), formatMoney(gst.intraStateTax, locale))
                LabelValueRow(stringResource(Res.string.analytics_gst_inter), formatMoney(gst.interStateTax, locale))
                if (gst.byRate.isNotEmpty()) {
                    HorizontalDivider()
                    gst.byRate.forEach { r ->
                        LabelValueRow(r.taxCode.ifBlank { "—" }, formatMoney(r.tax, locale))
                    }
                }
            }
        }
    }
}

// ───────────────────────── Receivables aging ─────────────────────────

@Composable
private fun AgingSection(buckets: List<AgingBucket>, locale: com.ampairs.common.locale.AppLocale) {
    SectionHeader(stringResource(Res.string.analytics_section_aging))
    SectionSurface {
        val nonEmpty = buckets.filter { it.count > 0 }
        if (nonEmpty.isEmpty()) {
            EmptyRow(stringResource(Res.string.analytics_aging_empty))
        } else {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                nonEmpty.forEach { b ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                stringResource(Res.string.analytics_aging_invoices, b.count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(formatMoney(b.amount, locale), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Demand forecast (sparkline + reorder) ─────────────────────────

@Composable
private fun ForecastSection(forecasts: List<ProductForecast>) {
    SectionHeader(stringResource(Res.string.analytics_section_forecast))
    SectionSurface {
        if (forecasts.isEmpty()) {
            EmptyRow(stringResource(Res.string.analytics_forecast_empty))
        } else {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                forecasts.forEach { ForecastRow(it) }
            }
        }
    }
}

@Composable
private fun ForecastRow(forecast: ProductForecast) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    forecast.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (forecast.reorderCandidate) ReorderBadge()
            }
            Text(
                forecast.subtitle(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Sparkline(
            values = forecast.recentDailyUnits,
            modifier = Modifier.width(72.dp).height(28.dp),
        )

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                forecast.expectedDemand.asQty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                stringResource(Res.string.analytics_forecast_horizon, forecast.horizonDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReorderBadge() {
    Text(
        stringResource(Res.string.analytics_forecast_reorder).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** "Expected demand · High confidence" (server) or "Estimated · 12 in stock" (EWMA). */
@Composable
private fun ProductForecast.subtitle(): String {
    val lead = when (source) {
        ForecastSource.SERVER -> stringResource(Res.string.analytics_forecast_expected)
        ForecastSource.EWMA -> stringResource(Res.string.analytics_forecast_estimated)
    }
    val tail = when (source) {
        ForecastSource.SERVER -> confidenceLabel()
        ForecastSource.EWMA -> stringResource(Res.string.analytics_forecast_in_stock, currentStock.asQty())
    }
    return if (tail.isBlank()) lead else "$lead · $tail"
}

@Composable
private fun ProductForecast.confidenceLabel(): String = when (confidence.uppercase()) {
    "HIGH" -> stringResource(Res.string.analytics_forecast_confidence_high)
    "MEDIUM" -> stringResource(Res.string.analytics_forecast_confidence_medium)
    "LOW" -> stringResource(Res.string.analytics_forecast_confidence_low)
    else -> ""
}

/**
 * A minimal line sparkline of the trailing daily-units [values]. Scaled to the box height between the
 * series min and max; a flat (all-equal) series draws a centered baseline.
 */
@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    val stroke = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.background(track, RoundedCornerShape(6.dp))) {
        if (values.size < 2) return@Box
        Canvas(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
            val min = values.min()
            val max = values.max()
            val span = (max - min).takeIf { it > 0.0 } ?: 1.0
            val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = stepX * i
                // Invert Y: higher value → higher on screen. Flat series → centered.
                val norm = if (max == min) 0.5 else (v - min) / span
                val y = size.height - (norm * size.height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = stroke, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }
    }
}

// ───────────────────────── Shared building blocks ─────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SectionSurface(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
    ) { content() }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun Double.as2dp(): String {
    val r = (this * 100).roundToLong() / 100.0
    return r.toString()
}

/** Quantity formatting for forecast figures: whole numbers drop the ".0", else 2 dp. */
private fun Double.asQty(): String {
    val rounded = roundToLong()
    return if (kotlin.math.abs(this - rounded) < 0.05) rounded.toString() else as2dp()
}
