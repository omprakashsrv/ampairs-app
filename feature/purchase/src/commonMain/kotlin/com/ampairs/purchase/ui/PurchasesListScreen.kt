package com.ampairs.purchase.ui

import ampairsapp.feature.purchase.generated.resources.Res
import ampairsapp.feature.purchase.generated.resources.pur_list_count
import ampairsapp.feature.purchase.generated.resources.pur_list_empty_cta
import ampairsapp.feature.purchase.generated.resources.pur_list_empty_desc
import ampairsapp.feature.purchase.generated.resources.pur_list_empty_title
import ampairsapp.feature.purchase.generated.resources.pur_list_items
import ampairsapp.feature.purchase.generated.resources.pur_list_new
import ampairsapp.feature.purchase.generated.resources.pur_list_retry
import ampairsapp.feature.purchase.generated.resources.pur_list_title
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.viewmodel.PurchasesListViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Purchase list: reactive rows straight from Room (number · supplier · status · ₹ amount), a sync
 * progress strip while pulling, an inline error with retry, and a CTA empty state.
 */
@Composable
fun PurchasesListScreen(
    onPurchaseSelected: (String) -> Unit,
    onCreatePurchase: () -> Unit = {},
    viewModel: PurchasesListViewModel = metroViewModel(),
) {
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePurchase,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.pur_list_new)) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(Res.string.pur_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (purchases.isNotEmpty()) {
                        Text(
                            text = stringResource(Res.string.pur_list_count, purchases.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider()

            if (sync.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            sync.error?.let { msg ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text(stringResource(Res.string.pur_list_retry))
                        }
                    }
                }
            }

            if (purchases.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(purchases, key = { it.id }, contentType = { 1 }) { purchase ->
                        PurchaseRow(
                            purchase = purchase,
                            onClick = { onPurchaseSelected(purchase.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.pur_list_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.pur_list_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(onClick = onCreatePurchase) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  " + stringResource(Res.string.pur_list_empty_cta))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(
    purchase: PurchaseEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalAppLocale.current
    val cs = MaterialTheme.colorScheme
    val supplier = purchase.supplier_name.ifBlank { "—" }
    val number = purchase.purchase_number.ifBlank { "—" }
    val amount = formatMoney(purchase.total_cost, locale)
    val synced = purchase.synced == 1L
    val rowCd = "$number, $supplier, $amount, ${purchase.status.lowercase()}"

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .heightIn(min = 56.dp)
                .semantics { contentDescription = rowCd },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(cs.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Inventory2, contentDescription = null,
                    tint = cs.onTertiaryContainer, modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "  $supplier",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    )
                    PurchaseStatusChip(purchase.status)
                }
                Text(
                    text = listOf(
                        purchase.purchase_date.take(10),
                        stringResource(Res.string.pur_list_items, purchase.total_items.toInt()),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (!synced) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = cs.secondary,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outlineVariant)
    }
}

@Composable
internal fun PurchaseStatusChip(status: String) {
    val cs = MaterialTheme.colorScheme
    val (container, content) = when (status.uppercase()) {
        "DRAFT" -> cs.secondaryContainer to cs.onSecondaryContainer
        "ORDERED" -> cs.tertiaryContainer to cs.onTertiaryContainer
        "RECEIVED" -> cs.primaryContainer to cs.onPrimaryContainer
        else -> cs.errorContainer to cs.onErrorContainer
    }
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            text = status.lowercase().replaceFirstChar { it.uppercaseChar() },
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}
