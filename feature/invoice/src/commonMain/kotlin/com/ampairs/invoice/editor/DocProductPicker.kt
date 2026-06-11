package com.ampairs.invoice.editor

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.doc_cmd_create
import ampairsapp.feature.invoice.generated.resources.doc_cmd_create_sub
import ampairsapp.feature.invoice.generated.resources.doc_cmd_hsn_gst
import ampairsapp.feature.invoice.generated.resources.doc_line_none
import ampairsapp.feature.invoice.generated.resources.doc_pick_search
import ampairsapp.feature.invoice.generated.resources.doc_pick_title
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toInr
import com.ampairs.product.domain.ProductSummary
import org.jetbrains.compose.resources.stringResource

/**
 * Product picker sheet (spec 010 v2, screen "picker") used to change a line's product.
 * Sticky search; rows show name, HSN+GST chips and price; the last row is always
 * "+ Create '<typed>'" when there is a query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocProductPicker(
    results: List<ProductSummary>,
    ratePercents: Map<String, Double?>,
    onSearch: (String) -> Unit,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { onSearch(""); focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
            Text(stringResource(Res.string.doc_pick_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onSearch(it) },
                placeholder = { Text(stringResource(Res.string.doc_pick_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).focusRequester(focusRequester),
            )
            LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                items(results.size, key = { results[it].id }) { i ->
                    val p = results[i]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p.id) }
                            .padding(vertical = 8.dp)
                            .heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
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
                                .padding(vertical = 10.dp)
                                .heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Column(Modifier.padding(horizontal = 10.dp)) {
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
