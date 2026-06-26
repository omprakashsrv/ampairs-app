package com.ampairs.product.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.form.render.ConfigAttributesSection
import com.ampairs.form.ui.FormAgentChatDialog
import com.ampairs.form.ui.FormAgentViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.ampairs.product.domain.Group
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductType
import com.ampairs.product.domain.ServiceType
import com.ampairs.product.ui.images.ProductImageManagementScreen
import com.ampairs.product.ui.images.ProductImageViewModel
import com.ampairs.unit.domain.model.Unit as UnitDomain
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    onSaveSuccess: () -> Unit,
    onManageVariants: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProductFormViewModel = assistedMetroViewModel<ProductFormViewModel, ProductFormViewModel.Factory> { create(productId) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // AI form assistant: speak the product details and let the on-device model fill the fields.
    val formAgent = assistedMetroViewModel<FormAgentViewModel, FormAgentViewModel.Factory>(key = "product") { create("product") }
    var showAssistant by remember { mutableStateOf(false) }
    LaunchedEffect(formAgent) {
        formAgent.fills.collect { fill -> viewModel.updateField(fill.fieldKey, fill.value) }
    }

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct()
        }
    }

    val newProductTitle = stringResource(Res.string.prod_form_new_title)
    val editProductTitle = stringResource(Res.string.prod_form_edit_title)

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (productId == null) newProductTitle else editProductTitle) },
            actions = {
                IconButton(onClick = { showAssistant = true }) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(Res.string.prod_ai_fill),
                    )
                }
                TextButton(
                    onClick = { viewModel.saveProduct { onSaveSuccess() } },
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.prod_save))
                    }
                }
            }
        )

        if (showAssistant) {
            FormAgentChatDialog(
                entityType = "product",
                viewModel = formAgent,
                onDismiss = { showAssistant = false },
            )
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                ProductFormLayout(
                    productId = productId,
                    uiState = uiState,
                    onFormChange = viewModel::updateForm,
                    onSave = { viewModel.saveProduct { onSaveSuccess() } },
                    canSave = uiState.canSave && !uiState.isSaving,
                    isSaving = uiState.isSaving,
                    onManageVariants = onManageVariants,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Layout selector:
 * - productId == null          → just the details form (save first before images)
 * - productId != null, mobile  → tab layout (Details | Images)
 * - productId != null, desktop → side-by-side layout (form left, images right)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormLayout(
    productId: String?,
    uiState: ProductFormUiState,
    onFormChange: (ProductFormState) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    onManageVariants: ((String, String) -> Unit)?,
    viewModel: ProductFormViewModel,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactOrMedium = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    when {
        productId != null && isCompactOrMedium -> {
            // Mobile: tabs
            ProductFormTabLayout(
                productId = productId,
                uiState = uiState,
                onFormChange = onFormChange,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                onManageVariants = onManageVariants,
                viewModel = viewModel,
                modifier = modifier,
            )
        }
        productId != null -> {
            // Desktop: side-by-side
            ProductFormSideBySideLayout(
                productId = productId,
                uiState = uiState,
                onFormChange = onFormChange,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                onManageVariants = onManageVariants,
                viewModel = viewModel,
                modifier = modifier,
            )
        }
        else -> {
            // New product: form only, no images until saved
            ProductDetailsForm(
                productId = null,
                uiState = uiState,
                onFormChange = onFormChange,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                onManageVariants = onManageVariants,
                viewModel = viewModel,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormTabLayout(
    productId: String,
    uiState: ProductFormUiState,
    onFormChange: (ProductFormState) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    onManageVariants: ((String, String) -> Unit)?,
    viewModel: ProductFormViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabDetails = stringResource(Res.string.prod_form_tab_details)
    val tabImages = stringResource(Res.string.prod_form_tab_images)

    Column(modifier = modifier) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(tabDetails) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(tabImages) })
        }

        when (selectedTab) {
            0 -> ProductDetailsForm(
                productId = productId,
                uiState = uiState,
                onFormChange = onFormChange,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                onManageVariants = onManageVariants,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
            1 -> ProductImageManagementScreen(
                productId = productId,
                readOnly = false,
                viewModel = assistedMetroViewModel<ProductImageViewModel, ProductImageViewModel.Factory>(key = productId) { create(productId) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ProductFormSideBySideLayout(
    productId: String,
    uiState: ProductFormUiState,
    onFormChange: (ProductFormState) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    onManageVariants: ((String, String) -> Unit)?,
    viewModel: ProductFormViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedCard(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            ProductDetailsForm(
                productId = productId,
                uiState = uiState,
                onFormChange = onFormChange,
                onSave = onSave,
                canSave = canSave,
                isSaving = isSaving,
                onManageVariants = onManageVariants,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedCard(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        ) {
            ProductImageManagementScreen(
                productId = productId,
                readOnly = false,
                viewModel = assistedMetroViewModel<ProductImageViewModel, ProductImageViewModel.Factory>(key = productId) { create(productId) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ProductDetailsForm(
    productId: String?,
    uiState: ProductFormUiState,
    onFormChange: (ProductFormState) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    onManageVariants: ((String, String) -> Unit)?,
    viewModel: ProductFormViewModel,
    modifier: Modifier = Modifier
) {
    val formState = uiState.formState
    val focusManager = LocalFocusManager.current
    val activeLabel = stringResource(Res.string.prod_active)
    val inactiveLabel = stringResource(Res.string.prod_inactive)
    val selectTypeLabel = stringResource(Res.string.prod_form_select_type)
    val clearSelectionLabel = stringResource(Res.string.prod_form_clear_selection)
    val selectNoneLabel = stringResource(Res.string.prod_form_select_none)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.error != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = uiState.error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        FormSection(title = stringResource(Res.string.prod_section_basic)) {
            OutlinedTextField(
                value = formState.name,
                onValueChange = { onFormChange(formState.copy(name = it)) },
                label = { Text(stringResource(Res.string.prod_form_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                singleLine = true
            )

            OutlinedTextField(
                value = formState.code,
                onValueChange = { onFormChange(formState.copy(code = it)) },
                label = { Text(stringResource(Res.string.prod_form_code_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                isError = formState.codeError != null,
                supportingText = formState.codeError?.let { { Text(it) } },
                singleLine = true
            )

            OutlinedTextField(
                value = formState.description,
                onValueChange = { onFormChange(formState.copy(description = it)) },
                label = { Text(stringResource(Res.string.prod_label_description)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true
            )

            TaxCodeAutocomplete(
                selectedTaxCode = formState.taxCode,
                taxCodeDescription = formState.taxCodeDescription,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            var expandedActive by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expandedActive,
                onExpandedChange = { expandedActive = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (formState.active) activeLabel else inactiveLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.prod_label_status)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActive) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(expanded = expandedActive, onDismissRequest = { expandedActive = false }) {
                    DropdownMenuItem(text = { Text(activeLabel) }, onClick = {
                        onFormChange(formState.copy(active = true)); expandedActive = false
                    })
                    DropdownMenuItem(text = { Text(inactiveLabel) }, onClick = {
                        onFormChange(formState.copy(active = false)); expandedActive = false
                    })
                }
            }
        }

        FormSection(title = stringResource(Res.string.prod_section_classification)) {
            var expandedProductType by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expandedProductType,
                onExpandedChange = { expandedProductType = it }
            ) {
                OutlinedTextField(
                    value = formState.productType?.displayName ?: selectTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.prod_label_product_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductType) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(expanded = expandedProductType, onDismissRequest = { expandedProductType = false }) {
                    ProductType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) }, onClick = {
                            onFormChange(formState.copy(productType = type)); expandedProductType = false
                        })
                    }
                    DropdownMenuItem(text = { Text(clearSelectionLabel) }, onClick = {
                        onFormChange(formState.copy(productType = null)); expandedProductType = false
                    })
                }
            }

            var expandedServiceType by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expandedServiceType,
                onExpandedChange = { expandedServiceType = it }
            ) {
                OutlinedTextField(
                    value = formState.serviceType?.displayName ?: selectTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.prod_label_service_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServiceType) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(expanded = expandedServiceType, onDismissRequest = { expandedServiceType = false }) {
                    ServiceType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName) }, onClick = {
                            onFormChange(formState.copy(serviceType = type)); expandedServiceType = false
                        })
                    }
                    DropdownMenuItem(text = { Text(clearSelectionLabel) }, onClick = {
                        onFormChange(formState.copy(serviceType = null)); expandedServiceType = false
                    })
                }
            }
        }

        FormSection(title = stringResource(Res.string.prod_section_category_brand)) {
            GroupDropdown(
                label = stringResource(Res.string.prod_label_category),
                selectedName = formState.categoryName.ifBlank { null },
                options = uiState.categories,
                onSelect = viewModel::onCategorySelected,
                onClear = viewModel::clearCategory,
                noneLabel = selectNoneLabel
            )

            GroupDropdown(
                label = stringResource(Res.string.prod_label_brand),
                selectedName = formState.brandName.ifBlank { null },
                options = uiState.brands,
                onSelect = viewModel::onBrandSelected,
                onClear = viewModel::clearBrand,
                noneLabel = selectNoneLabel
            )

            GroupDropdown(
                label = stringResource(Res.string.prod_label_group),
                selectedName = formState.groupName.ifBlank { null },
                options = uiState.groups,
                onSelect = viewModel::onGroupSelected,
                onClear = viewModel::clearGroup,
                noneLabel = selectNoneLabel
            )

            GroupDropdown(
                label = stringResource(Res.string.prod_label_sub_category),
                selectedName = formState.subCategoryName.ifBlank { null },
                options = uiState.subCategories,
                onSelect = viewModel::onSubCategorySelected,
                onClear = viewModel::clearSubCategory,
                noneLabel = selectNoneLabel
            )
        }

        FormSection(title = stringResource(Res.string.prod_section_unit)) {
            UnitDropdown(
                label = stringResource(Res.string.prod_label_base_unit),
                selectedName = formState.baseUnitName.ifBlank { null },
                options = uiState.units,
                onSelect = viewModel::onUnitSelected,
                onClear = viewModel::clearUnit,
                noneLabel = selectNoneLabel
            )
        }

        FormSection(title = stringResource(Res.string.prod_section_variants)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newValue = !formState.hasVariants
                        onFormChange(formState.copy(hasVariants = newValue))
                        if (newValue && productId != null && onManageVariants != null) {
                            onManageVariants(productId, formState.name)
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(checked = formState.hasVariants, onCheckedChange = null)
                Column {
                    Text(text = stringResource(Res.string.prod_form_has_variants_label), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(Res.string.prod_form_has_variants_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (formState.hasVariants) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (productId == null) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (productId == null) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(24.dp))
                            Text(text = stringResource(Res.string.prod_form_save_first_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(text = stringResource(Res.string.prod_form_save_first_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        } else {
                            Text(text = stringResource(Res.string.prod_form_manage_variants_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            Text(text = stringResource(Res.string.prod_form_manage_variants_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (onManageVariants != null) {
                                OutlinedButton(onClick = { onManageVariants(productId, formState.name) }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(Res.string.prod_manage_variants))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!formState.hasVariants) {
            FormSection(title = stringResource(Res.string.prod_section_pricing)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.mrp.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { price -> onFormChange(formState.copy(mrp = price)) } },
                        label = { Text(stringResource(Res.string.prod_label_mrp)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formState.dp.toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { price -> onFormChange(formState.copy(dp = price)) } },
                        label = { Text(stringResource(Res.string.prod_label_dealer_price)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = formState.sellingPrice.toString(),
                    onValueChange = { it.toDoubleOrNull()?.let { price -> onFormChange(formState.copy(sellingPrice = price)) } },
                    label = { Text(stringResource(Res.string.prod_label_selling_price)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    isError = formState.priceError != null,
                    supportingText = formState.priceError?.let { { Text(it) } },
                    singleLine = true
                )
            }

            FormSection(title = stringResource(Res.string.prod_section_stock_mgmt)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.stockQuantity?.toString() ?: "",
                        onValueChange = {
                            val quantity = if (it.isBlank()) null else it.toDoubleOrNull()
                            onFormChange(formState.copy(stockQuantity = quantity))
                        },
                        label = { Text(stringResource(Res.string.prod_label_current_stock)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        placeholder = { Text(stringResource(Res.string.prod_form_optional_placeholder)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formState.lowStockAlert?.toString() ?: "",
                        onValueChange = {
                            val alert = if (it.isBlank()) null else it.toDoubleOrNull()
                            onFormChange(formState.copy(lowStockAlert = alert))
                        },
                        label = { Text(stringResource(Res.string.prod_label_low_stock_alert)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        placeholder = { Text(stringResource(Res.string.prod_form_optional_placeholder)) },
                        singleLine = true
                    )
                }
                if (formState.stockError != null) {
                    Text(text = formState.stockError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Config-driven custom attributes (spec 011, US5) — rendered from the product FormSchema.
        val formSchema by viewModel.formSchema.collectAsStateWithLifecycle()
        ConfigAttributesSection(
            schema = formSchema,
            optionRegistry = viewModel.optionRegistry,
            widgetRegistry = viewModel.widgetRegistry,
            attributes = formState.attributes,
            onAttributesChange = { onFormChange(formState.copy(attributes = it)) },
            title = stringResource(Res.string.prod_section_attributes),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.prod_form_saving))
                } else {
                    Text(stringResource(Res.string.prod_form_save_product))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDropdown(
    label: String,
    selectedName: String?,
    options: List<Group>,
    onSelect: (Group) -> kotlin.Unit,
    onClear: () -> kotlin.Unit,
    noneLabel: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            placeholder = { Text(noneLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { onClear(); expanded = false }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    label: String,
    selectedName: String?,
    options: List<UnitDomain>,
    onSelect: (UnitDomain) -> kotlin.Unit,
    onClear: () -> kotlin.Unit,
    noneLabel: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            placeholder = { Text(noneLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { onClear(); expanded = false }
            )
            options.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(unit.name, style = MaterialTheme.typography.bodyMedium)
                            if (unit.shortName.isNotBlank()) {
                                Text(unit.shortName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = { onSelect(unit); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

data class ProductFormState(
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val taxCode: String = "",
    val taxCodeDescription: String? = null,
    val active: Boolean = true,
    val productType: ProductType? = null,
    val serviceType: ServiceType? = null,
    val hasVariants: Boolean = false,
    val mrp: Double = 0.0,
    val dp: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stockQuantity: Double? = null,
    val lowStockAlert: Double? = null,
    val categoryId: String = "",
    val categoryName: String = "",
    val brandId: String = "",
    val brandName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val subCategoryId: String = "",
    val subCategoryName: String = "",
    val baseUnitId: String = "",
    val baseUnitName: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val nameError: String? = null,
    val codeError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && code.isNotBlank() &&
                nameError == null && codeError == null &&
                priceError == null && stockError == null
}

fun Product.toFormState(): ProductFormState {
    return ProductFormState(
        name = this.name,
        code = this.code,
        description = this.description,
        taxCode = this.taxCode,
        active = this.active,
        productType = this.productType,
        serviceType = this.serviceType,
        hasVariants = this.hasVariants,
        mrp = this.mrp,
        dp = this.dp,
        sellingPrice = this.sellingPrice,
        stockQuantity = this.stockQuantity,
        lowStockAlert = this.lowStockAlert,
        categoryId = this.categoryId,
        categoryName = this.categoryName ?: "",
        brandId = this.brandId,
        brandName = this.brandName ?: "",
        groupId = this.groupId,
        groupName = "",
        subCategoryId = this.subCategoryId,
        subCategoryName = "",
        baseUnitId = this.baseUnitId ?: "",
        baseUnitName = "",
        attributes = this.attributes,
    )
}

fun ProductFormState.toProduct(): Product {
    return Product(
        name = this.name,
        code = this.code,
        description = this.description,
        taxCode = this.taxCode,
        active = this.active,
        productType = this.productType,
        serviceType = this.serviceType,
        hasVariants = this.hasVariants,
        mrp = this.mrp,
        dp = this.dp,
        sellingPrice = this.sellingPrice,
        stockQuantity = this.stockQuantity,
        lowStockAlert = this.lowStockAlert,
        categoryId = this.categoryId,
        categoryName = this.categoryName.takeIf { it.isNotBlank() },
        brandId = this.brandId,
        brandName = this.brandName.takeIf { it.isNotBlank() },
        groupId = this.groupId,
        subCategoryId = this.subCategoryId,
        baseUnitId = this.baseUnitId.takeIf { it.isNotBlank() },
        attributes = this.attributes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCodeAutocomplete(
    selectedTaxCode: String,
    taxCodeDescription: String?,
    viewModel: ProductFormViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.taxCodeSearchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.taxCodeSuggestions.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        OutlinedTextField(
            value = if (selectedTaxCode.isNotEmpty()) {
                taxCodeDescription ?: selectedTaxCode
            } else {
                searchQuery
            },
            onValueChange = {
                if (selectedTaxCode.isEmpty()) {
                    viewModel.onTaxCodeSearchQueryChange(it)
                    expanded = suggestions.isNotEmpty()
                }
            },
            label = { Text(stringResource(Res.string.prod_label_tax_code)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (selectedTaxCode.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearTaxCode(); expanded = false }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.prod_form_clear_tax_code))
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.prod_form_cd_search))
                }
            },
            placeholder = { Text(stringResource(Res.string.prod_form_tax_search_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            colors = OutlinedTextFieldDefaults.colors()
        )

        if (expanded && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(suggestions) { taxCode ->
                        ListItem(
                            headlineContent = { Text(text = taxCode.code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
                            supportingContent = { Text(text = taxCode.shortDescription, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
                            trailingContent = { Text(text = taxCode.codeType.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.clickable { viewModel.selectTaxCode(taxCode); expanded = false }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
