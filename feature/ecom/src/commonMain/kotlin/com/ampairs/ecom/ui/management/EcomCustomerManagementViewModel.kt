package com.ampairs.ecom.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.EcomContactResponse
import com.ampairs.ecom.data.repository.EcomCustomerManagementRepository
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

data class EcomCustomerManagementUiState(
    val contacts: List<EcomContactResponse> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
)

sealed interface EcomCustomerManagementEvent {
    data class Message(val text: String) : EcomCustomerManagementEvent
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class EcomCustomerManagementViewModel(
    private val repository: EcomCustomerManagementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EcomCustomerManagementUiState())
    val state: StateFlow<EcomCustomerManagementUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EcomCustomerManagementEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<EcomCustomerManagementEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            repository.getContacts().fold(
                onSuccess = { contacts -> _state.update { it.copy(contacts = contacts, isLoading = false) } },
                onFailure = { error ->
                    EcomLogger.e("EcomCustomerMgmt", "load failed", error)
                    _state.update { it.copy(isLoading = false, loadError = error.message ?: "Failed to load ecom users") }
                },
            )
        }
    }

    /** Restrict (false) or re-enable (true) a linked account's ordering access. */
    fun setContactActive(contactUid: String, active: Boolean) {
        viewModelScope.launch {
            repository.setContactActive(contactUid, active).fold(
                onSuccess = { load() },
                onFailure = { error ->
                    EcomLogger.e("EcomCustomerMgmt", "setContactActive failed", error)
                    _events.tryEmit(EcomCustomerManagementEvent.Message(error.message ?: "Couldn't update that account"))
                },
            )
        }
    }
}
