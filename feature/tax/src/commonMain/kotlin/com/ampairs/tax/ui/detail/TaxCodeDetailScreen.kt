package com.ampairs.tax.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.domain.model.TaxCode
import kotlin.time.Instant
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.tax.generated.resources.Res
import ampairsapp.feature.tax.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCodeDetailScreen(
    taxCodeId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaxCodeDetailViewModel = assistedMetroViewModel<TaxCodeDetailViewModel, TaxCodeDetailViewModel.Factory> { create(taxCodeId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tax_detail_title)) },
                navigationIcon = {
                    ScreenBackButton(onClick = onNavigateBack, contentDescription = stringResource(Res.string.tax_detail_cd_back))
                },
                actions = {
                    if (uiState is TaxCodeDetailUiState.Success) {
                        val state = uiState as TaxCodeDetailUiState.Success
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (state.taxCode.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(Res.string.tax_detail_cd_favorite),
                                tint = if (state.taxCode.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showUnsubscribeDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.tax_detail_cd_unsubscribe), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        when (val state = uiState) {
            is TaxCodeDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TaxCodeDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = state.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onNavigateBack) { Text(stringResource(Res.string.tax_error_go_back)) }
                    }
                }
            }
            is TaxCodeDetailUiState.Success -> {
                if (isExpanded) {
                    TaxDetailExpandedLayout(
                        state = state,
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onEditNotes = { viewModel.setEditing(true) },
                        onSaveNotes = { notes -> viewModel.saveNotes(notes) },
                        onCancelEdit = { viewModel.setEditing(false) },
                        onUnsubscribe = { showUnsubscribeDialog = true },
                        onClearError = { viewModel.clearError() },
                        onCalculatePreview = { amount, quantity, src, dest ->
                            viewModel.calculateTaxPreview(amount, quantity, src, dest)
                        },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                } else {
                    TaxDetailContent(
                        state = state,
                        onToggleFavorite = { viewModel.toggleFavorite() },
                        onEditNotes = { viewModel.setEditing(true) },
                        onSaveNotes = { notes -> viewModel.saveNotes(notes) },
                        onCancelEdit = { viewModel.setEditing(false) },
                        onUnsubscribe = { showUnsubscribeDialog = true },
                        onClearError = { viewModel.clearError() },
                        onCalculatePreview = { amount, quantity, src, dest ->
                            viewModel.calculateTaxPreview(amount, quantity, src, dest)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                if (showUnsubscribeDialog) {
                    UnsubscribeConfirmationDialog(
                        taxCode = state.taxCode,
                        onConfirm = {
                            viewModel.unsubscribe(onSuccess = onNavigateBack)
                            showUnsubscribeDialog = false
                        },
                        onDismiss = { showUnsubscribeDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaxDetailExpandedLayout(
    state: TaxCodeDetailUiState.Success,
    onToggleFavorite: () -> Unit,
    onEditNotes: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onUnsubscribe: () -> Unit,
    onClearError: () -> Unit,
    onCalculatePreview: (Double, Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Left sidebar
        OutlinedCard(modifier = Modifier.width(280.dp).verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TaxHeroBadge(taxCode = state.taxCode)
                HorizontalDivider()
                TaxDetailInfoRows(taxCode = state.taxCode)
            }
        }
        // Right content panel
        OutlinedCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TaxCalculationCard(
                    calculationResult = state.calculationResult,
                    isCalculating = state.isCalculating,
                    onCalculatePreview = onCalculatePreview
                )
                TaxUsageCard(taxCode = state.taxCode)
                TaxRulesCard(taxRules = state.taxRules, isLoadingRules = state.isLoadingRules)
                TaxNotesCard(
                    taxCode = state.taxCode,
                    isEditing = state.isEditing,
                    isSaving = state.isSaving,
                    saveError = state.saveError,
                    onEditNotes = onEditNotes,
                    onSaveNotes = onSaveNotes,
                    onCancelEdit = onCancelEdit,
                    onClearError = onClearError
                )
            }
        }
    }
}

@Composable
private fun TaxDetailContent(
    state: TaxCodeDetailUiState.Success,
    onToggleFavorite: () -> Unit,
    onEditNotes: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onUnsubscribe: () -> Unit,
    onClearError: () -> Unit,
    onCalculatePreview: (Double, Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero card
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaxHeroBadge(taxCode = state.taxCode)
            }
        }
        TaxDetailInfoCard(taxCode = state.taxCode)
        TaxCalculationCard(
            calculationResult = state.calculationResult,
            isCalculating = state.isCalculating,
            onCalculatePreview = onCalculatePreview
        )
        TaxUsageCard(taxCode = state.taxCode)
        TaxRulesCard(taxRules = state.taxRules, isLoadingRules = state.isLoadingRules)
        TaxNotesCard(
            taxCode = state.taxCode,
            isEditing = state.isEditing,
            isSaving = state.isSaving,
            saveError = state.saveError,
            onEditNotes = onEditNotes,
            onSaveNotes = onSaveNotes,
            onCancelEdit = onCancelEdit,
            onClearError = onClearError
        )
    }
}

@Composable
private fun TaxHeroBadge(taxCode: TaxCode, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = taxCode.code,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = taxCode.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (taxCode.shortDescription.isNotBlank() && taxCode.shortDescription != taxCode.description) {
            Text(
                text = taxCode.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = {},
                label = { Text(taxCode.codeType.name, style = MaterialTheme.typography.labelSmall) }
            )
            val (syncIcon, syncColor, syncLabel) = when (taxCode.syncStatus) {
                "SYNCED" -> Triple(Icons.Default.CloudDone, MaterialTheme.colorScheme.primary, stringResource(Res.string.tax_sync_synced))
                "PENDING" -> Triple(Icons.Default.CloudQueue, MaterialTheme.colorScheme.tertiary, stringResource(Res.string.tax_sync_pending))
                "DELETE_PENDING" -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.error, stringResource(Res.string.tax_sync_deleting))
                else -> Triple(Icons.Default.CloudQueue, MaterialTheme.colorScheme.onSurfaceVariant, taxCode.syncStatus)
            }
            AssistChip(
                onClick = {},
                label = { Text(syncLabel, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(syncIcon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(labelColor = syncColor, leadingIconContentColor = syncColor)
            )
        }
    }
}

@Composable
private fun TaxDetailInfoCard(taxCode: TaxCode, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(Res.string.tax_detail_section_details), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            TaxDetailInfoRows(taxCode = taxCode)
        }
    }
}

@Composable
private fun TaxDetailInfoRows(taxCode: TaxCode) {
    DetailRow(label = stringResource(Res.string.tax_detail_code_type), value = taxCode.codeType.name)
    DetailRow(label = stringResource(Res.string.tax_detail_master_code_id), value = taxCode.masterTaxCodeId)
    if (taxCode.customTaxRuleId != null) {
        DetailRow(label = stringResource(Res.string.tax_detail_custom_rule), value = taxCode.customTaxRuleId!!)
    }
    DetailRow(
        label = stringResource(Res.string.tax_detail_status_label),
        value = if (taxCode.isActive) stringResource(Res.string.tax_active) else stringResource(Res.string.tax_inactive)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(Res.string.tax_detail_sync_status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val (icon, color, label) = when (taxCode.syncStatus) {
            "SYNCED" -> Triple(Icons.Default.CloudDone, MaterialTheme.colorScheme.primary, stringResource(Res.string.tax_sync_synced))
            "PENDING" -> Triple(Icons.Default.CloudQueue, MaterialTheme.colorScheme.tertiary, stringResource(Res.string.tax_sync_pending))
            "DELETE_PENDING" -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.error, stringResource(Res.string.tax_sync_deleting))
            else -> Triple(Icons.Default.CloudQueue, MaterialTheme.colorScheme.onSurfaceVariant, taxCode.syncStatus)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

@Composable
private fun TaxCalculationCard(
    calculationResult: TaxCalculationResult?,
    isCalculating: Boolean,
    onCalculatePreview: (Double, Int, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalAppLocale.current
    var previewAmount by remember { mutableStateOf("1000.00") }
    var previewQuantity by remember { mutableStateOf("1") }
    var previewSourceState by remember { mutableStateOf("MH") }
    var previewDestState by remember { mutableStateOf("MH") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.tax_detail_section_calc),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = stringResource(Res.string.tax_detail_calc_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = previewAmount,
                    onValueChange = { previewAmount = it },
                    label = { Text(stringResource(Res.string.tax_detail_calc_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = previewQuantity,
                    onValueChange = { previewQuantity = it },
                    label = { Text(stringResource(Res.string.tax_detail_calc_qty)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.5f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = previewSourceState,
                    onValueChange = { previewSourceState = it },
                    label = { Text(stringResource(Res.string.tax_detail_calc_from_state)) },
                    placeholder = { Text("MH") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = previewDestState,
                    onValueChange = { previewDestState = it },
                    label = { Text(stringResource(Res.string.tax_detail_calc_to_state)) },
                    placeholder = { Text("MH") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    val amount = previewAmount.toDoubleOrNull() ?: 1000.0
                    val qty = previewQuantity.toIntOrNull() ?: 1
                    onCalculatePreview(amount, qty, previewSourceState, previewDestState)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCalculating
            ) {
                Icon(Icons.Default.Calculate, contentDescription = stringResource(Res.string.tax_detail_calc_cd_calc))
                Spacer(Modifier.width(8.dp))
                Text(if (isCalculating) stringResource(Res.string.tax_detail_calc_calculating) else stringResource(Res.string.tax_detail_calc_btn))
            }
            if (calculationResult != null) {
                HorizontalDivider()
                DetailRow(label = stringResource(Res.string.tax_detail_base_amount), value = formatMoney(calculationResult.baseAmount, locale))
                calculationResult.taxComponents.forEach { component ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${component.componentName} (${component.ratePercentage}%):", style = MaterialTheme.typography.bodySmall)
                        Text(text = formatMoney(component.taxAmount, locale), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(Res.string.tax_detail_total_tax), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = formatMoney(calculationResult.totalTaxAmount, locale), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(Res.string.tax_detail_grand_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = formatMoney(calculationResult.totalAmount, locale), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TaxUsageCard(taxCode: TaxCode, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(Res.string.tax_detail_section_usage), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow(label = stringResource(Res.string.tax_detail_usage_count), value = "${taxCode.usageCount}")
            if (taxCode.lastUsedAt != null) {
                DetailRow(label = stringResource(Res.string.tax_detail_last_used), value = formatTimestamp(taxCode.lastUsedAt!!))
            }
            DetailRow(label = stringResource(Res.string.tax_detail_added), value = formatTimestamp(taxCode.addedAt))
            DetailRow(label = stringResource(Res.string.tax_detail_last_updated), value = formatTimestamp(taxCode.updatedAt))
        }
    }
}

@Composable
private fun TaxRulesCard(
    taxRules: List<com.ampairs.tax.domain.model.TaxRule>,
    isLoadingRules: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(Res.string.tax_detail_section_rules), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            when {
                isLoadingRules -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                taxRules.isEmpty() -> Text(text = stringResource(Res.string.tax_detail_no_rules), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> taxRules.forEachIndexed { index, rule ->
                    TaxRuleItem(rule = rule)
                    if (index < taxRules.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun TaxNotesCard(
    taxCode: TaxCode,
    isEditing: Boolean,
    isSaving: Boolean,
    saveError: String?,
    onEditNotes: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var notesText by remember(taxCode.notes) { mutableStateOf(taxCode.notes ?: "") }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(Res.string.tax_detail_section_notes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (!isEditing) {
                    IconButton(onClick = onEditNotes) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.tax_detail_cd_edit_notes))
                    }
                }
            }
            if (isEditing) {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.tax_detail_notes_placeholder)) },
                    minLines = 3,
                    maxLines = 6
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onCancelEdit, enabled = !isSaving) {
                        Text(stringResource(Res.string.tax_detail_cancel_notes))
                    }
                    Button(onClick = { onSaveNotes(notesText) }, enabled = !isSaving) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Save, contentDescription = stringResource(Res.string.tax_detail_cd_save), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.tax_detail_save_notes))
                    }
                }
            } else {
                if (taxCode.notes.isNullOrBlank()) {
                    Text(text = stringResource(Res.string.tax_detail_no_notes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(text = taxCode.notes!!, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (saveError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = saveError, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = onClearError) { Text(stringResource(Res.string.tax_dismiss)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun UnsubscribeConfirmationDialog(taxCode: TaxCode, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(Res.string.tax_unsub_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.tax_unsub_dialog_text))
                Text(text = "${taxCode.code} - ${taxCode.description}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                if (taxCode.usageCount > 0) {
                    Text(
                        text = stringResource(Res.string.tax_unsub_used_times_format, taxCode.usageCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(Res.string.tax_unsub_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.tax_cancel)) }
        }
    )
}

@Composable
private fun TaxRuleItem(rule: com.ampairs.tax.domain.model.TaxRule, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(Res.string.tax_rule_jurisdiction_format, rule.jurisdiction), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = rule.jurisdictionLevel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(Res.string.tax_rule_tax_code_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${rule.taxCode} (${rule.taxCodeType})", style = MaterialTheme.typography.bodySmall)
        }
        if (rule.taxCodeDescription != null) {
            Text(text = rule.taxCodeDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        if (rule.componentComposition.isNotEmpty()) {
            Text(text = stringResource(Res.string.tax_rule_components_label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
            rule.componentComposition.forEach { (scenarioKey, composition) ->
                val scenarioLabel = when (scenarioKey) {
                    "intra_state" -> stringResource(Res.string.tax_rule_intra_state)
                    "inter_state" -> stringResource(Res.string.tax_rule_inter_state)
                    "standard" -> stringResource(Res.string.tax_rule_standard)
                    "b2b" -> stringResource(Res.string.tax_rule_b2b)
                    "b2c" -> stringResource(Res.string.tax_rule_b2c)
                    else -> scenarioKey.replace("_", " ").replaceFirstChar { it.uppercase() }
                }
                Text(text = "$scenarioLabel (Total: ${composition.totalRate}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                composition.components.forEach { component ->
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = component.name, style = MaterialTheme.typography.bodySmall)
                        Text(text = "${component.rate}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (rule.isActive) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = stringResource(Res.string.tax_rule_active), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

private fun formatTimestamp(timestamp: Instant): String =
    formatTimestamp(timestamp.toEpochMilliseconds())

private fun formatTimestamp(timestamp: Long): String {
    val seconds = timestamp / 1000
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 365 -> "${days / 365} year(s) ago"
        days > 30 -> "${days / 30} month(s) ago"
        days > 0 -> "$days day(s) ago"
        hours > 0 -> "$hours hour(s) ago"
        minutes > 0 -> "$minutes minute(s) ago"
        else -> "Just now"
    }
}
