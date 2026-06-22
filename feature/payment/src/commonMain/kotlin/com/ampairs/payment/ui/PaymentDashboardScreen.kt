package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_collections_title
import ampairsapp.feature.payment.generated.resources.payment_empty_body
import ampairsapp.feature.payment.generated.resources.payment_empty_title
import ampairsapp.feature.payment.generated.resources.payment_filter_all
import ampairsapp.feature.payment.generated.resources.payment_filter_settled
import ampairsapp.feature.payment.generated.resources.payment_filter_to_pay
import ampairsapp.feature.payment.generated.resources.payment_record_a_payment
import ampairsapp.feature.payment.generated.resources.payment_record_payment
import ampairsapp.feature.payment.generated.resources.payment_search_hint
import ampairsapp.feature.payment.generated.resources.payment_section_active
import ampairsapp.feature.payment.generated.resources.payment_set_opening_balance_cta
import ampairsapp.feature.payment.generated.resources.payment_settled
import ampairsapp.feature.payment.generated.resources.payment_summary_counts
import ampairsapp.feature.payment.generated.resources.payment_to_pay
import ampairsapp.feature.payment.generated.resources.payment_to_receive
import ampairsapp.feature.payment.generated.resources.payment_total_payable
import ampairsapp.feature.payment.generated.resources.payment_total_receivable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.payment.domain.AgingSummary
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.ui.components.AgingBar
import com.ampairs.payment.ui.components.AgingSegment
import com.ampairs.payment.ui.components.CollectionsColors
import com.ampairs.payment.ui.components.CollectionsEmptyState
import com.ampairs.payment.ui.components.MoneyText
import com.ampairs.payment.ui.components.PartyAvatar
import com.ampairs.payment.ui.components.SectionHeader
import com.ampairs.payment.ui.components.SyncStatusIcon
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

private enum class DashFilter { ALL, TO_PAY, SETTLED }

/** Wide-screen breakpoint where the dashboard switches to portfolio + statement master–detail. */
private val TWO_PANE_MIN_WIDTH = 840.dp

