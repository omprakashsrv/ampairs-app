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
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val accessMode: StorefrontAccessMode = StorefrontAccessMode.PUBLIC,
    val status: StorefrontStatus = StorefrontStatus.UNKNOWN,
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
) : ViewModel() {

    private val _state = MutableStateFlow(StorefrontManagementUiState())
    val state: StateFlow<StorefrontManagementUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<StorefrontManagementEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<StorefrontManagementEvent> = _events.asSharedFlow()

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
    fun onLogoUrlChange(value: String) = _state.update { it.copy(logoUrl = value.trim()) }
    fun onBannerUrlChange(value: String) = _state.update { it.copy(bannerUrl = value.trim()) }
    fun onAccessModeChange(mode: StorefrontAccessMode) = _state.update { it.copy(accessMode = mode) }

    fun save() {
        val current = _state.value
        if (current.isSaving) return
        if (current.name.isBlank()) {
            _events.tryEmit(StorefrontManagementEvent.Message("Store name is required"))
            return
        }
        if (!current.exists) {
            val slug = current.slug
            if (!SLUG_REGEX.matches(slug)) {
                _events.tryEmit(StorefrontManagementEvent.Message("Store URL must be 3–50 lowercase letters, digits, or hyphens"))
                return
            }
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (current.exists) {
                repository.updateStorefront(
                    StorefrontUpdateRequest(
                        name = current.name,
                        description = current.description.ifBlank { null },
                        logoUrl = current.logoUrl.ifBlank { null },
                        bannerUrl = current.bannerUrl.ifBlank { null },
                        accessMode = current.accessMode.name,
                    )
                )
            } else {
                repository.createStorefront(
                    StorefrontCreateRequest(
                        name = current.name,
                        slug = current.slug,
                        description = current.description.ifBlank { null },
                        logoUrl = current.logoUrl.ifBlank { null },
                        bannerUrl = current.bannerUrl.ifBlank { null },
                    )
                )
            }
            result.fold(
                onSuccess = { storefront ->
                    _state.update { it.applyStorefront(storefront).copy(isSaving = false) }
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

    fun preview() {
        val current = _state.value
        if (current.canPreview) _events.tryEmit(StorefrontManagementEvent.OpenPreview(current.slug))
    }

    private fun StorefrontManagementUiState.applyStorefront(s: ManagedStorefront) = copy(
        exists = true,
        uid = s.uid,
        slug = s.slug,
        name = s.name,
        description = s.description.orEmpty(),
        logoUrl = s.logoUrl.orEmpty(),
        bannerUrl = s.bannerUrl.orEmpty(),
        accessMode = parseAccessMode(s.accessMode),
        status = parseStatus(s.status),
        loadError = null,
    )

    private fun parseStatus(raw: String): StorefrontStatus =
        StorefrontStatus.entries.firstOrNull { it.name == raw } ?: StorefrontStatus.UNKNOWN

    private fun parseAccessMode(raw: String): StorefrontAccessMode =
        StorefrontAccessMode.entries.firstOrNull { it.name == raw } ?: StorefrontAccessMode.PUBLIC

    companion object {
        private val SLUG_REGEX = Regex("^[a-z0-9-]{3,50}$")
    }
}
