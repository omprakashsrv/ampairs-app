package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessImage
import com.ampairs.business.domain.BusinessImageType
import com.ampairs.business.domain.UpdateBusinessImageRequest
import com.ampairs.common.ApiUrlBuilder
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
class BusinessImagesViewModel(
    private val businessApi: BusinessApi
) : ViewModel() {

    companion object {
        private val SUPPORTED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
        private const val MAX_LOGO_SIZE = 10 * 1024 * 1024L // 10MB for logo
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L // 10MB for gallery images
    }

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

    /**
     * Refresh data (for pull-to-refresh)
     */
    fun refresh() {
        loadData()
    }

    // ==================== Logo Operations ====================

    /**
     * Pick and upload logo in one action using FileKit
     */
    fun pickAndUploadLogo() {
        viewModelScope.launch {
            try {
                val file = FileKit.openFilePicker(
                    type = FileKitType.Image
                )

                if (file == null) {
                    return@launch
                }

                val fileName = file.name
                val fileSize = file.size()

                // Validate file extension
                val extension = fileName.substringAfterLast(".", "").lowercase()
                if (extension !in SUPPORTED_IMAGE_EXTENSIONS) {
                    _uiState.update { it.copy(error = "Unsupported file type. Please select JPG, PNG, or WebP.") }
                    return@launch
                }

                // Validate file size
                if (fileSize > MAX_LOGO_SIZE) {
                    _uiState.update { it.copy(error = "Image too large. Maximum size is 10MB.") }
                    return@launch
                }

                val bytes = file.readBytes()
                val contentType = when (extension) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }

                // Set and upload immediately
                _uiState.update { it.copy(
                    selectedLogoBytes = bytes,
                    selectedLogoFileName = fileName,
                    selectedLogoContentType = contentType,
                    isUploadingLogo = true,
                    error = null
                )}

                // Upload
                businessApi.uploadLogo(bytes, fileName, contentType)
                    .onSuccess { business ->
                        // After upload, logo URLs should be available
                        val hasLogo = !business.logoUrl.isNullOrBlank()
                        @OptIn(ExperimentalTime::class)
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

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isUploadingLogo = false,
                    error = e.message ?: "Failed to pick image"
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
                    @OptIn(ExperimentalTime::class)
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

    /**
     * Pick and upload gallery image in one action using FileKit
     */
    fun pickAndUploadImage(
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                val file = FileKit.openFilePicker(
                    type = FileKitType.Image
                )

                if (file == null) {
                    return@launch
                }

                val fileName = file.name
                val fileSize = file.size()

                // Validate file extension
                val extension = fileName.substringAfterLast(".", "").lowercase()
                if (extension !in SUPPORTED_IMAGE_EXTENSIONS) {
                    _uiState.update { it.copy(error = "Unsupported file type. Please select JPG, PNG, or WebP.") }
                    return@launch
                }

                // Validate file size
                if (fileSize > MAX_IMAGE_SIZE) {
                    _uiState.update { it.copy(error = "Image too large. Maximum size is 10MB.") }
                    return@launch
                }

                // Check max images limit
                if (_uiState.value.images.size >= 20) {
                    _uiState.update { it.copy(error = "Maximum 20 images allowed.") }
                    return@launch
                }

                val bytes = file.readBytes()
                val contentType = when (extension) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg"
                }

                // Set uploading state
                _uiState.update { it.copy(
                    selectedImageBytes = bytes,
                    selectedImageFileName = fileName,
                    selectedImageContentType = contentType,
                    isUploadingImage = true,
                    error = null
                )}

                // Upload
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

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isUploadingImage = false,
                    error = e.message ?: "Failed to pick image"
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
