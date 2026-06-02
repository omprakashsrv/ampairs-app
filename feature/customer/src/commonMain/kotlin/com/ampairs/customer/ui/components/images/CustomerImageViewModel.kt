package com.ampairs.customer.ui.components.images

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FilePickerResult
import com.ampairs.file.api.FileRepository
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.file.picker.FilePicker
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

data class CustomerImageUploadData(
    val pickerResult: FilePickerResult,
    val description: String = "",
    val isPrimary: Boolean = false,
) {
    val fileName: String get() = pickerResult.fileName
    val fileSize: Long get() = pickerResult.fileSize
    val contentType: String get() = pickerResult.contentType
}

data class CustomerImageUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val images: List<FileItem> = emptyList(),
    val selectedImage: FileItem? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
    val showImageViewer: Boolean = false,
    val showUploadDialog: Boolean = false,
    val uploadData: CustomerImageUploadData? = null,
    val syncError: Boolean = false,
)

@AssistedInject
class CustomerImageViewModel(
    @Assisted private val customerId: String,
    private val fileRepository: FileRepository,
    private val filePicker: FilePicker,
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

    companion object {
        private const val TAG = "CustomerImageViewModel"
    }

    init {
        loadImages()
        viewModelScope.launch {
            fileRepository.pullFromServer(FileEntityType.CUSTOMER, customerId)
                .onFailure { CustomerLogger.w(TAG, "Initial image pull failed", it) }
        }
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            fileRepository.observeFiles(FileEntityType.CUSTOMER, customerId)
                .catch { error ->
                    CustomerLogger.e(TAG, "Failed to observe images", error)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load images: ${error.message}") }
                }
                .collect { images ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            images = images.sortedWith(
                                compareByDescending<FileItem> { it.isPrimary }.thenBy { it.displayOrder }
                            ),
                            error = null,
                        )
                    }
                }
        }
    }

    fun showUploadDialog(uploadData: CustomerImageUploadData) {
        _uiState.update { it.copy(showUploadDialog = true, uploadData = uploadData, error = null) }
    }

    fun hideUploadDialog() {
        _uiState.update { it.copy(showUploadDialog = false, uploadData = null, isUploading = false) }
    }

    fun showImageViewer(uid: String) {
        val image = _uiState.value.images.find { it.uid == uid }
        _uiState.update { it.copy(selectedImage = image, showImageViewer = true, error = null) }
    }

    fun hideImageViewer() {
        _uiState.update { it.copy(showImageViewer = false, selectedImage = null) }
    }

    fun pickSingleImage() {
        viewModelScope.launch {
            try {
                val pickerResult = filePicker.pickSingleImage()
                if (pickerResult != null) {
                    showUploadDialog(CustomerImageUploadData(pickerResult = pickerResult))
                }
            } catch (e: Exception) {
                CustomerLogger.e(TAG, "Error picking image", e)
                _uiState.update { it.copy(error = "Failed to pick image: ${e.message}") }
            }
        }
    }

    fun uploadImage(uploadData: CustomerImageUploadData) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            fileRepository.saveLocally(
                entityType = FileEntityType.CUSTOMER,
                entityUid = customerId,
                result = uploadData.pickerResult,
                isPrimary = uploadData.isPrimary,
            ).onSuccess {
                syncService.markPendingPush(SyncEntity.FILE)
                _uiState.update { it.copy(isUploading = false, showUploadDialog = false, uploadData = null, error = null) }
            }.onFailure { error ->
                CustomerLogger.e(TAG, "Failed to save image locally", error)
                _uiState.update { it.copy(isUploading = false, error = "Failed to save image: ${error.message}") }
            }
        }
    }

    fun setPrimaryImage(uid: String) {
        viewModelScope.launch {
            fileRepository.setPrimaryFile(FileEntityType.CUSTOMER, customerId, uid)
                .onSuccess {
                    syncService.markPendingPush(SyncEntity.FILE)
                    val updated = _uiState.value.images.find { it.uid == uid }
                    _uiState.update { it.copy(selectedImage = updated) }
                }
                .onFailure { error ->
                    CustomerLogger.e(TAG, "Failed to set primary image", error)
                    _uiState.update { it.copy(error = "Failed to set primary image: ${error.message}") }
                }
        }
    }

    fun deleteImage(uid: String) {
        viewModelScope.launch {
            fileRepository.deleteFile(uid)
                .onSuccess {
                    syncService.markPendingPush(SyncEntity.FILE)
                    if (_uiState.value.selectedImage?.uid == uid) hideImageViewer()
                }
                .onFailure { error ->
                    CustomerLogger.e(TAG, "Failed to delete image", error)
                    _uiState.update { it.copy(error = "Failed to delete image: ${error.message}") }
                }
        }
    }

    fun syncImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, syncError = false) }
            fileRepository.pullFromServer(FileEntityType.CUSTOMER, customerId)
                .onFailure {
                    CustomerLogger.e(TAG, "Image sync failed", it)
                    _uiState.update { s -> s.copy(syncError = true) }
                }
            _uiState.update { it.copy(isRefreshing = false) }
        }
        syncService.emit(SyncEvent.TriggerPush(SyncEntity.FILE))
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
