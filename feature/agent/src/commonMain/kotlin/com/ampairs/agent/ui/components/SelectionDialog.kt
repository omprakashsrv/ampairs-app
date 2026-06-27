package com.ampairs.agent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.agent.SelectionOption

/**
 * Modal dialog for parameter selection (FR-017) — supports both single and multi-select.
 * Renders a list of options with either radio buttons (single) or checkboxes (multi).
 * Works across text (tap), UI (buttons), and voice (optional: "say 1" for number-based selection).
 */
@Composable
fun SelectionDialog(
    question: String,
    options: List<SelectionOption>,
    onSelect: (selectedId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    onMultiSelect: (selectedIds: List<String>) -> Unit = {},
) {
    val selectedIds = remember { mutableStateOf(emptySet<String>()) }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header: question + close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel selection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Options list — each row shows index (for voice), label, and optional secondary text
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(options, key = { index, option -> option.id }) { index, option ->
                    if (multiSelect) {
                        MultiSelectOptionRow(
                            number = index + 1,
                            option = option,
                            isSelected = option.id in selectedIds.value,
                            onToggle = { isSelected ->
                                selectedIds.value = if (isSelected) {
                                    selectedIds.value + option.id
                                } else {
                                    selectedIds.value - option.id
                                }
                            },
                        )
                    } else {
                        SelectionOptionRow(
                            number = index + 1,
                            option = option,
                            onClick = { onSelect(option.id) },
                        )
                    }
                }
            }

            // Voice hint + action buttons
            if (options.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (multiSelect) {
                        "Select items or say numbers (1, 2, 3...)"
                    } else {
                        "Tap an option or say the number (1, 2, 3...)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                if (multiSelect) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onMultiSelect(selectedIds.value.toList()) },
                            enabled = selectedIds.value.isNotEmpty(),
                        ) {
                            Text("Confirm (${selectedIds.value.size})")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single-select option row (radio button style).
 * Shows: [number] · [label] (optional: secondaryLabel)
 */
@Composable
private fun SelectionOptionRow(
    number: Int,
    option: SelectionOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Number badge for voice ("Say 1")
        Surface(
            modifier = Modifier.padding(4.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Primary + secondary labels (stacked if secondary exists)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            option.secondaryLabel?.let { secondary ->
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Multi-select option row (checkbox style).
 * Shows: [number] [checkbox] [label] (optional: secondaryLabel)
 */
@Composable
private fun MultiSelectOptionRow(
    number: Int,
    option: SelectionOption,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Number badge for voice ("Say 1, 2")
        Surface(
            modifier = Modifier.padding(4.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(
                modifier = Modifier.padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(4.dp),
        )

        // Primary + secondary labels (stacked if secondary exists)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            option.secondaryLabel?.let { secondary ->
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