/**
 * Collections dashboard — the portfolio landing for the Payments module (Route.Payment). Aging
 * summary up top, then a scannable, searchable, filterable list of parties. On a wide window the
 * portfolio (master) and the selected party's statement (detail) sit side by side with the
 * per-customer actions inline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDashboardScreen(
    onRecordPayment: () -> Unit,
    onNewAdjustment: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    onOpenParty: (String) -> Unit,
    onRecordPaymentForParty: (String) -> Unit = { onRecordPayment() },
    onAdjustmentForParty: (String) -> Unit = { onNewAdjustment() },
    onOpeningForParty: (String) -> Unit = { onSetOpeningBalance() },
    onReceiptsForParty: (String) -> Unit = { onOpenParty(it) },
    onEditPaymentForParty: (String, String) -> Unit = { partyUid, _ -> onOpenParty(partyUid) },
    viewModel: PaymentDashboardViewModel = metroViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val filteredRows by viewModel.filteredRows.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val aging by viewModel.aging.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    var filter by remember { mutableStateOf(DashFilter.ALL) }
    var selectedPartyUid by rememberSaveable { mutableStateOf<String?>(null) }

    val receiveCount = rows.count { it.balance.cachedClosingBalance.isPositive }
    val payCount = rows.count { it.balance.cachedClosingBalance.isNegative }

    val visibleRows = remember(filteredRows, filter) {
        filteredRows
            .filter { row ->
                when (filter) {
                    DashFilter.ALL -> true
                    DashFilter.TO_PAY -> row.balance.cachedClosingBalance.isNegative
                    DashFilter.SETTLED -> row.balance.cachedClosingBalance.isZero
                }
            }
            .sortedByDescending { kotlin.math.abs(it.balance.cachedClosingBalance.minor) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.payment_collections_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRecordPayment,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.payment_record_payment)) },
            )
        },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH
            if (twoPane) {
                // Default-select the first party so the detail pane is never blank.
                val effectiveSelection = selectedPartyUid ?: visibleRows.firstOrNull()?.balance?.partyUid
                Row(modifier = Modifier.fillMaxSize()) {
                    DashboardListContent(
                        receiveCount = receiveCount,
                        payCount = payCount,
                        aging = aging,
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                        filter = filter,
                        onFilterChange = { filter = it },
                        loading = loading,
                        rowsEmpty = rows.isEmpty(),
                        visibleRows = visibleRows,
                        locale = locale,
                        selectedPartyUid = effectiveSelection,
                        onSelect = { selectedPartyUid = it.balance.partyUid },
                        onRecordPayment = onRecordPayment,
                        onSetOpeningBalance = onSetOpeningBalance,
                        modifier = Modifier.width(430.dp).fillMaxHeight(),
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxHeight().width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (effectiveSelection != null) {
                            StatementDetailPane(
                                partyUid = effectiveSelection,
                                locale = locale,
                                onRecordPayment = { onRecordPaymentForParty(effectiveSelection) },
                                onAdjustment = { onAdjustmentForParty(effectiveSelection) },
                                onOpeningBalance = { onOpeningForParty(effectiveSelection) },
                                onReceipts = { onReceiptsForParty(effectiveSelection) },
                                onEditPayment = { voucherUid -> onEditPaymentForParty(effectiveSelection, voucherUid) },
                            )
                        }
                    }
                }
            } else {
                DashboardListContent(
                    receiveCount = receiveCount,
                    payCount = payCount,
                    aging = aging,
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    filter = filter,
                    onFilterChange = { filter = it },
                    loading = loading,
                    rowsEmpty = rows.isEmpty(),
                    visibleRows = visibleRows,
                    locale = locale,
                    selectedPartyUid = null,
                    onSelect = { onOpenParty(it.balance.partyUid) },
                    onRecordPayment = onRecordPayment,
                    onSetOpeningBalance = onSetOpeningBalance,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun DashboardListContent(
    receiveCount: Int,
    payCount: Int,
    aging: AgingSummary?,
    query: String,
    onQueryChange: (String) -> Unit,
    filter: DashFilter,
    onFilterChange: (DashFilter) -> Unit,
    loading: Boolean,
    rowsEmpty: Boolean,
    visibleRows: List<PartyRow>,
    locale: AppLocale,
    selectedPartyUid: String?,
    onSelect: (PartyRow) -> Unit,
    onRecordPayment: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            stringResource(Res.string.payment_summary_counts, receiveCount, payCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )

        aging?.let { AgingSummaryCard(it, locale) }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(Res.string.payment_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(23.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(filter == DashFilter.ALL, { onFilterChange(DashFilter.ALL) }, { Text(stringResource(Res.string.payment_filter_all)) })
            FilterChip(filter == DashFilter.TO_PAY, { onFilterChange(DashFilter.TO_PAY) }, { Text(stringResource(Res.string.payment_filter_to_pay)) })
            FilterChip(filter == DashFilter.SETTLED, { onFilterChange(DashFilter.SETTLED) }, { Text(stringResource(Res.string.payment_filter_settled)) })
        }

        when {
            loading -> DashboardSkeleton()
            rowsEmpty -> CollectionsEmptyState(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(Res.string.payment_empty_title),
                body = stringResource(Res.string.payment_empty_body),
                primaryActionLabel = stringResource(Res.string.payment_record_a_payment),
                onPrimaryAction = onRecordPayment,
                secondaryActionLabel = stringResource(Res.string.payment_set_opening_balance_cta),
                onSecondaryAction = onSetOpeningBalance,
            )
            else -> {
                SectionHeader(stringResource(Res.string.payment_section_active))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleRows, key = { it.balance.uid }) { row ->
                        PartyBalanceRow(
                            row = row,
                            onClick = { onSelect(row) },
                            selected = row.balance.partyUid == selectedPartyUid,
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 73.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/** Detail pane (desktop): the selected party's statement with inline per-customer actions. */
@Composable
private fun StatementDetailPane(
    partyUid: String,
    locale: AppLocale,
    onRecordPayment: () -> Unit,
    onAdjustment: () -> Unit,
    onOpeningBalance: () -> Unit,
    onReceipts: () -> Unit,
    onEditPayment: (String) -> Unit,
) {
    val viewModel: PartyStatementViewModel = assistedMetroViewModel<PartyStatementViewModel, PartyStatementViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PartyStatementContent(
        state = state,
        locale = locale,
        showInlineActions = true,
        onRecordPayment = onRecordPayment,
        onAdjustment = onAdjustment,
        onOpeningBalance = onOpeningBalance,
        onSendStatement = {},
        onReceipts = onReceipts,
        onEditPayment = onEditPayment,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun PartyBalanceRow(
    row: PartyRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val balance = row.balance
    val receivable = balance.closingDirection == Direction.DR
    val settled = balance.cachedClosingBalance.isZero
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PartyAvatar(name = row.name)
        Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
            Text(
                row.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            row.phone?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            if (settled) {
                Text(
                    stringResource(Res.string.payment_settled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MoneyText(amount = balance.cachedClosingBalance.abs().toDouble(), isReceivable = receivable)
                Text(
                    stringResource(if (receivable) Res.string.payment_to_receive else Res.string.payment_to_pay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (row.pendingSync) {
                SyncStatusIcon(pending = true, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun AgingSummaryCard(aging: AgingSummary, locale: AppLocale) {
    val bucketColors = CollectionsColors.agingBuckets
    val segments = aging.buckets.mapIndexed { i, b ->
        AgingSegment(label = b.label, amount = b.amount.toDouble(), color = bucketColors[i % bucketColors.size])
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.payment_total_receivable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(aging.totalReceivable.toDouble(), isReceivable = true, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
            }
            Box(
                modifier = Modifier.padding(horizontal = 14.dp).width(1.dp).height(44.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.payment_total_payable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(aging.totalPayable.toDouble(), isReceivable = false, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
            }
        }
        if (segments.any { it.amount > 0 }) {
            Spacer(Modifier.height(16.dp))
            AgingBar(segments = segments)
        }
    }
}

@Composable
private fun DashboardSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.height(44.dp).width(44.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(22.dp)))
                Column(modifier = Modifier.weight(1f).padding(start = 13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(7.dp)))
                    Box(modifier = Modifier.fillMaxWidth(0.75f).height(11.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(6.dp)))
                }
                Box(modifier = Modifier.width(60.dp).height(14.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(7.dp)))
            }
        }
    }
}
