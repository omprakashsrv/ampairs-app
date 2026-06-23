package com.ampairs.inventory.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_form_close
import ampairsapp.feature.inventory.generated.resources.inv_form_edit_title
import ampairsapp.feature.inventory.generated.resources.inv_form_error_name_required
import ampairsapp.feature.inventory.generated.resources.inv_form_label_buying_price
import ampairsapp.feature.inventory.generated.resources.inv_form_label_mrp
import ampairsapp.feature.inventory.generated.resources.inv_form_label_name
import ampairsapp.feature.inventory.generated.resources.inv_form_label_opening_stock
import ampairsapp.feature.inventory.generated.resources.inv_form_label_reorder
import ampairsapp.feature.inventory.generated.resources.inv_form_label_selling_price
import ampairsapp.feature.inventory.generated.resources.inv_form_label_sku
import ampairsapp.feature.inventory.generated.resources.inv_form_new_title
import ampairsapp.feature.inventory.generated.resources.inv_form_opening_hint
import ampairsapp.feature.inventory.generated.resources.inv_form_save
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryFormScreen(
    itemId: String?,
    onSaveSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: InventoryFormViewModel = assistedMetroViewModel<InventoryFormViewModel, InventoryFormViewModel.Factory>(key = itemId ?: "new") { create(itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.saved.collectLatest { onSaveSuccess() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEdit) Res.string.inv_form_edit_title else Res.string.inv_form_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.inv_form_close))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !state.saving) {
                        Text(stringResource(Res.string.inv_form_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text(stringResource(Res.string.inv_form_label_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) { { Text(stringResource(Res.string.inv_form_error_name_required)) } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.sku,
                onValueChange = viewModel::onSku,
                label = { Text(stringResource(Res.string.inv_form_label_sku)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(state.costPrice, viewModel::onCostPrice, stringResource(Res.string.inv_form_label_buying_price), Modifier.weight(1f))
                NumberField(state.sellingPrice, viewModel::onSellingPrice, stringResource(Res.string.inv_form_label_selling_price), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(state.mrp, viewModel::onMrp, stringResource(Res.string.inv_form_label_mrp), Modifier.weight(1f))
                NumberField(state.reorder, viewModel::onReorder, stringResource(Res.string.inv_form_label_reorder), Modifier.weight(1f))
            }
            if (!state.isEdit) {
                NumberField(
                    state.openingStock, viewModel::onOpeningStock,
                    stringResource(Res.string.inv_form_label_opening_stock), Modifier.fillMaxWidth(),
                    imeAction = ImeAction.Done,
                )
                Text(
                    stringResource(Res.string.inv_form_opening_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier,
    )
}
