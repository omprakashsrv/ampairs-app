package com.ampairs.product.ui.create

import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.ProductImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    onSaveSuccess: () -> Unit,
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
                    formState = uiState.formState,
                    onFormChange = viewModel::updateForm,
                    error = uiState.error,
                    onAddImage = viewModel::addImage,
                    onRemoveImage = viewModel::removeImage,
                    onSave = { viewModel.saveProduct { onSaveSuccess() } },
                    canSave = uiState.canSave && !uiState.isSaving,
                    isSaving = uiState.isSaving,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ProductForm(
    formState: ProductFormState,
    onFormChange: (ProductFormState) -> Unit,
    error: String?,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
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

            OutlinedTextField(
                value = formState.taxCode,
                onValueChange = { onFormChange(formState.copy(taxCode = it)) },
                label = { Text("Tax Code") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
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

        // Pricing Information
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
    Card(
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
    val active: Boolean = true,
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