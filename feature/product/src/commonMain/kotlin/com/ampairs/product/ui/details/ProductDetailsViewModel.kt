package com.ampairs.product.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileRepository
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import com.ampairs.product.domain.Product
import com.ampairs.unit.data.repository.UnitLookup
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailsUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val categoryName: String = "",
    val brandName: String = "",
    val groupName: String = "",
    val subCategoryName: String = "",
    val baseUnitName: String = "",
    val primaryImageUrl: String? = null,
    /** Whether this product is listed on the workspace storefront (server state). */
    val isEcomListed: Boolean = false,
    val isEcomToggling: Boolean = false,
    val ecomError: String? = null,
)

@AssistedInject
class ProductDetailsViewModel(
    @Assisted private val productId: String,
    private val productRepository: ProductRepository,
    private val productApi: com.ampairs.product.data.api.ProductApi,
    private val fileRepository: FileRepository,
    private val categoryDao: CategoryDao,
    private val brandDao: BrandDao,
    private val groupDao: GroupDao,
    private val subCategoryDao: SubCategoryDao,
    private val unitLookup: UnitLookup,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String): ProductDetailsViewModel
    }

    private val _uiState = MutableStateFlow(ProductDetailsUiState())
    val uiState: StateFlow<ProductDetailsUiState> = _uiState.asStateFlow()

    init {
        observeProduct()
        observePrimaryImage()
        loadEcomListing()
    }

    /** Fetch current storefront-listing state (online; best-effort — silent if it fails). */
    private fun loadEcomListing() {
        viewModelScope.launch {
            productApi.getProduct(productId)
                .onSuccess { p -> _uiState.update { it.copy(isEcomListed = p.isEcomListed) } }
        }
    }

    /**
     * Toggle storefront listing for this product (online action). On success the storefront catalog
     * is updated server-side asynchronously.
     */
    fun setEcomListing(listed: Boolean) {
        if (_uiState.value.isEcomToggling) return
        _uiState.update { it.copy(isEcomToggling = true, ecomError = null) }
        viewModelScope.launch {
            productApi.setEcomListing(productId, listed)
                .onSuccess { p ->
                    _uiState.update { it.copy(isEcomListed = p.isEcomListed, isEcomToggling = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isEcomToggling = false, ecomError = e.message ?: "Failed to update storefront listing") }
                }
        }
    }

    private fun observeProduct() {
        _uiState.update { it.copy(isLoading = true) }
        productRepository.observeProduct(productId)
            .onEach { product ->
                _uiState.update { it.copy(product = product, isLoading = false, error = null) }
                if (product != null) resolveNames(product)
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load product") }
            }
            .launchIn(viewModelScope)
    }

    private fun observePrimaryImage() {
        fileRepository.observePrimaryFile(FileEntityType.PRODUCT, productId)
            .onEach { file ->
                val url = when {
                    file == null -> null
                    file.uploadStatus == FileUploadStatus.PENDING || file.uploadStatus == FileUploadStatus.UPLOADING ->
                        file.localPath?.let { "file://$it" }
                    file.imageUrl.isNotBlank() -> file.imageUrl
                    else -> null
                }
                _uiState.update { it.copy(primaryImageUrl = url) }
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    private fun resolveNames(product: Product) {
        viewModelScope.launch {
            try {
                val categoryName = if (product.categoryId.isNotEmpty()) {
                    product.categoryName?.takeIf { it.isNotBlank() }
                        ?: categoryDao.getActiveCategories().firstOrNull { it.id == product.categoryId }?.name ?: ""
                } else ""

                val brandName = if (product.brandId.isNotEmpty()) {
                    product.brandName?.takeIf { it.isNotBlank() }
                        ?: brandDao.getActiveBrands().firstOrNull { it.id == product.brandId }?.name ?: ""
                } else ""

                val groupName = if (product.groupId.isNotEmpty()) {
                    groupDao.getActiveGroups().firstOrNull { it.id == product.groupId }?.name ?: ""
                } else ""

                val subCategoryName = if (product.subCategoryId.isNotEmpty()) {
                    subCategoryDao.getActiveSubCategories().firstOrNull { it.id == product.subCategoryId }?.name ?: ""
                } else ""

                val baseUnitName = product.baseUnitId
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { unitLookup.getUnitById(it)?.name } ?: ""

                _uiState.update {
                    it.copy(
                        categoryName = categoryName,
                        brandName = brandName,
                        groupName = groupName,
                        subCategoryName = subCategoryName,
                        baseUnitName = baseUnitName,
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun deleteProduct(onSuccess: () -> Unit) {
        val currentProduct = _uiState.value.product ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            try {
                val result = productRepository.deleteProduct(currentProduct.id)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(isDeleting = false, error = result.exceptionOrNull()?.message ?: "Delete failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, error = e.message ?: "Delete failed") }
            }
        }
    }
}
