package com.ampairs.analytics.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.analytics.generated.resources.Res
import ampairsapp.feature.analytics.generated.resources.analytics_home_last7
import ampairsapp.feature.analytics.generated.resources.analytics_home_outstanding
import ampairsapp.feature.analytics.generated.resources.analytics_home_today_invoices
import ampairsapp.feature.analytics.generated.resources.analytics_home_today_sales
import ampairsapp.feature.analytics.generated.resources.analytics_home_view_dashboard
import com.ampairs.analytics.ui.charts.LineChart
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The home-screen overview strip (hybrid home, feature 022): a compact, tap-through summary of the
 * business — today's sales + invoice count, outstanding receivables, and a trailing 7-day sales
 * sparkline. Reads the same offline-first aggregates as the full dashboard via [HomeSummaryViewModel].
 *
 * Drop this into the workspace home in place of the placeholder KPI cards. When [onOpenDashboard] is
 * non-null the whole card is tappable and shows a "View dashboard" affordance; pass `null` (e.g. when
 * the analytics module isn't enabled for the workspace) to render it as a static, non-navigating strip.
 */
@Composable
fun AnalyticsHomeSummary(
    onOpenDashboard: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: HomeSummaryViewModel = metroViewModel(),
) {
    val locale = LocalAppLocale.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(locale.timeZoneId) { viewModel.setLocale(locale.timeZoneId) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onOpenDashboard != null) Modifier.clickable { onOpenDashboard() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // KPI strip
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCell(
                    label = stringResource(Res.string.analytics_home_today_sales),
                    value = formatMoney(state.todaySales, locale),
                    modifier = Modifier.weight(1f),
                )
                KpiCell(
                    label = stringResource(Res.string.analytics_home_today_invoices),
                    value = state.todayInvoices.toString(),
                    modifier = Modifier.weight(1f),
                )
                KpiCell(
                    label = stringResource(Res.string.analytics_home_outstanding),
                    value = formatMoney(state.outstanding, locale),
                    modifier = Modifier.weight(1f),
                )
            }

            // 7-day sales sparkline
            if (state.weekSales.size >= 2) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(Res.string.analytics_home_last7),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LineChart(
                    values = state.weekSales,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    showAxis = false,
                )
            }

            // Tap-through affordance
            if (onOpenDashboard != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.analytics_home_view_dashboard),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
