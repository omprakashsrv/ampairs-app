package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_cmd_create
import ampairsapp.feature.invoice.generated.resources.doc_cmd_create_sub
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hsn_gst
import ampairsapp.feature.invoice.generated.resources.doc_line_none
import ampairsapp.feature.invoice.generated.resources.doc_pick_search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toInr
import com.ampairs.product.domain.ProductSummary
import org.jetbrains.compose.resources.stringResource

/**
 * Product picker as a cell-anchored popover (spec 010 v2, screen "picker": "type-ahead
 * popover/dropdown under the Product cell — no takeover"). Anchor it inside a Box wrapping
 * the product cell/row. Sticky search on top; the last row is always "+ Create '<typed>'"
 * when there is a query.
 */
@Composable
fun DocProductPickerPopover(
    expanded: Boolean,
    results: List<ProductSummary>,
    ratePercents: Map<String, Double?>,
    onSearch: (String) -> Unit,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(expanded) {
        if (expanded) {
            query = ""
            onSearch("")
            focusRequester.requestFocus()
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        Column(Modifier.width(340.dp).padding(horizontal = 8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onSearch(it) },
                placeholder = { Text(stringResource(Res.string.doc_pick_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).focusRequester(focusRequester),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(results.size, key = { results[it].id }) { i ->
                    val p = results[i]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p.id) }
                            .padding(vertical = 7.dp, horizontal = 4.dp)
                            .heightIn(min = 40.dp),
                    ) {
                        Icon(
                            Icons.Filled.Inventory2, contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp),
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(
                                    Res.string.doc_cmd_hsn_gst,
                                    p.taxCode.ifBlank { stringResource(Res.string.doc_line_none) },
                                    ratePctLabel(ratePercents[p.taxCode] ?: 0.0),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        Text(
                            p.sellingPrice.toInr(),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                }
                if (query.isNotBlank()) {
                    item(key = "create") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCreate(query.trim()) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .heightIn(min = 40.dp),
                        ) {
                            Icon(
                                Icons.Filled.AddCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp),
                            )
                            Column(Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    stringResource(Res.string.doc_cmd_create, query.trim()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    stringResource(Res.string.doc_cmd_create_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
