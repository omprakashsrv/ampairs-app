package com.ampairs.cbmaintenance.ui.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.TicketRepository
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbstore.data.repository.StoreLookup
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TICKET_UID_PREFIX = "TKT"

data class RaiseTicketUiState(
    val storeId: String = "",
    val assetCategory: String = "",
    val subCategory: String = "",
    val description: String = "",
    val storeOptions: List<Store> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() = storeId.isNotBlank() && assetCategory.isNotBlank() && subCategory.isNotBlank()
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class RaiseTicketViewModel(
    private val ticketRepository: TicketRepository,
    private val storeLookup: StoreLookup,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaiseTicketUiState())
    val uiState: StateFlow<RaiseTicketUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(storeOptions = storeLookup.activeStores()) }
        }
    }

    fun onStore(v: String) = _uiState.update { it.copy(storeId = v) }
    fun onAssetCategory(v: String) = _uiState.update { it.copy(assetCategory = v) }
    fun onSubCategory(v: String) = _uiState.update { it.copy(subCategory = v) }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Store, asset and issue are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // zonalOfficeId left blank — the server denormalizes it from the store on upsert.
            val ticket = Ticket(
                uid = UidGenerator.generateUid(TICKET_UID_PREFIX),
                storeId = state.storeId,
                assetCategory = state.assetCategory.trim(),
                subCategory = state.subCategory.trim(),
                description = state.description.trim().ifBlank { null },
                status = "OPEN",
                active = true,
            )
            val result = ticketRepository.raiseTicket(ticket)
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to raise ticket")
            }
        }
    }
}
