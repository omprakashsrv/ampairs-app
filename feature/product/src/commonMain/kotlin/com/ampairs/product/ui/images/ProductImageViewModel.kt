package com.ampairs.product.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.product.data.repository.ProductImageFilePicker
import com.ampairs.product.data.repository.ProductImageRepository
import com.ampairs.product.util.ProductLogger
import com.ampairs.product.domain.ProductUploadImage
import com.ampairs.product.domain.ProductUploadImageListItem
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
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

data class ProductImageUploadData(
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val imageData: ByteArray,
    val description: String = "",
    val isPrimary: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ProductImageUploadData
        if (fileName != other.fileName) return false
        if (fileSize != other.fileSize) return false
        if (contentType != other.contentType) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (description != other.description) return false
        if (isPrimary != other.isPrimary) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + isPrimary.hashCode()
        return result
    }
}

data class ProductImageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val images: List<ProductUploadImageListItem> = emptyList(),
    val selectedImage: ProductUploadImage? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
    val showImageViewer: Boolean = false,
    val showUploadDialog: Boolean = false,
    val uploadData: ProductImageUploadData? = null,
    val syncError: Boolean = false,
)

@AssistedInject
class ProductImageViewModel(
    @Assisted private val productId: String,
    private val repository: ProductImageRepository,
    private val imagePicker: ProductImageFilePicker,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(productId: String): ProductImageViewModel
    }

    private val _uiState = MutableStateFlow(ProductImageUiState())
    val uiState: StateFlow<ProductImageUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "ProductImageViewModel"
    }

    init {
        loadImages()
        syncService.observeEntity(SyncEntity.PRODUCT_IMAGE)
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        isRefreshing = state?.status is SyncStatus.Syncing,
                        syncError = state?.status is SyncStatus.Failed,
                    )
                }
            }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.PRODUCT_IMAGE))
    }

    fun loadImages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                repository.observeProductImages(productId)
                    .catch { error ->
                        ProductLogger.e(TAG, "Failed to observe images", error)
                        _uiState.update {
                            it.copy(isLoading = false, error = "Failed to load images: ${error.message}")
                        }
                    }
                    .collect { images ->
                        _uiState.update { it.copy(isLoading = false, images = images, error = null) }
                    }
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Failed to load images", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load images: ${e.message}") }
            }
        }
    }

    fun showUploadDialog(uploadData: ProductImageUploadData) {
        _uiState.update { it.copy(showUploadDialog = true, uploadData = uploadData, error = null) }
    }

    fun hideUploadDialog() {
        _uiState.update { it.copy(showUploadDialog = false, uploadData = null, isUploading = false) }
    }

    fun pickSingleImage() {
        viewModelScope.launch {
            try {
                val pickerResult = imagePicker.pickImage()
                if (pickerResult != null) {
                    showUploadDialog(
                        ProductImageUploadData(
                            fileName = pickerResult.fileName,
                            fileSize = pickerResult.fileSize,
                            contentType = pickerResult.contentType,
                            imageData = pickerResult.imageData,
                        )
                    )
                }
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Error picking image", e)
                _uiState.update { it.copy(error = "Failed to pick image: ${e.message}") }
            }
        }
    }

    fun uploadImage(uploadData: ProductImageUploadData) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUploading = true, error = null) }

                val result = repository.saveImageLocally(
                    productId = productId,
                    fileName = uploadData.fileName,
                    contentType = uploadData.contentType,
                    fileSize = uploadData.fileSize,
                    imageData = uploadData.imageData,
                    description = uploadData.description.takeIf { it.isNotBlank() },
                    isPrimary = uploadData.isPrimary,
                )

                result.fold(
                    onSuccess = {
                        syncService.markPendingPush(SyncEntity.PRODUCT_IMAGE)
                        _uiState.update {
                            it.copy(isUploading = false, showUploadDialog = false, uploadData = null, error = null)
                        }
                    },
                    onFailure = { error ->
                        ProductLogger.e(TAG, "Failed to save image locally", error)
                        _uiState.update { it.copy(isUploading = false, error = "Failed to save image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Upload error", e)
                _uiState.update { it.copy(isUploading = false, error = "Failed to save image: ${e.message}") }
            }
        }
    }

    fun showImageViewer(uid: String) {
        viewModelScope.launch {
            try {
                val image = repository.getProductImage(uid)
                _uiState.update { it.copy(selectedImage = image, showImageViewer = true, error = null) }
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Failed to load image details", e)
                _uiState.update { it.copy(error = "Failed to load image: ${e.message}") }
            }
        }
    }

    fun hideImageViewer() {
        _uiState.update { it.copy(showImageViewer = false, selectedImage = null) }
    }

    fun setPrimaryImage(uid: String) {
        viewModelScope.launch {
            try {
                val result = repository.setPrimaryImage(uid)
                result.fold(
                    onSuccess = { updatedImage ->
                        _uiState.update { it.copy(selectedImage = updatedImage) }
                    },
                    onFailure = { error ->
                        ProductLogger.e(TAG, "Failed to set primary image", error)
                        _uiState.update { it.copy(error = "Failed to set primary image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Set primary image error", e)
                _uiState.update { it.copy(error = "Failed to set primary image: ${e.message}") }
            }
        }
    }

    fun deleteImage(uid: String) {
        viewModelScope.launch {
            try {
                val result = repository.deleteImage(uid)
                result.fold(
                    onSuccess = {
                        if (_uiState.value.selectedImage?.uid == uid) hideImageViewer()
                    },
                    onFailure = { error ->
                        ProductLogger.e(TAG, "Failed to delete image", error)
                        _uiState.update { it.copy(error = "Failed to delete image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                ProductLogger.e(TAG, "Delete image error", e)
                _uiState.update { it.copy(error = "Failed to delete image: ${e.message}") }
            }
        }
    }

    fun syncImages() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.PRODUCT_IMAGE))
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
