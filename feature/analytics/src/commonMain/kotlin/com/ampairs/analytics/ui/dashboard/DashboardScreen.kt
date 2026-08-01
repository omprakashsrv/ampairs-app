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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import ampairsapp.feature.analytics.generated.resources.Res
import ampairsapp.feature.analytics.generated.resources.analytics_aging_empty
import ampairsapp.feature.analytics.generated.resources.analytics_aging_invoices
import ampairsapp.feature.analytics.generated.resources.analytics_ask_clear
import ampairsapp.feature.analytics.generated.resources.analytics_ask_placeholder
import ampairsapp.feature.analytics.generated.resources.analytics_ask_q_collections
import ampairsapp.feature.analytics.generated.resources.analytics_ask_q_invoices
import ampairsapp.feature.analytics.generated.resources.analytics_ask_q_low_stock
import ampairsapp.feature.analytics.generated.resources.analytics_ask_q_outstanding
import ampairsapp.feature.analytics.generated.resources.analytics_ask_q_sales
import ampairsapp.feature.analytics.generated.resources.analytics_ask_unanswered
import ampairsapp.feature.analytics.generated.resources.analytics_coverage_from
import ampairsapp.feature.analytics.generated.resources.analytics_customize_action
import ampairsapp.feature.analytics.generated.resources.analytics_customize_cancel
import ampairsapp.feature.analytics.generated.resources.analytics_customize_move_down
import ampairsapp.feature.analytics.generated.resources.analytics_customize_move_up
import ampairsapp.feature.analytics.generated.resources.analytics_customize_save
import ampairsapp.feature.analytics.generated.resources.analytics_customize_title
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
import ampairsapp.feature.analytics.generated.resources.analytics_section_ask
import ampairsapp.feature.analytics.generated.resources.analytics_section_forecast
import ampairsapp.feature.analytics.generated.resources.analytics_orders_empty
import ampairsapp.feature.analytics.generated.resources.analytics_orders_total
import ampairsapp.feature.analytics.generated.resources.analytics_section_kpis
import ampairsapp.feature.analytics.generated.resources.analytics_section_orders
import ampairsapp.feature.analytics.generated.resources.analytics_section_top_customers
import ampairsapp.feature.analytics.generated.resources.analytics_section_top_products
import ampairsapp.feature.analytics.generated.resources.analytics_section_trend
import ampairsapp.feature.analytics.generated.resources.analytics_top_empty
import ampairsapp.feature.analytics.generated.resources.analytics_trend_empty
import com.ampairs.analytics.domain.AgingBucket
import com.ampairs.analytics.domain.DashboardCoverage
import com.ampairs.analytics.domain.DashboardKpis
import com.ampairs.analytics.domain.DashboardPeriod
import com.ampairs.analytics.domain.DashboardTile
import com.ampairs.analytics.domain.ForecastSource
import com.ampairs.analytics.domain.GstSummary
import com.ampairs.analytics.domain.NlAnswer
import com.ampairs.analytics.domain.NlMetric
import com.ampairs.analytics.ui.charts.BarChart
import com.ampairs.analytics.ui.charts.ChartBar
import com.ampairs.analytics.ui.charts.ChartSlice
import com.ampairs.analytics.ui.charts.DonutChart
import com.ampairs.analytics.ui.charts.HorizontalBarList
import com.ampairs.analytics.ui.charts.LineChart
import com.ampairs.analytics.domain.ProductForecast
import com.ampairs.analytics.domain.RankedItem
import com.ampairs.analytics.domain.SalesTrendPoint
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.currencySymbol
import com.ampairs.common.locale.formatMoney
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.datetime.LocalDate
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
    var showCustomize by rememberSaveable { mutableStateOf(false) }

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
                    IconButton(onClick = { showCustomize = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = stringResource(Res.string.analytics_customize_action))
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
                    (state.coverage as? DashboardCoverage.Reduced)?.let { CoverageBadge(it.fromDate) }
                    NlQuerySection(state.nlAnswer, locale, onAsk = viewModel::askNl, onClear = viewModel::clearNl)
                    KpiSection(state.tiles, state.data.kpis, locale, expanded)
                    TrendSection(state.data.trend, locale)
                    TopProductsSection(state.data.topProducts, locale)
                    TopCustomersSection(state.data.topCustomers, locale)
                    ForecastSection(state.data.forecasts)
                    GstSection(state.data.gst, locale)
                    AgingSection(state.data.aging.buckets, locale)
                    OrdersSection(state.data.orderCount, state.data.ordersByStatus)
                }
            }
        }
    }

    if (showCustomize) {
        CustomizeLayoutDialog(
            current = state.tiles,
            onDismiss = { showCustomize = false },
            onSave = { viewModel.setTiles(it); showCustomize = false },
        )
    }
}

