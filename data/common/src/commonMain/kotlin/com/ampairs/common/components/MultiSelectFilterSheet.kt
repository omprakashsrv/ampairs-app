package com.ampairs.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.data.common.generated.resources.Res
import ampairsapp.data.common.generated.resources.filter_apply
import ampairsapp.data.common.generated.resources.filter_clear_all
import ampairsapp.data.common.generated.resources.filter_no_options
import ampairsapp.data.common.generated.resources.filter_sheet_title
import org.jetbrains.compose.resources.stringResource

/** A single selectable value within a [FilterCategory]. [value] is the stored/queried key, [label] the display text. */
data class FilterOption(val value: String, val label: String)

/** A tab in the filter sheet — e.g. "State", "Brand". [key] identifies the category in the selection map. */
data class FilterCategory(
    val key: String,
    val title: String,
    val options: List<FilterOption>,
)

/**
 * Generic multi-select filter bottom sheet. Renders one tab per [FilterCategory]; each tab shows its
 * options as multi-selectable chips. The caller owns the applied selection — this sheet edits a local
 * working copy and reports it back via [onApply].
 *
 * String-agnostic by design (category titles/option labels are resolved by the caller) so it can be
 * reused across feature modules without leaking feature-specific resources into data:common.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MultiSelectFilterSheet(
    categories: List<FilterCategory>,
    selected: Map<String, Set<String>>,
    onApply: (Map<String, Set<String>>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var working by remember(selected) { mutableStateOf(selected) }
    var tabIndex by remember { mutableStateOf(0) }
    val safeTabIndex = tabIndex.coerceIn(0, (categories.size - 1).coerceAtLeast(0))

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.filter_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                val anySelected = working.values.any { it.isNotEmpty() }
                if (anySelected) {
                    TextButton(onClick = { working = emptyMap() }) {
                        Text(stringResource(Res.string.filter_clear_all))
                    }
                }
            }

            if (categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = safeTabIndex,
                    edgePadding = 16.dp
                ) {
                    categories.forEachIndexed { index, category ->
                        val count = working[category.key].orEmpty().size
                        Tab(
                            selected = safeTabIndex == index,
                            onClick = { tabIndex = index },
                            text = {
                                Text(if (count > 0) "${category.title} ($count)" else category.title)
                            }
                        )
                    }
                }

                val current = categories[safeTabIndex]
                if (current.options.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.filter_no_options),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        current.options.forEach { option ->
                            val isSelected = working[current.key].orEmpty().contains(option.value)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val set = working[current.key].orEmpty().toMutableSet()
                                    if (isSelected) set.remove(option.value) else set.add(option.value)
                                    working = working.toMutableMap().apply { put(current.key, set) }
                                },
                                label = { Text(option.label) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onApply(working)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(stringResource(Res.string.filter_apply))
            }
        }
    }
}
