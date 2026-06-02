package com.ampairs.product.ui.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FilePickerResult
import com.ampairs.file.api.FileRepository
import com.ampairs.file.picker.FilePicker
import com.ampairs.product.util.ProductLogger
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
    val pickerResult: FilePickerResult,
    val description: String = "",
    val isPrimary: Boolean = false,
) {
    val fileName: String get() = pickerResult.fileName
    val fileSize: Long get() = pickerResult.fileSize
    val contentType: String get() = pickerResult.contentType
}

data class ProductImageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val images: List<FileItem> = emptyList(),
    val selectedImage: FileItem? = null,
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
    private val fileRepository: FileRepository,
    private val filePicker: FilePicker,
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
        syncService.observeEntity(SyncEntity.FILE)
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        isRefreshing = state?.status is SyncStatus.Syncing,
                        syncError = state?.status is SyncStatus.Failed,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadImages() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                fileRepository.observeFiles(FileEntityType.PRODUCT, productId)
                    .catch { error ->
                        ProductLogger.e(TAG, "Failed to observe images", error)
                        _uiState.update {
                            it.copy(isLoading = false, error = "Failed to load images: ${error.message}")
                        }
                    }
                    .collect { images ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                images = images.sortedWith(compareByDescending<FileItem> { it.isPrimary }.thenBy { it.displayOrder }),
                                error = null,
                            )
                        }
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
                val pickerResult = filePicker.pickSingleImage()
                if (pickerResult != null) {
                    showUploadDialog(ProductImageUploadData(pickerResult = pickerResult))
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

                val result = fileRepository.saveLocally(
                    entityType = FileEntityType.PRODUCT,
                    entityUid = productId,
                    result = uploadData.pickerResult,
                    isPrimary = uploadData.isPrimary,
                )

                result.fold(
                    onSuccess = {
                        syncService.markPendingPush(SyncEntity.FILE)
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
        val image = _uiState.value.images.find { it.uid == uid }
        if (image != null) {
            _uiState.update { it.copy(selectedImage = image, showImageViewer = true, error = null) }
        }
    }

    fun hideImageViewer() {
        _uiState.update { it.copy(showImageViewer = false, selectedImage = null) }
    }

    fun setPrimaryImage(uid: String) {
        viewModelScope.launch {
            try {
                val result = fileRepository.setPrimaryFile(FileEntityType.PRODUCT, productId, uid)
                result.fold(
                    onSuccess = {
                        syncService.markPendingPush(SyncEntity.FILE)
                        val updated = _uiState.value.images.find { it.uid == uid }
                        if (updated != null) _uiState.update { it.copy(selectedImage = updated.copy(isPrimary = true)) }
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
                val result = fileRepository.deleteFile(uid)
                result.fold(
                    onSuccess = {
                        syncService.markPendingPush(SyncEntity.FILE)
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
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.FILE))
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