// ───────────────────────── Customize dashboard layout ─────────────────────────

@Composable
private fun CustomizeLayoutDialog(
    current: List<DashboardTile>,
    onDismiss: () -> Unit,
    onSave: (List<DashboardTile>) -> Unit,
) {
    // Enabled tiles first (in stored order), then the rest in canonical order; each row is toggleable
    // and reorderable, so save = the checked tiles in their row order (add / remove / reorder).
    val enabled = current.toSet()
    val rows = remember {
        mutableStateListOf<Pair<DashboardTile, Boolean>>().apply {
            addAll((current + DashboardTile.DEFAULT_ORDER.filter { it !in enabled }).map { it to (it in enabled) })
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.analytics_customize_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                rows.forEachIndexed { index, (tile, isOn) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isOn, onCheckedChange = { rows[index] = tile to it })
                        Text(tile.displayName(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { if (index > 0) rows.swap(index, index - 1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(Res.string.analytics_customize_move_up))
                        }
                        IconButton(onClick = { if (index < rows.lastIndex) rows.swap(index, index + 1) }, enabled = index < rows.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(Res.string.analytics_customize_move_down))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rows.filter { it.second }.map { it.first }) }) {
                Text(stringResource(Res.string.analytics_customize_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.analytics_customize_cancel)) }
        },
    )
}

