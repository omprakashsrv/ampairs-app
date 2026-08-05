package com.ampairs.product.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.db.Constants
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.domain.Group
import com.ampairs.product.domain.asCategoryGroupDomainModel
import com.ampairs.product.domain.asGroupDomainModel
import com.ampairs.form.data.repository.ConfigLookup
import com.ampairs.form.domain.FormSchema
import com.ampairs.form.render.CustomFieldWidgetRegistry
import com.ampairs.form.render.DynamicOptionRegistry
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.tax.data.repository.TaxCodeLookup
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.unit.data.repository.UnitLookup
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.ampairs.unit.domain.model.Unit as UnitDomain

@AssistedInject
@OptIn(FlowPreview::class)
class ProductFormViewModel(
    @Assisted private val productId: String?,
    private val productRepository: ProductRepository,
    private val taxCodeRepository: TaxCodeLookup,
    private val syncService: CentralSyncService,
    private val categoryDao: CategoryDao,
    private val brandDao: BrandDao,
    private val groupDao: GroupDao,
    private val subCategoryDao: SubCategoryDao,
    private val unitLookup: UnitLookup,
    private val configRepository: ConfigLookup,
    val optionRegistry: DynamicOptionRegistry,
    val widgetRegistry: CustomFieldWidgetRegistry,
) : ViewModel() {

    /**
     * Live unified schema for the product form (spec 011, US5). The schema-driven attributes
     * section consumes this together with [optionRegistry] / [widgetRegistry].
     */
    val formSchema: StateFlow<FormSchema?> =
        configRepository.observeSchema("product")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String?): ProductFormViewModel
    }

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _taxCodeSearchQuery = MutableStateFlow("")
    val taxCodeSearchQuery: StateFlow<String> = _taxCodeSearchQuery.asStateFlow()

    private val _taxCodeSuggestions = MutableStateFlow<List<TaxCode>>(emptyList())
    val taxCodeSuggestions: StateFlow<List<TaxCode>> = _taxCodeSuggestions.asStateFlow()

    init {
        viewModelScope.launch {
            _taxCodeSearchQuery
                .debounce(300)
                .collect { query -> searchTaxCodes(query) }
        }
        loadDropdownData()
        // Pull the form schema so the config-driven attributes section is current.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.FORM))
    }

    private fun loadDropdownData() {
        viewModelScope.launch {
            try {
                val categories = categoryDao.getActiveCategories().asCategoryGroupDomainModel()
                val brands = brandDao.getActiveBrands().map { Group(id = it.id, name = it.name, active = it.active == 1) }
                val groups = groupDao.getActiveGroups().asGroupDomainModel()
                val subCategories = subCategoryDao.getActiveSubCategories()
                    .map { Group(id = it.id, name = it.name, active = it.active == 1) }
                val units = unitLookup.getActiveUnits()
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    brands = brands,
                    groups = groups,
                    subCategories = subCategories,
                    units = units,
                )
                resolveDropdownNamesIfNeeded()
            } catch (_: Exception) {}
        }
    }

    private fun resolveDropdownNamesIfNeeded() {
        val state = _uiState.value
        val form = state.formState
        var updated = form

        if (form.categoryName.isBlank() && form.categoryId.isNotBlank()) {
            state.categories.firstOrNull { it.id == form.categoryId }?.let {
                updated = updated.copy(categoryName = it.name)
            }
        }
        if (form.brandName.isBlank() && form.brandId.isNotBlank()) {
            state.brands.firstOrNull { it.id == form.brandId }?.let {
                updated = updated.copy(brandName = it.name)
            }
        }
        if (form.groupName.isBlank() && form.groupId.isNotBlank()) {
            state.groups.firstOrNull { it.id == form.groupId }?.let {
                updated = updated.copy(groupName = it.name)
            }
        }
        if (form.subCategoryName.isBlank() && form.subCategoryId.isNotBlank()) {
            state.subCategories.firstOrNull { it.id == form.subCategoryId }?.let {
                updated = updated.copy(subCategoryName = it.name)
            }
        }
        if (form.baseUnitName.isBlank() && form.baseUnitId.isNotBlank()) {
            state.units.firstOrNull { it.uid == form.baseUnitId }?.let {
                updated = updated.copy(baseUnitName = it.name)
            }
        }
        if (updated != form) {
            _uiState.value = _uiState.value.copy(formState = updated)
        }
    }

    fun loadProduct() {
        if (productId == null) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        productRepository.observeProduct(productId)
            .onEach { product ->
                if (product != null) {
                    val formState = product.toFormState()
                    val taxCodeDescription = if (product.taxCode.isNotBlank()) {
                        try {
                            val taxCodes = taxCodeRepository.searchWorkspaceTaxCodes(product.taxCode, limit = 1)
                            taxCodes.firstOrNull()?.let { "${it.code} - ${it.shortDescription}" }
                        } catch (_: Exception) { null }
                    } else null
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        formState = formState.copy(taxCodeDescription = taxCodeDescription)
                    )
                    resolveDropdownNamesIfNeeded()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Product not found")
                }
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load product")
            }
            .launchIn(viewModelScope)
    }

    fun updateForm(newFormState: ProductFormState) {
        val validatedFormState = validateForm(newFormState)
        _uiState.value = _uiState.value.copy(
            formState = validatedFormState,
            canSave = validatedFormState.isValid,
            error = null
        )
    }

    /**
     * Apply a single field value predicted by the form assistant (keyed by the form-schema `fieldKey`).
     * Standard keys map to [ProductFormState]; relational keys (category/brand/group/sub-category/unit)
     * resolve the spoken name to its id via the loaded dropdowns; anything else becomes a custom
     * attribute. Accepts camelCase and snake_case. Reuses [updateForm] for validation. The product
     * form's standard inputs bind directly to formState, so filled values render immediately.
     */
    fun updateField(fieldKey: String, value: String) {
        val s = _uiState.value
        val f = s.formState
        val updated = when (fieldKey) {
            "name" -> f.copy(name = value)
            "code" -> f.copy(code = value)
            "description" -> f.copy(description = value)
            "taxCode", "tax_code", "hsn", "hsnCode", "hsn_code" -> f.copy(taxCode = value, taxCodeDescription = null)
            "mrp" -> f.copy(mrp = value.toDoubleOrNull() ?: f.mrp)
            "dp", "dealerPrice", "dealer_price" -> f.copy(dp = value.toDoubleOrNull() ?: f.dp)
            "sellingPrice", "selling_price", "price" -> f.copy(sellingPrice = value.toDoubleOrNull() ?: f.sellingPrice)
            "stockQuantity", "stock_quantity", "stock" -> f.copy(stockQuantity = value.toDoubleOrNull() ?: f.stockQuantity)
            "lowStockAlert", "low_stock_alert" -> f.copy(lowStockAlert = value.toDoubleOrNull() ?: f.lowStockAlert)
            "category", "categoryName", "category_name" -> {
                val m = s.categories.firstOrNull { it.name.equals(value, ignoreCase = true) }
                f.copy(categoryId = m?.id ?: f.categoryId, categoryName = m?.name ?: value)
            }
            "brand", "brandName", "brand_name" -> {
                val m = s.brands.firstOrNull { it.name.equals(value, ignoreCase = true) }
                f.copy(brandId = m?.id ?: f.brandId, brandName = m?.name ?: value)
            }
            "group", "groupName", "group_name" -> {
                val m = s.groups.firstOrNull { it.name.equals(value, ignoreCase = true) }
                f.copy(groupId = m?.id ?: f.groupId, groupName = m?.name ?: value)
            }
            "subCategory", "subCategoryName", "sub_category" -> {
                val m = s.subCategories.firstOrNull { it.name.equals(value, ignoreCase = true) }
                f.copy(subCategoryId = m?.id ?: f.subCategoryId, subCategoryName = m?.name ?: value)
            }
            "unit", "baseUnit", "base_unit", "baseUnitName", "unitName" -> {
                val m = s.units.firstOrNull { it.name.equals(value, ignoreCase = true) }
                f.copy(baseUnitId = m?.uid ?: f.baseUnitId, baseUnitName = m?.name ?: value)
            }
            else -> f.copy(attributes = f.attributes + (fieldKey to value))
        }
        updateForm(updated)
    }

    fun onCategorySelected(category: Group) {
        updateForm(_uiState.value.formState.copy(categoryId = category.id, categoryName = category.name))
    }

    fun onBrandSelected(brand: Group) {
        updateForm(_uiState.value.formState.copy(brandId = brand.id, brandName = brand.name))
    }

    fun onGroupSelected(group: Group) {
        updateForm(_uiState.value.formState.copy(groupId = group.id, groupName = group.name))
    }

    fun onSubCategorySelected(subCategory: Group) {
        updateForm(_uiState.value.formState.copy(subCategoryId = subCategory.id, subCategoryName = subCategory.name))
    }

    fun onUnitSelected(unit: UnitDomain) {
        updateForm(_uiState.value.formState.copy(baseUnitId = unit.uid, baseUnitName = unit.name))
    }

    fun clearCategory() {
        updateForm(_uiState.value.formState.copy(categoryId = "", categoryName = ""))
    }

    fun clearBrand() {
        updateForm(_uiState.value.formState.copy(brandId = "", brandName = ""))
    }

    fun clearGroup() {
        updateForm(_uiState.value.formState.copy(groupId = "", groupName = ""))
    }

    fun clearSubCategory() {
        updateForm(_uiState.value.formState.copy(subCategoryId = "", subCategoryName = ""))
    }

    fun clearUnit() {
        updateForm(_uiState.value.formState.copy(baseUnitId = "", baseUnitName = ""))
    }

    fun saveProduct(onSuccess: () -> Unit) {
        val formState = _uiState.value.formState
        if (!formState.isValid) return

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val product = formState.toProduct()

                val result = if (productId == null) {
                    val uid = UidGenerator.generateUid(Constants.PRODUCT_PREFIX)
                    productRepository.createProduct(product.copy(id = uid))
                } else {
                    productRepository.updateProduct(product.copy(id = productId))
                }

                if (result.isSuccess) {
                    syncService.markPendingPush(SyncEntity.PRODUCT)
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save product"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save product"
                )
            }
        }
    }

    private fun validateForm(formState: ProductFormState): ProductFormState {
        val nameError = when {
            formState.name.isBlank() -> "Product name is required"
            formState.name.length < 2 -> "Product name must be at least 2 characters"
            formState.name.length > 100 -> "Product name must be less than 100 characters"
            else -> null
        }

        val codeError = when {
            formState.code.isBlank() -> "Product code is required"
            formState.code.length < 2 -> "Product code must be at least 2 characters"
            formState.code.length > 50 -> "Product code must be less than 50 characters"
            !formState.code.matches(Regex("^[a-zA-Z0-9_-]+$")) -> "Product code can only contain letters, numbers, hyphens, and underscores"
            else -> null
        }

        val priceError = when {
            formState.sellingPrice < 0 -> "Selling price cannot be negative"
            formState.mrp < 0 -> "MRP cannot be negative"
            formState.dp < 0 -> "Dealer price cannot be negative"
            formState.mrp > 0 && formState.sellingPrice > formState.mrp -> "Selling price cannot be higher than MRP"
            else -> null
        }

        val stockError = when {
            formState.stockQuantity != null && formState.stockQuantity < 0 -> "Stock quantity cannot be negative"
            formState.lowStockAlert != null && formState.lowStockAlert < 0 -> "Low stock alert cannot be negative"
            formState.stockQuantity != null && formState.lowStockAlert != null &&
                formState.lowStockAlert > formState.stockQuantity -> "Low stock alert cannot be higher than current stock"
            else -> null
        }

        return formState.copy(
            nameError = nameError,
            codeError = codeError,
            priceError = priceError,
            stockError = stockError
        )
    }

    fun onTaxCodeSearchQueryChange(query: String) {
        _taxCodeSearchQuery.value = query
    }

    private suspend fun searchTaxCodes(query: String) {
        if (query.isBlank()) {
            _taxCodeSuggestions.value = emptyList()
            return
        }
        try {
            val results = taxCodeRepository.searchWorkspaceTaxCodes(query, limit = 10)
            _taxCodeSuggestions.value = results
        } catch (_: Exception) {}
    }

    fun selectTaxCode(taxCode: TaxCode) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(
                taxCode = taxCode.code,
                taxCodeDescription = "${taxCode.code} - ${taxCode.shortDescription}"
            )
        )
        _taxCodeSearchQuery.value = ""
        _taxCodeSuggestions.value = emptyList()
        viewModelScope.launch {
            taxCodeRepository.incrementUsageCount(taxCode.id)
        }
    }

    fun clearTaxCode() {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(
                taxCode = "",
                taxCodeDescription = null
            )
        )
    }
}

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val error: String? = null,
    val formState: ProductFormState = ProductFormState(),
    val categories: List<Group> = emptyList(),
    val brands: List<Group> = emptyList(),
    val groups: List<Group> = emptyList(),
    val subCategories: List<Group> = emptyList(),
    val units: List<UnitDomain> = emptyList(),
)
