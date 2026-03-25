package com.ampairs.customer.ui.components.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.repository.CustomerImageRepository
import com.ampairs.customer.data.repository.ImageFilePicker
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageListItem
import com.ampairs.customer.domain.CustomerImageUpdateRequest
import com.ampairs.customer.util.CustomerConstants.CUSTOMER_IMAGE_UID_PREFIX
import com.ampairs.customer.util.CustomerLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerImageUiState(
    val isLoading: Boolean = false,
    val images: List<CustomerImageListItem> = emptyList(),
    val selectedImage: CustomerImage? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
    val uploadProgress: Float? = null,
    val showImageViewer: Boolean = false,
    val showUploadDialog: Boolean = false,
    val uploadData: ImageUploadData? = null,
    val syncError: Boolean = false // Track if error occurred during sync
)

class CustomerImageViewModel(
    private val customerId: String,
    private val repository: CustomerImageRepository,
    private val imagePicker: ImageFilePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerImageUiState())
    val uiState: StateFlow<CustomerImageUiState> = _uiState.asStateFlow()

    init {
        loadImages()
        syncImages() // Auto-sync on screen entry
    }

    fun loadImages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                repository.observeCustomerImages(customerId)
                    .catch { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to observe images", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to load images: ${error.message}"
                            )
                        }
                    }
                    .collect { images ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                images = images.sortedWith(
                                    compareByDescending<CustomerImageListItem> { it.isPrimary }
                                        .thenBy { it.sortOrder }
                                ),
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Failed to load images", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load images: ${e.message}"
                    )
                }
            }
        }
    }

    fun showUploadDialog(uploadData: ImageUploadData) {
        _uiState.update {
            it.copy(
                showUploadDialog = true,
                uploadData = uploadData,
                error = null
            )
        }
    }

    fun hideUploadDialog() {
        _uiState.update {
            it.copy(
                showUploadDialog = false,
                uploadData = null,
                isUploading = false,
                uploadProgress = null
            )
        }
    }

    fun pickSingleImage() {
        viewModelScope.launch {
            try {
                CustomerLogger.d("CustomerImageViewModel", "Starting single image picker for customer: $customerId")

                val pickerResult = imagePicker.pickImage()
                if (pickerResult != null) {
                    CustomerLogger.d("CustomerImageViewModel", "Image selected: ${pickerResult.fileName}")

                    // Convert picker result to ImageUploadData and show upload dialog
                    val uploadData = ImageUploadData(
                        fileName = pickerResult.fileName,
                        fileSize = pickerResult.fileSize,
                        contentType = pickerResult.contentType,
                        imageData = pickerResult.imageData,
                        description = "",
                        isPrimary = false
                    )

                    showUploadDialog(uploadData)
                } else {
                    CustomerLogger.d("CustomerImageViewModel", "Image selection cancelled")
                }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Error picking image", e)
                _uiState.update {
                    it.copy(error = "Failed to pick image: ${e.message}")
                }
            }
        }
    }

    fun pickMultipleImages() {
        viewModelScope.launch {
            try {
                CustomerLogger.d("CustomerImageViewModel", "Starting multiple image picker for customer: $customerId")

                val maxImagesAllowed = 10 - uiState.value.images.size
                if (maxImagesAllowed <= 0) {
                    _uiState.update {
                        it.copy(error = "Maximum of 10 images allowed per customer")
                    }
                    return@launch
                }

                val pickerResults = imagePicker.pickMultipleImages(maxImagesAllowed)
                if (pickerResults.isNotEmpty()) {
                    CustomerLogger.d("CustomerImageViewModel", "${pickerResults.size} images selected")

                    // Upload all selected images directly without showing upload dialog
                    for (result in pickerResults) {
                        val uploadData = ImageUploadData(
                            fileName = result.fileName,
                            fileSize = result.fileSize,
                            contentType = result.contentType,
                            imageData = result.imageData,
                            description = "",
                            isPrimary = false
                        )
                        uploadImage(uploadData)
                    }
                } else {
                    CustomerLogger.d("CustomerImageViewModel", "Multiple image selection cancelled")
                }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Error picking multiple images", e)
                _uiState.update {
                    it.copy(error = "Failed to pick images: ${e.message}")
                }
            }
        }
    }

    fun uploadImage(uploadData: ImageUploadData) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isUploading = true,
                        error = null,
                        uploadProgress = 0f
                    )
                }

                // Generate UID for the image
                val imageUid = UidGenerator.generateUid(CUSTOMER_IMAGE_UID_PREFIX)

                val result = repository.uploadImage(
                    customerId = customerId,
                    fileName = uploadData.fileName,
                    contentType = uploadData.contentType,
                    fileSize = uploadData.fileSize,
                    imageData = uploadData.imageData,
                    description = uploadData.description.takeIf { it.isNotBlank() },
                    isPrimary = uploadData.isPrimary
                )

                result.fold(
                    onSuccess = { customerImage ->
                        CustomerLogger.i("CustomerImageViewModel", "Image uploaded successfully: ${customerImage.uid}")
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = null,
                                showUploadDialog = false,
                                uploadData = null,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to upload image", error)
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = null,
                                error = "Upload failed: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Upload error", e)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadProgress = null,
                        error = "Upload failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun showImageViewer(imageId: String) {
        viewModelScope.launch {
            try {
                val image = repository.getCustomerImage(imageId)
                _uiState.update {
                    it.copy(
                        selectedImage = image,
                        showImageViewer = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Failed to load image details", e)
                _uiState.update {
                    it.copy(error = "Failed to load image: ${e.message}")
                }
            }
        }
    }

    fun hideImageViewer() {
        _uiState.update {
            it.copy(
                showImageViewer = false,
                selectedImage = null
            )
        }
    }

    fun setPrimaryImage(imageId: String) {
        viewModelScope.launch {
            try {
                val result = repository.setPrimaryImage(imageId)
                result.fold(
                    onSuccess = { updatedImage ->
                        CustomerLogger.i("CustomerImageViewModel", "Primary image updated: ${updatedImage.uid}")
                        // Update selectedImage in viewer to reflect the change immediately
                        _uiState.update {
                            it.copy(selectedImage = updatedImage)
                        }
                        // Images will be automatically updated via the Flow
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to set primary image", error)
                        _uiState.update {
                            it.copy(error = "Failed to set primary image: ${error.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Set primary image error", e)
                _uiState.update {
                    it.copy(error = "Failed to set primary image: ${e.message}")
                }
            }
        }
    }

    fun updateImage(imageId: String, description: String?, isPrimary: Boolean?) {
        viewModelScope.launch {
            try {
                val updateRequest = CustomerImageUpdateRequest(
                    description = description,
                    isPrimary = isPrimary
                )

                val result = repository.updateImage(imageId, updateRequest)
                result.fold(
                    onSuccess = { updatedImage ->
                        CustomerLogger.i("CustomerImageViewModel", "Image updated: ${updatedImage.uid}")
                        // Images will be automatically updated via the Flow
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to update image", error)
                        _uiState.update {
                            it.copy(error = "Failed to update image: ${error.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Update image error", e)
                _uiState.update {
                    it.copy(error = "Failed to update image: ${e.message}")
                }
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
                        // Hide image viewer if the deleted image was being viewed
                        if (_uiState.value.selectedImage?.uid == imageId) {
                            hideImageViewer()
                        }
                        // Images will be automatically updated via the Flow
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to delete image", error)
                        _uiState.update {
                            it.copy(error = "Failed to delete image: ${error.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Delete image error", e)
                _uiState.update {
                    it.copy(error = "Failed to delete image: ${e.message}")
                }
            }
        }
    }

    fun syncImages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, syncError = false) }

                val result = repository.syncCustomerImages(customerId)
                result.fold(
                    onSuccess = { syncedCount ->
                        CustomerLogger.i("CustomerImageViewModel", "Images synced successfully: $syncedCount images")
                        // Images will be automatically updated via the Flow
                        _uiState.update { it.copy(isLoading = false, syncError = false) }
                    },
                    onFailure = { error ->
                        CustomerLogger.e("CustomerImageViewModel", "Failed to sync images", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Sync failed: ${error.message}",
                                syncError = true
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                CustomerLogger.e("CustomerImageViewModel", "Sync error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Sync failed: ${e.message}",
                        syncError = true
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retryLastOperation() {
        // This could be enhanced to remember and retry the last failed operation
        loadImages()
    }

    // Stats and utility methods
    fun getImageCount(): Int = _uiState.value.images.size

    fun getPrimaryImage(): CustomerImageListItem? = _uiState.value.images.find { it.isPrimary }

    fun hasImages(): Boolean = _uiState.value.images.isNotEmpty()

    fun canAddMoreImages(maxImages: Int = 10): Boolean = _uiState.value.images.size < maxImages
}