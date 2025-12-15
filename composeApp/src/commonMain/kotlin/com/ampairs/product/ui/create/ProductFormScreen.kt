package com.ampairs.product.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductImage
import com.ampairs.product.domain.ProductType
import com.ampairs.product.domain.ServiceType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    onSaveSuccess: () -> Unit,
    onManageVariants: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProductFormViewModel = koinViewModel { parametersOf(productId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (productId == null) "New Product" else "Edit Product") },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveProduct { onSaveSuccess() }
                    },
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                ProductForm(
                    productId = productId,
                    formState = uiState.formState,
                    onFormChange = viewModel::updateForm,
                    error = uiState.error,
                    onAddImage = viewModel::addImage,
                    onRemoveImage = viewModel::removeImage,
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

@Composable
private fun ProductForm(
    productId: String?,
    formState: ProductFormState,
    onFormChange: (ProductFormState) -> Unit,
    error: String?,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    onManageVariants: ((String, String) -> Unit)?,
    viewModel: ProductFormViewModel,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Basic Information
        FormSection(title = "Basic Information") {
            OutlinedTextField(
                value = formState.name,
                onValueChange = { onFormChange(formState.copy(name = it)) },
                label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                singleLine = true
            )

            OutlinedTextField(
                value = formState.code,
                onValueChange = { onFormChange(formState.copy(code = it)) },
                label = { Text("Product Code *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                isError = formState.codeError != null,
                supportingText = formState.codeError?.let { { Text(it) } },
                singleLine = true
            )

            OutlinedTextField(
                value = formState.description,
                onValueChange = { onFormChange(formState.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            // Tax Code with Autocomplete
            TaxCodeAutocomplete(
                selectedTaxCode = formState.taxCode,
                taxCodeDescription = formState.taxCodeDescription,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var expandedActive by remember { mutableStateOf(false) }

                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = expandedActive,
                    onExpandedChange = { expandedActive = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (formState.active) "Active" else "Inactive",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedActive) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedActive,
                        onDismissRequest = { expandedActive = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Active") },
                            onClick = {
                                onFormChange(formState.copy(active = true))
                                expandedActive = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Inactive") },
                            onClick = {
                                onFormChange(formState.copy(active = false))
                                expandedActive = false
                            }
                        )
                    }
                }
            }
        }

        // Classification
        FormSection(title = "Classification") {
            // Product Type Dropdown
            var expandedProductType by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expandedProductType,
                onExpandedChange = { expandedProductType = it }
            ) {
                OutlinedTextField(
                    value = formState.productType?.displayName ?: "Select Type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Product Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductType) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = expandedProductType,
                    onDismissRequest = { expandedProductType = false }
                ) {
                    ProductType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onFormChange(formState.copy(productType = type))
                                expandedProductType = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Clear Selection") },
                        onClick = {
                            onFormChange(formState.copy(productType = null))
                            expandedProductType = false
                        }
                    )
                }
            }

            // Service Type Dropdown
            var expandedServiceType by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expandedServiceType,
                onExpandedChange = { expandedServiceType = it }
            ) {
                OutlinedTextField(
                    value = formState.serviceType?.displayName ?: "Select Type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServiceType) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = expandedServiceType,
                    onDismissRequest = { expandedServiceType = false }
                ) {
                    ServiceType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onFormChange(formState.copy(serviceType = type))
                                expandedServiceType = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Clear Selection") },
                        onClick = {
                            onFormChange(formState.copy(serviceType = null))
                            expandedServiceType = false
                        }
                    )
                }
            }
        }

        // Category Information
        FormSection(title = "Category & Brand") {
            OutlinedTextField(
                value = formState.categoryId,
                onValueChange = { onFormChange(formState.copy(categoryId = it)) },
                label = { Text("Category ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = formState.brandId,
                onValueChange = { onFormChange(formState.copy(brandId = it)) },
                label = { Text("Brand ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = formState.groupId,
                onValueChange = { onFormChange(formState.copy(groupId = it)) },
                label = { Text("Group ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = formState.subCategoryId,
                onValueChange = { onFormChange(formState.copy(subCategoryId = it)) },
                label = { Text("Sub Category ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )
        }

        // Product Images
        FormSection(title = "Product Images") {
            ProductImageSection(
                images = formState.images,
                onAddImage = onAddImage,
                onRemoveImage = onRemoveImage
            )
        }

        // Unit Information
        FormSection(title = "Unit Information") {
            OutlinedTextField(
                value = formState.baseUnitId,
                onValueChange = { onFormChange(formState.copy(baseUnitId = it)) },
                label = { Text("Base Unit ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )
        }

        // Variants Section
        FormSection(title = "Product Variants") {
            // Has Variants Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newValue = !formState.hasVariants
                        onFormChange(formState.copy(hasVariants = newValue))
                        // If enabling variants and product is saved, navigate to variant management
                        if (newValue && productId != null && onManageVariants != null) {
                            onManageVariants(productId, formState.name)
                        }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = formState.hasVariants,
                    onCheckedChange = null // Handled by Row click
                )
                Column {
                    Text(
                        text = "This product has variants",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Enable if product comes in different sizes, colors, or configurations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Show variant management card when hasVariants is enabled
            if (formState.hasVariants) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (productId == null) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (productId == null) {
                            // Product not yet created - show instruction
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Save product first to add variants",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "After saving this product, you'll be able to add and manage variants like sizes, colors, and other options.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        } else {
                            // Product exists - show manage button
                            Text(
                                text = "Manage Product Variants",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Add and manage sizes, colors, and other variant options. At least one variant is required.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (onManageVariants != null) {
                                OutlinedButton(
                                    onClick = { onManageVariants(productId, formState.name) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Manage Variants")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pricing Information (hidden when product has variants)
        if (!formState.hasVariants) {
            FormSection(title = "Pricing") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.mrp.toString(),
                    onValueChange = {
                        it.toDoubleOrNull()?.let { price ->
                            onFormChange(formState.copy(mrp = price))
                        }
                    },
                    label = { Text("MRP") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formState.dp.toString(),
                    onValueChange = {
                        it.toDoubleOrNull()?.let { price ->
                            onFormChange(formState.copy(dp = price))
                        }
                    },
                    label = { Text("Dealer Price") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = formState.sellingPrice.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { price ->
                        onFormChange(formState.copy(sellingPrice = price))
                    }
                },
                label = { Text("Selling Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                isError = formState.priceError != null,
                supportingText = formState.priceError?.let { { Text(it) } },
                singleLine = true
            )
            }

            // Stock Management
            FormSection(title = "Stock Management") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.stockQuantity?.toString() ?: "",
                    onValueChange = {
                        val quantity = if (it.isBlank()) null else it.toDoubleOrNull()
                        onFormChange(formState.copy(stockQuantity = quantity))
                    },
                    label = { Text("Current Stock") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    placeholder = { Text("Optional") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = formState.lowStockAlert?.toString() ?: "",
                    onValueChange = {
                        val alert = if (it.isBlank()) null else it.toDoubleOrNull()
                        onFormChange(formState.copy(lowStockAlert = alert))
                    },
                    label = { Text("Low Stock Alert") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    placeholder = { Text("Optional") },
                    singleLine = true
                )
            }

            if (formState.stockError != null) {
                Text(
                    text = formState.stockError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            }
        }

        // Save Button Section
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Product")
                }
            }
        }
    }
}

@Composable
private fun ProductImageSection(
    images: List<ProductImage>,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Images (${images.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onAddImage,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Image",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Image", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (images.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(images.size) { index ->
                    ProductImageCard(
                        image = images[index],
                        onRemove = { onRemoveImage(index) }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No images added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductImageCard(
    image: ProductImage,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box {
            // Display placeholder for product image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // For now, we show a placeholder icon
                // In the future, we can add actual image loading with rememberImagePainter
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Product Image",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove Image",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
    val brandId: String = "",
    val groupId: String = "",
    val subCategoryId: String = "",
    val baseUnitId: String = "",
    val images: List<ProductImage> = emptyList(),
    val nameError: String? = null,
    val codeError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null
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
        brandId = this.brandId,
        groupId = this.groupId,
        subCategoryId = this.subCategoryId,
        baseUnitId = this.baseUnitId ?: "",
        images = this.images ?: emptyList()
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
        brandId = this.brandId,
        groupId = this.groupId,
        subCategoryId = this.subCategoryId,
        baseUnitId = this.baseUnitId.takeIf { it.isNotBlank() },
        images = this.images
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
    val searchQuery by viewModel.taxCodeSearchQuery.collectAsState()
    val suggestions by viewModel.taxCodeSuggestions.collectAsState()
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
            label = { Text("Tax Code") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (selectedTaxCode.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.clearTaxCode()
                        expanded = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear tax code")
                    }
                } else if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            placeholder = { Text("Search tax codes...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) }
            ),
            colors = OutlinedTextFieldDefaults.colors()
        )

        // Dropdown suggestions
        if (expanded && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(suggestions) { taxCode ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = taxCode.code,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = taxCode.shortDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = taxCode.codeType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.selectTaxCode(taxCode)
                                expanded = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}