private fun <T> MutableList<T>.swap(a: Int, b: Int) {
    val tmp = this[a]
    this[a] = this[b]
    this[b] = tmp
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
private fun KpiSection(
    tiles: List<DashboardTile>,
    kpis: DashboardKpis,
    locale: com.ampairs.common.locale.AppLocale,
    expanded: Boolean,
) {
    SectionHeader(stringResource(Res.string.analytics_section_kpis))
    val perRow = if (expanded) 4 else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile -> KpiTileCard(tile, kpis, locale, Modifier.weight(1f)) }
                repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Render one [DashboardTile] from the current KPIs — the switchboard the layout setting drives. */
@Composable
private fun KpiTileCard(
    tile: DashboardTile,
    kpis: DashboardKpis,
    locale: com.ampairs.common.locale.AppLocale,
    modifier: Modifier,
) = when (tile) {
    DashboardTile.GROSS_SALES ->
        KpiCard(stringResource(Res.string.analytics_kpi_gross_sales), formatMoney(kpis.grossSales, locale), modifier)
    DashboardTile.NET_SALES ->
        KpiCard(stringResource(Res.string.analytics_kpi_net_sales), formatMoney(kpis.netSales, locale), modifier)
    DashboardTile.TAX ->
        KpiCard(stringResource(Res.string.analytics_kpi_tax), formatMoney(kpis.totalTax, locale), modifier)
    DashboardTile.INVOICES ->
        KpiCard(stringResource(Res.string.analytics_kpi_invoices), kpis.invoiceCount.toString(), modifier)
    DashboardTile.AVG_INVOICE ->
        KpiCard(stringResource(Res.string.analytics_kpi_avg_invoice), formatMoney(kpis.averageInvoiceValue, locale), modifier)
    DashboardTile.COLLECTIONS ->
        KpiCard(stringResource(Res.string.analytics_kpi_collections), formatMoney(kpis.collectionsReceived, locale), modifier)
    DashboardTile.STOCK_VALUE ->
        KpiCard(stringResource(Res.string.analytics_kpi_stock_value), formatMoney(kpis.stockValue, locale), modifier)
    DashboardTile.LOW_STOCK -> KpiCard(
        stringResource(Res.string.analytics_kpi_low_stock), kpis.lowStockCount.toString(), modifier,
        container = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    DashboardTile.OUTSTANDING -> KpiCard(
        stringResource(Res.string.analytics_kpi_outstanding), formatMoney(kpis.outstandingReceivable, locale), modifier,
        container = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
    DashboardTile.INVENTORY_TURNS ->
        KpiCard(stringResource(Res.string.analytics_kpi_inventory_turns), "${kpis.inventoryTurns.as2dp()}×", modifier)
}

@Composable
private fun DashboardTile.displayName(): String = stringResource(
    when (this) {
        DashboardTile.GROSS_SALES -> Res.string.analytics_kpi_gross_sales
        DashboardTile.NET_SALES -> Res.string.analytics_kpi_net_sales
        DashboardTile.TAX -> Res.string.analytics_kpi_tax
        DashboardTile.INVOICES -> Res.string.analytics_kpi_invoices
        DashboardTile.AVG_INVOICE -> Res.string.analytics_kpi_avg_invoice
        DashboardTile.COLLECTIONS -> Res.string.analytics_kpi_collections
        DashboardTile.STOCK_VALUE -> Res.string.analytics_kpi_stock_value
        DashboardTile.LOW_STOCK -> Res.string.analytics_kpi_low_stock
        DashboardTile.OUTSTANDING -> Res.string.analytics_kpi_outstanding
        DashboardTile.INVENTORY_TURNS -> Res.string.analytics_kpi_inventory_turns
    },
)

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
            val recent = points.takeLast(30)
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (recent.size < 2) {
                    LabelValueRow(recent.first().bucket.takeLast(5), formatMoney(recent.first().total, locale))
                } else {
                    LineChart(
                        recent.map { it.total },
                        Modifier.fillMaxWidth().height(140.dp),
                        yFormatter = { formatMoney(it, locale) },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            recent.first().bucket.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            recent.last().bucket.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── Top products / customers (ranked by sales) ─────────────────────────

@Composable
private fun TopProductsSection(items: List<RankedItem>, locale: com.ampairs.common.locale.AppLocale) {
    RankedSection(stringResource(Res.string.analytics_section_top_products), items, locale)
}

@Composable
private fun TopCustomersSection(items: List<RankedItem>, locale: com.ampairs.common.locale.AppLocale) {
    RankedSection(stringResource(Res.string.analytics_section_top_customers), items, locale)
}

@Composable
private fun RankedSection(
    header: String,
    items: List<RankedItem>,
    locale: com.ampairs.common.locale.AppLocale,
) {
    SectionHeader(header)
    SectionSurface {
        val ranked = items.filter { it.value > 0.0 }
        if (ranked.isEmpty()) {
            EmptyRow(stringResource(Res.string.analytics_top_empty))
        } else {
            Column(Modifier.padding(12.dp)) {
                HorizontalBarList(
                    bars = ranked.map { ChartBar(it.label, it.value) },
                    valueFormatter = { formatMoney(it, locale) },
                )
            }
        }
    }
}

// ───────────────────────── Orders (active count + status breakdown) ─────────────────────────

@Composable
private fun OrdersSection(orderCount: Int, byStatus: List<RankedItem>) {
    SectionHeader(stringResource(Res.string.analytics_section_orders))
    SectionSurface {
        if (orderCount == 0) {
            EmptyRow(stringResource(Res.string.analytics_orders_empty))
        } else {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabelValueRow(stringResource(Res.string.analytics_orders_total), orderCount.toString())
                if (byStatus.isNotEmpty()) {
                    HorizontalDivider()
                    HorizontalBarList(
                        bars = byStatus.map { ChartBar(it.label, it.value) },
                        valueFormatter = { it.toInt().toString() },
                    )
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
            val intraLabel = stringResource(Res.string.analytics_gst_intra)
            val interLabel = stringResource(Res.string.analytics_gst_inter)
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (gst.intraStateTax > 0.0 || gst.interStateTax > 0.0) {
                    DonutChart(
                        slices = listOf(
                            ChartSlice(
                                "$intraLabel · ${formatMoney(gst.intraStateTax, locale)}",
                                gst.intraStateTax,
                                MaterialTheme.colorScheme.primary,
                            ),
                            ChartSlice(
                                "$interLabel · ${formatMoney(gst.interStateTax, locale)}",
                                gst.interStateTax,
                                MaterialTheme.colorScheme.tertiary,
                            ),
                        ),
                    )
                }
                if (gst.byRate.isNotEmpty()) {
                    HorizontalDivider()
                    // Cap the per-rate breakdown to a fixed height and scroll internally so a long
                    // list of tax codes doesn't stretch the whole dashboard (bounded height is also
                    // required for a nested scroll inside the page's outer verticalScroll).
                    Column(
                        Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        gst.byRate.forEach { r ->
                            LabelValueRow(r.taxCode.ifBlank { "—" }, formatMoney(r.tax, locale))
                        }
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
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BarChart(
                    bars = nonEmpty.map { ChartBar(it.label, it.amount) },
                    valueFormatter = { formatMoney(it, locale) },
                    yFormatter = { formatMoney(it, locale) },
                )
                HorizontalDivider()
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

// ───────────────────────── Ask a question (NL → KPI) ─────────────────────────

@Composable
private fun NlQuerySection(
    answer: NlAnswer?,
    locale: com.ampairs.common.locale.AppLocale,
    onAsk: (String) -> Unit,
    onClear: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val suggestions = listOf(
        stringResource(Res.string.analytics_ask_q_sales),
        stringResource(Res.string.analytics_ask_q_invoices),
        stringResource(Res.string.analytics_ask_q_outstanding),
        stringResource(Res.string.analytics_ask_q_collections),
        stringResource(Res.string.analytics_ask_q_low_stock),
    )
    SectionHeader(stringResource(Res.string.analytics_section_ask))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.analytics_ask_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (text.isNotBlank()) onAsk(text) }),
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = ""; onClear() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.analytics_ask_clear))
                    }
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { q ->
                AssistChip(onClick = { text = q; onAsk(q) }, label = { Text(q) })
            }
        }
        when (answer) {
            is NlAnswer.Answered -> NlAnswerCard(answer, locale)
            NlAnswer.Unanswered -> SectionSurface {
                EmptyRow(stringResource(Res.string.analytics_ask_unanswered))
            }
            null -> Unit
        }
    }
}

@Composable
private fun NlAnswerCard(answer: NlAnswer.Answered, locale: com.ampairs.common.locale.AppLocale) {
    val value = if (answer.metric.moneyValued) formatMoney(answer.value, locale) else answer.value.asQty()
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            answer.metric.label().uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun NlMetric.label(): String = stringResource(
    when (this) {
        NlMetric.GROSS_SALES -> Res.string.analytics_kpi_gross_sales
        NlMetric.NET_SALES -> Res.string.analytics_kpi_net_sales
        NlMetric.TAX -> Res.string.analytics_kpi_tax
        NlMetric.INVOICES -> Res.string.analytics_kpi_invoices
        NlMetric.AVG_INVOICE -> Res.string.analytics_kpi_avg_invoice
        NlMetric.COLLECTIONS -> Res.string.analytics_kpi_collections
        NlMetric.STOCK_VALUE -> Res.string.analytics_kpi_stock_value
        NlMetric.LOW_STOCK -> Res.string.analytics_kpi_low_stock
        NlMetric.OUTSTANDING -> Res.string.analytics_kpi_outstanding
        NlMetric.INVENTORY_TURNS -> Res.string.analytics_kpi_inventory_turns
    },
)

// ───────────────────────── Coverage badge (reduced sync window) ─────────────────────────

@Composable
private fun CoverageBadge(fromDate: LocalDate) {
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            stringResource(Res.string.analytics_coverage_from, fromDate.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
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
