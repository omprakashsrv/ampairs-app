package com.ampairs.ecom.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.ecom.api.model.ManagedStorefront
import com.ampairs.ecom.api.model.StorefrontAccessMode
import com.ampairs.ecom.api.model.StorefrontCreateRequest
import com.ampairs.ecom.api.model.StorefrontStatus
import com.ampairs.ecom.api.model.StorefrontUpdateRequest
import com.ampairs.ecom.data.repository.StorefrontManagementRepository
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FileRepository
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.file.picker.FilePicker
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorefrontManagementUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    /** True once a storefront exists for this workspace (drives create vs edit UI). */
    val exists: Boolean = false,
    val uid: String = "",
    val slug: String = "",
    val name: String = "",
    val description: String = "",
    val accessMode: StorefrontAccessMode = StorefrontAccessMode.PUBLIC,
    val status: StorefrontStatus = StorefrontStatus.UNKNOWN,
    /** Logo image being uploaded / already uploaded (file module), null when none. */
    val logoFile: FileItem? = null,
    /** Banner image being uploaded / already uploaded (file module), null when none. */
    val bannerFile: FileItem? = null,
    /** Load-level error (null while OK). Per-action errors are surfaced as [StorefrontManagementEvent.Message]. */
    val loadError: String? = null,
) {
    val isPublished: Boolean get() = status == StorefrontStatus.PUBLISHED
    val canPreview: Boolean get() = exists && slug.isNotBlank()
    /** Slug is only editable while creating; permanent once the storefront exists. */
    val slugEditable: Boolean get() = !exists
}

sealed interface StorefrontManagementEvent {
    data class Message(val text: String) : StorefrontManagementEvent
    data class OpenPreview(val slug: String) : StorefrontManagementEvent
}

