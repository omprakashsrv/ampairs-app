package com.ampairs.inventory.ui.adjust

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_adjust_apply
import ampairsapp.feature.inventory.generated.resources.inv_adjust_blocked_negative
import ampairsapp.feature.inventory.generated.resources.inv_adjust_direction
import ampairsapp.feature.inventory.generated.resources.inv_adjust_disabled
import ampairsapp.feature.inventory.generated.resources.inv_adjust_preview
import ampairsapp.feature.inventory.generated.resources.inv_adjust_reason_optional
import ampairsapp.feature.inventory.generated.resources.inv_adjust_in
import ampairsapp.feature.inventory.generated.resources.inv_adjust_out
import ampairsapp.feature.inventory.generated.resources.inv_detail_on_hand
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.formatQty
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockSheet(
    itemId: String,
    onDismiss: () -> Unit,
    viewModel: AdjustStockViewModel = assistedMetroViewModel<AdjustStockViewModel, AdjustStockViewModel.Factory>(key = itemId) { create(itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel) {
        viewModel.done.collectLatest { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                state.item?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${stringResource(Res.string.inv_detail_on_hand)} ${formatQty(state.onHand)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(Res.string.inv_adjust_direction), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.stockIn,
                    onClick = { viewModel.setDirection(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(Res.string.inv_adjust_in)) }
                SegmentedButton(
                    selected = !state.stockIn,
                    onClick = { viewModel.setDirection(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(Res.string.inv_adjust_out)) }
            }

            // Quantity stepper
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = viewModel::decrement) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                }
                OutlinedTextField(
                    value = formatQty(state.quantity),
                    onValueChange = { viewModel.setQuantity(it.toDoubleOrNull() ?: 0.0) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                )
                IconButton(onClick = viewModel::increment) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(Res.string.inv_adjust_preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatQty(state.preview),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            OutlinedTextField(
                value = state.reason,
                onValueChange = viewModel::setReason,
                label = { Text(stringResource(Res.string.inv_adjust_reason_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.blocked) {
                GuardrailNote(stringResource(Res.string.inv_adjust_blocked_negative))
            }
            if (!state.manualAllowed) {
                GuardrailNote(stringResource(Res.string.inv_adjust_disabled))
            }

            Button(
                onClick = viewModel::apply,
                enabled = state.quantity > 0.0 && !state.blocked && state.manualAllowed && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.inv_adjust_apply)) }
        }
    }
}

@Composable
private fun GuardrailNote(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
