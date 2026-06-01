package com.ampairs.product.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.repository.ProductImageFilePicker
import com.ampairs.product.data.repository.ProductImagePickerResult
import com.ampairs.product.db.Constants
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogFormState(
    val name: String = "",
    val active: Boolean = true,
    val isSaving: Boolean = false,
    val nameError: Boolean = false,
    // existing server image URL (from DB after pull sync)
    val currentImageUrl: String? = null,
    // newly picked image bytes (shown as preview, uploaded on save)
    val pendingImage: ProductImagePickerResult? = null,
    val imageRemoved: Boolean = false,
)

sealed interface CatalogFormEvent {
    data object SaveSuccess : CatalogFormEvent
    data class ShowError(val message: String) : CatalogFormEvent
}

@AssistedInject
class CatalogFormViewModel(
    private val repository: ProductCatalogRepository,
    private val syncService: CentralSyncService,
    private val filePicker: ProductImageFilePicker,
    @Assisted val catalogType: ProductCatalogType,
    @Assisted val itemId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(catalogType: ProductCatalogType, itemId: String?): CatalogFormViewModel
    }

    private val _state = MutableStateFlow(CatalogFormState())
    val state: StateFlow<CatalogFormState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CatalogFormEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CatalogFormEvent> = _events.asSharedFlow()

    val isEditing: Boolean get() = itemId != null

    init {
        if (itemId != null) loadItem(itemId)
    }

    private fun loadItem(id: String) {
        viewModelScope.launch {
            val item = repository.getItemById(catalogType, id) ?: return@launch
            _state.value = _state.value.copy(
                name = item.name,
                active = item.active,
                currentImageUrl = item.imageUrl,
            )
        }
    }

    fun onNameChange(name: String) {
        _state.value = _state.value.copy(name = name, nameError = false)
    }

    fun onActiveToggle(active: Boolean) {
        _state.value = _state.value.copy(active = active)
    }

    fun pickImage() {
        viewModelScope.launch {
            val picked = filePicker.pickImage() ?: return@launch
            _state.value = _state.value.copy(
                pendingImage = picked,
                imageRemoved = false,
            )
        }
    }

    fun removeImage() {
        _state.value = _state.value.copy(
            pendingImage = null,
            currentImageUrl = null,
            imageRemoved = true,
        )
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.value = current.copy(nameError = true)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)

            val resolvedId: String
            val entityResult: Result<Unit>
            if (isEditing) {
                resolvedId = itemId!!
                entityResult = repository.updateItem(catalogType, resolvedId, current.name.trim(), current.active)
            } else {
                resolvedId = when (catalogType) {
                    ProductCatalogType.BRANDS -> UidGenerator.generateUid(Constants.PRODUCT_BRAND_PREFIX)
                    ProductCatalogType.CATEGORIES -> UidGenerator.generateUid(Constants.PRODUCT_CATEGORY_PREFIX)
                    ProductCatalogType.SUB_CATEGORIES -> UidGenerator.generateUid(Constants.PRODUCT_SUB_CATEGORY_PREFIX)
                    ProductCatalogType.GROUPS -> UidGenerator.generateUid(Constants.PRODUCT_GROUP_PREFIX)
                }
                entityResult = repository.createItem(catalogType, resolvedId, current.name.trim())
            }

            if (entityResult.isFailure) {
                _state.value = _state.value.copy(isSaving = false)
                _events.emit(CatalogFormEvent.ShowError(entityResult.exceptionOrNull()?.message ?: "Save failed"))
                return@launch
            }

            when {
                current.pendingImage != null -> {
                    val imgResult = repository.uploadItemImage(
                        type = catalogType,
                        itemId = resolvedId,
                        fileName = current.pendingImage.fileName,
                        contentType = current.pendingImage.contentType,
                        imageData = current.pendingImage.imageData,
                    )
                    if (imgResult.isFailure) {
                        _state.value = _state.value.copy(isSaving = false)
                        _events.emit(CatalogFormEvent.ShowError(imgResult.exceptionOrNull()?.message ?: "Image upload failed"))
                        return@launch
                    }
                }
                current.imageRemoved && isEditing -> {
                    repository.removeItemImage(catalogType, resolvedId)
                }
            }

            _state.value = _state.value.copy(isSaving = false)
            syncService.markPendingPush(SyncEntity.PRODUCT_CATALOG)
            _events.emit(CatalogFormEvent.SaveSuccess)
        }
    }
}
