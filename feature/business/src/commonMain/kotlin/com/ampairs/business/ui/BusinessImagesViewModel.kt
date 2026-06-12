package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessImage
import com.ampairs.business.domain.BusinessImageType
import com.ampairs.business.domain.UpdateBusinessImageRequest
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.file.picker.FilePicker
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch

/**
 * UI State for Business Images screen
 */
data class BusinessImagesUiState(
    val business: Business? = null,
    val logoUrl: String? = null,
    val logoThumbnailUrl: String? = null,
    val logoCacheBuster: Long = 0L, // Used to force image reload after upload
    val images: List<BusinessImage> = emptyList(),
    val isLoading: Boolean = false,
    val isUploadingLogo: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isDeletingLogo: Boolean = false,
    val isDeletingImage: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    // Selected image for picker
    val selectedLogoBytes: ByteArray? = null,
    val selectedLogoFileName: String? = null,
    val selectedLogoContentType: String? = null,
    val selectedImageBytes: ByteArray? = null,
    val selectedImageFileName: String? = null,
    val selectedImageContentType: String? = null
)

/**
 * ViewModel for Business Images management.
 * Handles logo and gallery image operations.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class BusinessImagesViewModel(
    private val businessApi: BusinessApi,
    private val filePicker: FilePicker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessImagesUiState())
    val uiState: StateFlow<BusinessImagesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Load business data and gallery images
     */
    @OptIn(ExperimentalTime::class)
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load business profile for logo info
            businessApi.getBusiness()
                .onSuccess { business ->
                    // Use logo URLs from business response if available, otherwise use API endpoint URLs
                    val hasLogo = !business.logoUrl.isNullOrBlank()
                    // Always use current timestamp as cache buster to ensure fresh image on screen load
                    val cacheBuster = Clock.System.now().toEpochMilliseconds()
                    _uiState.update { state ->
                        state.copy(
                            business = business,
                            logoUrl = if (hasLogo) ApiUrlBuilder.businessLogoUrl() else null,
                            logoThumbnailUrl = if (hasLogo) ApiUrlBuilder.businessLogoThumbnailUrl() else null,
                            logoCacheBuster = cacheBuster
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }

            // Load gallery images
            businessApi.getImages()
                .onSuccess { images ->
                    _uiState.update { it.copy(images = images, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // ==================== Logo Operations ====================

    @OptIn(ExperimentalTime::class)
    fun pickAndUploadLogo() {
        viewModelScope.launch {
            val result = filePicker.pickSingleImage() ?: return@launch
            _uiState.update { it.copy(
                selectedLogoBytes = result.imageData,
                selectedLogoFileName = result.fileName,
                selectedLogoContentType = result.contentType,
                isUploadingLogo = true,
                error = null,
            )}
            businessApi.uploadLogo(result.imageData, result.fileName, result.contentType)
                .onSuccess { business ->
                    val hasLogo = !business.logoUrl.isNullOrBlank()
                    val cacheBuster = Clock.System.now().toEpochMilliseconds()
                    _uiState.update { it.copy(
                        business = business,
                        logoUrl = if (hasLogo) ApiUrlBuilder.businessLogoUrl() else null,
                        logoThumbnailUrl = if (hasLogo) ApiUrlBuilder.businessLogoThumbnailUrl() else null,
                        logoCacheBuster = cacheBuster,
                        isUploadingLogo = false,
                        selectedLogoBytes = null,
                        selectedLogoFileName = null,
                        selectedLogoContentType = null,
                        successMessage = "Logo uploaded successfully",
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isUploadingLogo = false,
                        error = error.message ?: "Failed to upload logo",
                    )}
                }
        }
    }

    /**
     * Set selected logo from file picker
     */
    fun setSelectedLogo(bytes: ByteArray, fileName: String, contentType: String) {
        _uiState.update { it.copy(
            selectedLogoBytes = bytes,
            selectedLogoFileName = fileName,
            selectedLogoContentType = contentType
        )}
    }

    /**
     * Clear selected logo
     */
    fun clearSelectedLogo() {
        _uiState.update { it.copy(
            selectedLogoBytes = null,
            selectedLogoFileName = null,
            selectedLogoContentType = null
        )}
    }

    /**
     * Upload business logo
     */
    @OptIn(ExperimentalTime::class)
    fun uploadLogo() {
        val state = _uiState.value
        val bytes = state.selectedLogoBytes ?: return
        val fileName = state.selectedLogoFileName ?: "logo.jpg"
        val contentType = state.selectedLogoContentType ?: "image/jpeg"

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingLogo = true, error = null) }

            businessApi.uploadLogo(bytes, fileName, contentType)
                .onSuccess { business ->
                    val hasLogo = !business.logoUrl.isNullOrBlank()
                    val cacheBuster = Clock.System.now().toEpochMilliseconds()
                    _uiState.update { it.copy(
                        business = business,
                        logoUrl = if (hasLogo) ApiUrlBuilder.businessLogoUrl() else null,
                        logoThumbnailUrl = if (hasLogo) ApiUrlBuilder.businessLogoThumbnailUrl() else null,
                        logoCacheBuster = cacheBuster,
                        isUploadingLogo = false,
                        selectedLogoBytes = null,
                        selectedLogoFileName = null,
                        selectedLogoContentType = null,
                        successMessage = "Logo uploaded successfully"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isUploadingLogo = false,
                        error = error.message ?: "Failed to upload logo"
                    )}
                }
        }
    }

    /**
     * Delete business logo
     */
    fun deleteLogo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingLogo = true, error = null) }

            businessApi.deleteLogo()
                .onSuccess { business ->
                    _uiState.update { it.copy(
                        business = business,
                        logoUrl = null,
                        logoThumbnailUrl = null,
                        isDeletingLogo = false,
                        successMessage = "Logo deleted successfully"
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isDeletingLogo = false,
                        error = error.message ?: "Failed to delete logo"
                    )}
                }
        }
    }

    // ==================== Gallery Image Operations ====================

    fun pickAndUploadImage(
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null,
    ) {
        viewModelScope.launch {
            if (_uiState.value.images.size >= 20) {
                _uiState.update { it.copy(error = "Maximum 20 images allowed.") }
                return@launch
            }
            val result = filePicker.pickSingleImage() ?: return@launch
            _uiState.update { it.copy(
                selectedImageBytes = result.imageData,
                selectedImageFileName = result.fileName,
                selectedImageContentType = result.contentType,
                isUploadingImage = true,
                error = null,
            )}
            businessApi.uploadImage(
                imageData = result.imageData,
                fileName = result.fileName,
                contentType = result.contentType,
                imageType = imageType,
                title = title,
                description = description,
            )
                .onSuccess { newImage ->
                    _uiState.update { state ->
                        state.copy(
                            images = state.images + newImage,
                            isUploadingImage = false,
                            selectedImageBytes = null,
                            selectedImageFileName = null,
                            selectedImageContentType = null,
                            successMessage = "Image uploaded successfully",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isUploadingImage = false,
                        error = error.message ?: "Failed to upload image",
                    )}
                }
        }
    }

    /**
     * Set selected image from file picker
     */
    fun setSelectedImage(bytes: ByteArray, fileName: String, contentType: String) {
        _uiState.update { it.copy(
            selectedImageBytes = bytes,
            selectedImageFileName = fileName,
            selectedImageContentType = contentType
        )}
    }

    /**
     * Clear selected image
     */
    fun clearSelectedImage() {
        _uiState.update { it.copy(
            selectedImageBytes = null,
            selectedImageFileName = null,
            selectedImageContentType = null
        )}
    }

    /**
     * Upload gallery image
     */
    fun uploadImage(
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null
    ) {
        val state = _uiState.value
        val bytes = state.selectedImageBytes ?: return
        val fileName = state.selectedImageFileName ?: "image.jpg"
        val contentType = state.selectedImageContentType ?: "image/jpeg"

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, error = null) }

            businessApi.uploadImage(
                imageData = bytes,
                fileName = fileName,
                contentType = contentType,
                imageType = imageType,
                title = title,
                description = description
            )
                .onSuccess { newImage ->
                    _uiState.update { state ->
                        state.copy(
                            images = state.images + newImage,
                            isUploadingImage = false,
                            selectedImageBytes = null,
                            selectedImageFileName = null,
                            selectedImageContentType = null,
                            successMessage = "Image uploaded successfully"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isUploadingImage = false,
                        error = error.message ?: "Failed to upload image"
                    )}
                }
        }
    }

    /**
     * Delete gallery image
     */
    fun deleteImage(imageUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingImage = true, error = null) }

            businessApi.deleteImage(imageUid)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            images = state.images.filter { it.uid != imageUid },
                            isDeletingImage = false,
                            successMessage = "Image deleted successfully"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isDeletingImage = false,
                        error = error.message ?: "Failed to delete image"
                    )}
                }
        }
    }

    /**
     * Set image as primary
     */
    fun setImageAsPrimary(imageUid: String) {
        viewModelScope.launch {
            businessApi.setImageAsPrimary(imageUid)
                .onSuccess { updatedImage ->
                    _uiState.update { state ->
                        state.copy(
                            images = state.images.map { img ->
                                when {
                                    img.uid == imageUid -> img.copy(isPrimary = true)
                                    img.isPrimary -> img.copy(isPrimary = false)
                                    else -> img
                                }
                            },
                            successMessage = "Primary image updated"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Update image metadata
     */
    fun updateImageMetadata(imageUid: String, title: String?, description: String?, imageType: String?) {
        viewModelScope.launch {
            businessApi.updateImage(
                imageUid,
                UpdateBusinessImageRequest(title = title, description = description, imageType = imageType)
            )
                .onSuccess { updatedImage ->
                    _uiState.update { state ->
                        state.copy(
                            images = state.images.map { if (it.uid == imageUid) updatedImage else it },
                            successMessage = "Image updated successfully"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Reorder images
     */
    fun reorderImages(imageUids: List<String>) {
        viewModelScope.launch {
            businessApi.reorderImages(imageUids)
                .onSuccess {
                    // Reorder local list to match
                    _uiState.update { state ->
                        val reorderedImages = imageUids.mapNotNull { uid ->
                            state.images.find { it.uid == uid }
                        }
                        state.copy(images = reorderedImages)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
