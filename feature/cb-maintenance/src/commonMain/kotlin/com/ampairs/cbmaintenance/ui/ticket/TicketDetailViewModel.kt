package com.ampairs.cbmaintenance.ui.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbmaintenance.data.repository.PmEntryRepository
import com.ampairs.cbmaintenance.data.repository.TicketRepository
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val PM_ENTRY_UID_PREFIX = "PME"

data class TicketDetailUiState(
    val ticket: Ticket? = null,
    val pmEntries: List<PmEntry> = emptyList(),
    val isCreating: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

/**
 * Shows a ticket, the PM tasks raised against it, and lets the user raise a new one. Creating a PM
 * task writes an ad-hoc PM entry carrying this ticket's id; completing that entry (from the PM due
 * list) auto-resolves the ticket server-side. The linkage is visible both ways: this screen lists
 * the ticket's PM entries, and each PM-due card shows the ticket it addresses.
 */
@OptIn(ExperimentalTime::class)
@AssistedInject
class TicketDetailViewModel(
    @Assisted private val ticketId: String,
    private val ticketRepository: TicketRepository,
    private val pmEntryRepository: PmEntryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        loadTicket()
        pmEntryRepository.observeEntriesForTicket(ticketId)
            .onEach { list -> _uiState.update { it.copy(pmEntries = list) } }
            .launchIn(viewModelScope)
    }

    private fun loadTicket() {
        viewModelScope.launch {
            _uiState.update { it.copy(ticket = ticketRepository.getTicket(ticketId)) }
        }
    }

    fun refresh() = loadTicket()

    fun createPmTask() {
        val ticket = _uiState.value.ticket ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null, message = null) }
            val entry = PmEntry(
                uid = UidGenerator.generateUid(PM_ENTRY_UID_PREFIX),
                storeId = ticket.storeId,
                assetCategory = ticket.assetCategory,
                source = "AD_HOC",
                dueDate = Clock.System.now().toString(),
                status = "DUE",
                ticketId = ticket.uid,
                active = true,
            )
            val result = pmEntryRepository.createEntry(entry)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isCreating = false, message = "PM task created — complete it from PM Due to close this ticket")
                } else {
                    it.copy(isCreating = false, error = result.exceptionOrNull()?.message ?: "Failed to create PM task")
                }
            }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(ticketId: String): TicketDetailViewModel
    }
}
