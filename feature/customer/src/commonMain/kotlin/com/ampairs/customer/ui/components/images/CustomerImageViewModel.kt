package com.ampairs.customer.ui.components.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.data.repository.CustomerImageRepository
import com.ampairs.customer.data.repository.ImageFilePicker
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageListItem
import com.ampairs.customer.domain.CustomerImageUpdateRequest
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.common.di.AppScope
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerImageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val images: List<CustomerImageListItem> = emptyList(),
    val selectedImage: CustomerImage? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
    val uploadProgress: Float? = null,
    val showImageViewer: Boolean = false,
    val showUploadDialog: Boolean = false,
    val uploadData: ImageUploadData? = null,
    val syncError: Boolean = false,
)

@AssistedInject
class CustomerImageViewModel(
    @Assisted private val customerId: String,
    private val repository: CustomerImageRepository,
    private val imagePicker: ImageFilePicker,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(customerId: String): CustomerImageViewModel
    }

    private val _uiState = MutableStateFlow(CustomerImageUiState())
    val uiState: StateFlow<CustomerImageUiState> = _uiState.asStateFlow()

    init {
        loadImages()
        syncService.observeEntity(SyncEntity.CUSTOMER_IMAGE)
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        isRefreshing = state?.status is SyncStatus.Syncing,
                        syncError = state?.status is SyncStatus.Failed,
                    )
                }
            }
            .launchIn(viewModelScope)
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CUSTOMER_IMAGE))
    }

    fun loadImages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                repository.observeCustomerImages(customerId)
                    .catch { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to observe images", error)
                        _uiState.update { it.copy(isLoading = false, error = "Failed to load images: ${error.message}") }
                    }
                    .collect { images ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                images = images.sortedWith(
                                    compareByDescending<CustomerImageListItem> { it.isPrimary }.thenBy { it.sortOrder }
                                ),
                                error = null,
                            )
                        }
                    }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Failed to load images", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load images: ${e.message}") }
            }
        }
    }

    fun showUploadDialog(uploadData: ImageUploadData) {
        _uiState.update { it.copy(showUploadDialog = true, uploadData = uploadData, error = null) }
    }

    fun hideUploadDialog() {
        _uiState.update { it.copy(showUploadDialog = false, uploadData = null, isUploading = false, uploadProgress = null) }
    }

    fun pickSingleImage() {
        viewModelScope.launch {
            try {
                val pickerResult = imagePicker.pickImage()
                if (pickerResult != null) {
                    showUploadDialog(
                        ImageUploadData(
                            fileName = pickerResult.fileName,
                            fileSize = pickerResult.fileSize,
                            contentType = pickerResult.contentType,
                            imageData = pickerResult.imageData,
                            description = "",
                            isPrimary = false,
                        )
                    )
                }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Error picking image", e)
                _uiState.update { it.copy(error = "Failed to pick image: ${e.message}") }
            }
        }
    }

    fun pickMultipleImages() {
        viewModelScope.launch {
            try {
                val maxAllowed = 10 - uiState.value.images.size
                if (maxAllowed <= 0) {
                    _uiState.update { it.copy(error = "Maximum of 10 images allowed per customer") }
                    return@launch
                }
                val pickerResults = imagePicker.pickMultipleImages(maxAllowed)
                if (pickerResults.isNotEmpty()) {
                    for (result in pickerResults) {
                        uploadImage(
                            ImageUploadData(
                                fileName = result.fileName,
                                fileSize = result.fileSize,
                                contentType = result.contentType,
                                imageData = result.imageData,
                                description = "",
                                isPrimary = false,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Error picking multiple images", e)
                _uiState.update { it.copy(error = "Failed to pick images: ${e.message}") }
            }
        }
    }

    fun uploadImage(uploadData: ImageUploadData) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isUploading = true, error = null) }

                val result = repository.saveImageLocally(
                    customerId = customerId,
                    fileName = uploadData.fileName,
                    contentType = uploadData.contentType,
                    fileSize = uploadData.fileSize,
                    imageData = uploadData.imageData,
                    description = uploadData.description.takeIf { it.isNotBlank() },
                    isPrimary = uploadData.isPrimary,
                )

                result.fold(
                    onSuccess = {
                        syncService.markPendingPush(SyncEntity.CUSTOMER_IMAGE)
                        _uiState.update {
                            it.copy(isUploading = false, uploadProgress = null, showUploadDialog = false, uploadData = null, error = null)
                        }
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to save image locally", error)
                        _uiState.update { it.copy(isUploading = false, uploadProgress = null, error = "Failed to save image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Upload error", e)
                _uiState.update { it.copy(isUploading = false, uploadProgress = null, error = "Failed to save image: ${e.message}") }
            }
        }
    }

    fun showImageViewer(imageId: String) {
        viewModelScope.launch {
            try {
                val image = repository.getCustomerImage(imageId)
                _uiState.update { it.copy(selectedImage = image, showImageViewer = true, error = null) }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Failed to load image details", e)
                _uiState.update { it.copy(error = "Failed to load image: ${e.message}") }
            }
        }
    }

    fun hideImageViewer() {
        _uiState.update { it.copy(showImageViewer = false, selectedImage = null) }
    }

    fun setPrimaryImage(imageId: String) {
        viewModelScope.launch {
            try {
                val result = repository.setPrimaryImage(imageId)
                result.fold(
                    onSuccess = { updatedImage -> _uiState.update { it.copy(selectedImage = updatedImage) } },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to set primary image", error)
                        _uiState.update { it.copy(error = "Failed to set primary image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Set primary image error", e)
                _uiState.update { it.copy(error = "Failed to set primary image: ${e.message}") }
            }
        }
    }

    fun updateImage(imageId: String, description: String?, isPrimary: Boolean?) {
        viewModelScope.launch {
            try {
                val result = repository.updateImage(imageId, CustomerImageUpdateRequest(description = description, isPrimary = isPrimary))
                result.fold(
                    onSuccess = { CustomerLogger.i("CustomerImageViewModel", "Image updated: $imageId") },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to update image", error)
                        _uiState.update { it.copy(error = "Failed to update image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Update image error", e)
                _uiState.update { it.copy(error = "Failed to update image: ${e.message}") }
            }
        }
    }

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            try {
                val result = repository.deleteImage(imageId)
                result.fold(
                    onSuccess = {
                        CustomerLogger.i("CustomerImageViewModel", "Image deleted: $imageId")
                        if (_uiState.value.selectedImage?.uid == imageId) hideImageViewer()
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to delete image", error)
                        _uiState.update { it.copy(error = "Failed to delete image: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Delete image error", e)
                _uiState.update { it.copy(error = "Failed to delete image: ${e.message}") }
            }
        }
    }

    fun syncImages() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER_IMAGE))
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
