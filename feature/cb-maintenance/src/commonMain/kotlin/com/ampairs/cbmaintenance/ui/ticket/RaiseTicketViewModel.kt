package com.ampairs.cbmaintenance.ui.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.TicketBucketRepository
import com.ampairs.cbmaintenance.data.repository.TicketRepository
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbmaintenance.domain.model.TicketBucket
import com.ampairs.cbstore.data.repository.StoreLookup
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TICKET_UID_PREFIX = "TKT"

// The backend ticket `description` column is TEXT (unbounded); cap the free-text input at a sane
// maximum so a runaway paste can't bloat the row.
private const val DESCRIPTION_MAX = 1000

data class RaiseTicketUiState(
    val storeId: String = "",
    // Cascading classification selections (from the ticket-bucket catalog).
    val department: String = "",
    val category: String = "",
    val subCategory1: String = "",
    val subCategory2: String = "",
    val description: String = "",
    // true once the user types their own description — auto-generation then stops overriding it.
    val descriptionEdited: Boolean = false,
    val storeOptions: List<Store> = emptyList(),
    val buckets: List<TicketBucket> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    // Derived cascade options (distinct, sorted), narrowed by the selection above each level.
    val departmentOptions: List<String>
        get() = buckets.map { it.department }.filter { it.isNotBlank() }.distinct().sorted()

    val categoryOptions: List<String>
        get() = buckets.filter { it.department == department }
            .map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    val subCategory1Options: List<String>
        get() = buckets.filter { it.department == department && it.category == category }
            .map { it.subCategory1 }.filter { it.isNotBlank() }.distinct().sorted()

    val subCategory2Options: List<String>
        get() = buckets.filter {
            it.department == department && it.category == category && it.subCategory1 == subCategory1
        }.map { it.subCategory2 }.filter { it.isNotBlank() }.distinct().sorted()

    val isValid: Boolean get() = storeId.isNotBlank() && category.isNotBlank() && subCategory1.isNotBlank()
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class RaiseTicketViewModel(
    private val ticketRepository: TicketRepository,
    private val ticketBucketRepository: TicketBucketRepository,
    private val storeLookup: StoreLookup,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaiseTicketUiState())
    val uiState: StateFlow<RaiseTicketUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(storeOptions = storeLookup.activeStores()) }
        }
        ticketBucketRepository.observeBuckets()
            .onEach { list -> _uiState.update { it.copy(buckets = list) } }
            .launchIn(viewModelScope)
        // Make sure the classification catalog is present (pull-only reference data).
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CB_TICKET_BUCKET))
    }

    fun onStore(v: String) = _uiState.update { it.copy(storeId = v) }

    // Auto-generate a readable description from the cascade selection (Equipment · Issue · detail),
    // unless the user has typed their own — mirrors the PM schedule form's task-name behaviour.
    private fun autoDescription(category: String, sub1: String, sub2: String): String =
        listOf(category, sub1, sub2).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" · ")

    // Selecting a higher level clears the lower levels so the cascade stays consistent;
    // the description re-derives from the new selection while it hasn't been hand-edited.
    fun onDepartment(v: String) = _uiState.update {
        val s = it.copy(department = v, category = "", subCategory1 = "", subCategory2 = "")
        if (s.descriptionEdited) s else s.copy(description = autoDescription("", "", ""))
    }

    fun onCategory(v: String) = _uiState.update {
        val s = it.copy(category = v, subCategory1 = "", subCategory2 = "")
        if (s.descriptionEdited) s else s.copy(description = autoDescription(v, "", ""))
    }

    fun onSubCategory1(v: String) = _uiState.update {
        val s = it.copy(subCategory1 = v, subCategory2 = "")
        if (s.descriptionEdited) s else s.copy(description = autoDescription(s.category, v, ""))
    }

    fun onSubCategory2(v: String) = _uiState.update {
        val s = it.copy(subCategory2 = v)
        if (s.descriptionEdited) s else s.copy(description = autoDescription(s.category, s.subCategory1, v))
    }

    // Typing marks the field hand-edited; clearing it hands control back to auto-generation.
    fun onDescription(v: String) =
        _uiState.update { it.copy(description = v.take(DESCRIPTION_MAX), descriptionEdited = v.isNotBlank()) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Outlet, equipment and issue are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // The issue combines the two sub-category levels when a second level was picked.
            val issue = listOf(state.subCategory1.trim(), state.subCategory2.trim())
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            // Resolve the exact taxonomy leaf the cascade selection maps to, so reports can join
            // ticket -> ticket_bucket for the full classification.
            val bucketId = state.buckets.firstOrNull {
                it.department == state.department && it.category == state.category &&
                    it.subCategory1 == state.subCategory1 && it.subCategory2 == state.subCategory2
            }?.uid
            // zonalOfficeId left blank — the server denormalizes it from the store on upsert.
            val ticket = Ticket(
                uid = UidGenerator.generateUid(TICKET_UID_PREFIX),
                storeId = state.storeId,
                assetCategory = state.category.trim(),
                subCategory = issue,
                ticketBucketId = bucketId,
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