@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
class StorefrontManagementViewModel(
    private val repository: StorefrontManagementRepository,
    private val filePicker: FilePicker,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StorefrontManagementUiState())
    val state: StateFlow<StorefrontManagementUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<StorefrontManagementEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<StorefrontManagementEvent> = _events.asSharedFlow()

    private var fileObserversJob: Job? = null
    /** Storefront URLs already persisted server-side, to avoid re-pushing the same URL on every emit. */
    private var savedLogoUrl: String = ""
    private var savedBannerUrl: String = ""

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            repository.getStorefront().fold(
                onSuccess = { storefront ->
                    if (storefront == null) {
                        _state.update { it.copy(isLoading = false, exists = false) }
                    } else {
                        _state.update { it.applyStorefront(storefront).copy(isLoading = false) }
                        startFileObservers(storefront.uid)
                    }
                },
                onFailure = { error ->
                    EcomLogger.e("StorefrontMgmt", "load failed", error)
                    _state.update { it.copy(isLoading = false, loadError = error.message ?: "Failed to load storefront") }
                },
            )
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onSlugChange(value: String) = _state.update { it.copy(slug = value.lowercase().trim()) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }
    fun onAccessModeChange(mode: StorefrontAccessMode) = _state.update { it.copy(accessMode = mode) }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        if (current.name.isBlank()) {
            _events.tryEmit(StorefrontManagementEvent.Message("Store name is required"))
            return
        }
        if (!current.exists && !SLUG_REGEX.matches(current.slug)) {
            _events.tryEmit(StorefrontManagementEvent.Message("Store URL must be 3–50 lowercase letters, digits, or hyphens"))
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (current.exists) {
                repository.updateStorefront(
                    StorefrontUpdateRequest(
                        name = current.name,
                        description = current.description.ifBlank { null },
                        accessMode = current.accessMode.name,
                    )
                )
            } else {
                repository.createStorefront(
                    StorefrontCreateRequest(
                        name = current.name,
                        slug = current.slug,
                        description = current.description.ifBlank { null },
                    )
                )
            }
            result.fold(
                onSuccess = { storefront ->
                    _state.update { it.applyStorefront(storefront).copy(isSaving = false) }
                    startFileObservers(storefront.uid)
                    _events.tryEmit(StorefrontManagementEvent.Message("Storefront saved"))
                },
                onFailure = { error ->
                    EcomLogger.e("StorefrontMgmt", "save failed", error)
                    _state.update { it.copy(isSaving = false) }
                    _events.tryEmit(StorefrontManagementEvent.Message(error.message ?: "Failed to save storefront"))
                },
            )
        }
    }

    fun publish() = togglePublish(publish = true)
    fun unpublish() = togglePublish(publish = false)

    private fun togglePublish(publish: Boolean) {
        val current = _state.value
        if (current.isSaving || !current.exists) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (publish) repository.publish() else repository.unpublish()
            result.fold(
                onSuccess = { storefront ->
                    _state.update { it.applyStorefront(storefront).copy(isSaving = false) }
                    _events.tryEmit(
                        StorefrontManagementEvent.Message(if (publish) "Storefront published" else "Storefront unpublished")
                    )
                },
                onFailure = { error ->
                    EcomLogger.e("StorefrontMgmt", "publish toggle failed", error)
                    _state.update { it.copy(isSaving = false) }
                    _events.tryEmit(StorefrontManagementEvent.Message(error.message ?: "Action failed"))
                },
            )
        }
    }

    // ── Logo / banner upload via the shared file module ──

    fun pickLogo() = pickImage(FileEntityType.STOREFRONT_LOGO)
    fun pickBanner() = pickImage(FileEntityType.STOREFRONT_BANNER)

    private fun pickImage(type: FileEntityType) {
        val storefrontUid = _state.value.uid
        if (storefrontUid.isBlank()) return
        viewModelScope.launch {
            val picked = filePicker.pickSingleImage() ?: return@launch
            fileRepository.saveLocally(type, storefrontUid, picked, isPrimary = true).onFailure { error ->
                EcomLogger.e("StorefrontMgmt", "saveLocally failed", error)
                _events.tryEmit(StorefrontManagementEvent.Message(error.message ?: "Failed to add image"))
            }
        }
    }

    fun removeLogo() = _state.value.logoFile?.let { removeImage(it.uid) }
    fun removeBanner() = _state.value.bannerFile?.let { removeImage(it.uid) }

    private fun removeImage(fileUid: String) {
        viewModelScope.launch {
            fileRepository.deleteFile(fileUid).onFailure { error ->
                _events.tryEmit(StorefrontManagementEvent.Message(error.message ?: "Failed to remove image"))
            }
        }
    }

    fun preview() {
        val current = _state.value
        if (current.canPreview) _events.tryEmit(StorefrontManagementEvent.OpenPreview(current.slug))
    }

    /**
     * Observe the workspace's logo & banner images from the file module. Once an upload completes,
     * bridge the served URL into the storefront's logo_url / banner_url so the customer storefront
     * (which reads those columns) shows the image.
     */
    private fun startFileObservers(storefrontUid: String) {
        if (storefrontUid.isBlank() || fileObserversJob != null) return
        fileObserversJob = viewModelScope.launch {
            launch {
                fileRepository.observePrimaryFile(FileEntityType.STOREFRONT_LOGO, storefrontUid)
                    .onEach { file ->
                        _state.update { it.copy(logoFile = file) }
                        bridgeUploadedUrl(file, isLogo = true)
                    }
                    .collect {}
            }
            launch {
                fileRepository.observePrimaryFile(FileEntityType.STOREFRONT_BANNER, storefrontUid)
                    .onEach { file ->
                        _state.update { it.copy(bannerFile = file) }
                        bridgeUploadedUrl(file, isLogo = false)
                    }
                    .collect {}
            }
        }
    }

    private fun bridgeUploadedUrl(file: FileItem?, isLogo: Boolean) {
        if (file == null) return
        if (file.uploadStatus != FileUploadStatus.COMPLETED || file.imageUrl.isBlank()) return
        val alreadySaved = if (isLogo) savedLogoUrl else savedBannerUrl
        if (file.imageUrl == alreadySaved) return
        // Optimistically record so concurrent emits don't double-push.
        if (isLogo) savedLogoUrl = file.imageUrl else savedBannerUrl = file.imageUrl
        viewModelScope.launch {
            val request = if (isLogo) StorefrontUpdateRequest(logoUrl = file.imageUrl)
            else StorefrontUpdateRequest(bannerUrl = file.imageUrl)
            repository.updateStorefront(request).onFailure { error ->
                EcomLogger.e("StorefrontMgmt", "failed to bridge image url", error)
                // Reset so a retry can re-push on the next emit.
                if (isLogo) savedLogoUrl = "" else savedBannerUrl = ""
            }
        }
    }

    private fun StorefrontManagementUiState.applyStorefront(s: ManagedStorefront): StorefrontManagementUiState {
        savedLogoUrl = s.logoUrl.orEmpty()
        savedBannerUrl = s.bannerUrl.orEmpty()
        return copy(
            exists = true,
            uid = s.uid,
            slug = s.slug,
            name = s.name,
            description = s.description.orEmpty(),
            accessMode = parseAccessMode(s.accessMode),
            status = parseStatus(s.status),
            loadError = null,
        )
    }

    private fun parseStatus(raw: String): StorefrontStatus =
        StorefrontStatus.entries.firstOrNull { it.name == raw } ?: StorefrontStatus.UNKNOWN

    private fun parseAccessMode(raw: String): StorefrontAccessMode =
        StorefrontAccessMode.entries.firstOrNull { it.name == raw } ?: StorefrontAccessMode.PUBLIC

    companion object {
        private val SLUG_REGEX = Regex("^[a-z0-9-]{3,50}$")
    }
}
