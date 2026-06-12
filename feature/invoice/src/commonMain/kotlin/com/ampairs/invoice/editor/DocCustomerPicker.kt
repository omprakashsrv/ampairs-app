package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_cust_clear_cd
import ampairsapp.feature.invoice.generated.resources.doc_cust_search
import ampairsapp.feature.invoice.generated.resources.doc_cust_title
import ampairsapp.feature.invoice.generated.resources.doc_cust_unregistered
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_back
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_btn
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_gstin
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_hint
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_name
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_new
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_phone
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_sub
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_title
import ampairsapp.feature.invoice.generated.resources.doc_cust_walkin_use
import ampairsapp.feature.invoice.generated.resources.doc_scenario_inter_short
import ampairsapp.feature.invoice.generated.resources.doc_scenario_intra_short
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.tax.calculation.document.ScenarioResolver
import org.jetbrains.compose.resources.stringResource

/**
 * Searchable customer picker + walk-in capture (spec 010 v2, screen "customer"). Search matches
 * name / phone (space-insensitive) / GSTIN / state (DAO-side); rows show the GST scenario vs the
 * seller state; no match offers a pre-filled walk-in; ↑/↓ + Enter select, Esc clears then closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocCustomerPicker(
    results: List<CustomerListItem>,
    sellerStateCode: Int?,
    onSearch: (String) -> Unit,
    onSelect: (String) -> Unit,
    onWalkIn: (name: String, phone: String, gstin: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf(0) }
    var walkInName by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val noMatch = query.isNotBlank() && results.isEmpty()

    LaunchedEffect(Unit) { onSearch(""); focusRequester.requestFocus() }
    LaunchedEffect(query) { highlight = 0 }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        val walkIn = walkInName
        if (walkIn != null) {
            WalkInForm(
                initialName = walkIn,
                onBack = { walkInName = null },
                onUse = { name, phone, gstin -> onWalkIn(name, phone, gstin) },
            )
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
                Text(stringResource(Res.string.doc_cust_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    placeholder = { Text(stringResource(Res.string.doc_cust_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = ""; onSearch("") }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.doc_cust_clear_cd), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.DirectionDown -> { highlight = (highlight + 1).coerceAtMost(results.lastIndex.coerceAtLeast(0)); true }
                                Key.DirectionUp -> { highlight = (highlight - 1).coerceAtLeast(0); true }
                                Key.Enter, Key.NumPadEnter -> {
                                    when {
                                        noMatch -> walkInName = query.trim()
                                        results.isNotEmpty() -> onSelect(results[highlight.coerceIn(0, results.lastIndex)].id)
                                    }
                                    true
                                }
                                Key.Escape -> { if (query.isNotEmpty()) { query = ""; onSearch("") } else onDismiss(); true }
                                else -> false
                            }
                        },
                )
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(results.size, key = { results[it].id }) { i ->
                        CustomerRow(
                            customer = results[i],
                            sellerStateCode = sellerStateCode,
                            highlighted = i == highlight,
                            onClick = { onSelect(results[i].id) },
                        )
                    }
                    if (noMatch) {
                        item(key = "walkin") {
                            WalkInRow(typed = query.trim(), onClick = { walkInName = query.trim() })
                        }
                    }
                }
                FilledTonalButton(onClick = { walkInName = "" }, modifier = Modifier.padding(top = 10.dp)) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(Res.string.doc_cust_walkin_btn))
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: CustomerListItem,
    sellerStateCode: Int?,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val buyerState = ScenarioResolver.stateCodeFromGstin(customer.gstin)
    val intra = sellerStateCode == null || buyerState == null || buyerState == sellerStateCode
    val initials = customer.name.split(' ').filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) cs.secondaryContainer.copy(alpha = 0.4f) else cs.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .heightIn(min = 48.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(cs.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(customer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    customer.phone?.takeIf { it.isNotBlank() },
                    customer.state?.takeIf { it.isNotBlank() }?.let { st -> buyerState?.let { "$st ($it)" } ?: st },
                    if (customer.gstin.isNullOrBlank()) stringResource(Res.string.doc_cust_unregistered) else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(color = cs.surfaceContainerHigh, shape = RoundedCornerShape(50)) {
            Text(
                stringResource(if (intra) Res.string.doc_scenario_intra_short else Res.string.doc_scenario_inter_short),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        if (highlighted) Text("  ↵", style = MaterialTheme.typography.labelMedium, color = cs.primary)
    }
}

@Composable
private fun WalkInRow(typed: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp).heightIn(min = 48.dp),
    ) {
        Box(Modifier.size(36.dp).background(cs.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                stringResource(Res.string.doc_cust_walkin_new, typed),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.primary,
                fontWeight = FontWeight.Medium,
            )
            Text(stringResource(Res.string.doc_cust_walkin_sub), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
        Text("↵", style = MaterialTheme.typography.labelMedium, color = cs.primary)
    }
}

@Composable
private fun WalkInForm(
    initialName: String,
    onBack: () -> Unit,
    onUse: (name: String, phone: String, gstin: String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("+91 ") }
    var gstin by remember { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { nameFocus.requestFocus() }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.doc_cust_walkin_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(stringResource(Res.string.doc_cust_walkin_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
        )
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text(stringResource(Res.string.doc_cust_walkin_phone)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = gstin, onValueChange = { gstin = it.uppercase() },
            label = { Text(stringResource(Res.string.doc_cust_walkin_gstin)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(Res.string.doc_cust_walkin_hint),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
        Row {
            TextButton(onClick = onBack) { Text(stringResource(Res.string.doc_cust_walkin_back)) }
            Box(Modifier.weight(1f))
            Button(
                enabled = name.isNotBlank(),
                onClick = { onUse(name.trim(), phone.trim(), gstin.trim()) },
            ) { Text(stringResource(Res.string.doc_cust_walkin_use)) }
        }
    }
}
