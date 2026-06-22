package com.ampairs.printing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.core.transport.PrintPermissions
import com.ampairs.printing.data.repository.PrinterRepository
import com.ampairs.printing.service.PrintService
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrinterDetailUiState(
    val printer: PrinterProfile? = null,
    /** Document types whose default route currently points at this printer. */
    val routedTypes: Set<DocumentType> = emptySet(),
    val message: String? = null,
)

/**
 * Settings for one printer: test print, delete, and per-document-type routing. Each document type
 * routes to exactly one printer (one row in `print_routing`), so toggling a type on here reassigns it
 * away from any other printer; toggling off removes the route (the type falls back to any printer).
 */
@AssistedInject
class PrinterDetailViewModel(
    @Assisted val printerId: String,
    private val repository: PrinterRepository,
    private val printService: PrintService,
    private val permissions: PrintPermissions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterDetailUiState())
    val uiState: StateFlow<PrinterDetailUiState> = _uiState.asStateFlow()

    private val _deletedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deletedEvent: SharedFlow<Unit> = _deletedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(printer = repository.getProfile(printerId)) }
        }
        repository.observeRoutes()
            .onEach { routes ->
                val mine = routes.filter { it.printerId == printerId }
                    .mapNotNull { row -> DocumentType.entries.firstOrNull { it.key == row.documentType } }
                    .toSet()
                _uiState.update { it.copy(routedTypes = mine) }
            }
            .launchIn(viewModelScope)
    }

    /** Route [documentType] here (reassigning it) or drop the route entirely. */
    fun toggleRoute(documentType: DocumentType, routed: Boolean) {
        viewModelScope.launch {
            if (routed) {
                repository.setRoute(documentType.key, printerId)
            } else {
                repository.removeRoute(documentType.key)
            }
        }
    }

    fun testPrint() {
        val profile = _uiState.value.printer ?: return
        viewModelScope.launch {
            if (!permissions.ensure(profile.connectionType)) {
                _uiState.update { it.copy(message = "Permission required for ${profile.connectionType.name}") }
                return@launch
            }
            val message = runCatching { printService.testPrint(profile) }.fold(
                onSuccess = { outcome ->
                    if (outcome == SendOutcome.SENT_UNCONFIRMED || outcome == SendOutcome.CONFIRMED) {
                        "Test sent to ${profile.name}"
                    } else {
                        "Test failed: ${outcome.name}"
                    }
                },
                onFailure = { it.message ?: "Test failed" },
            )
            _uiState.update { it.copy(message = message) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.removePrinter(printerId)
            _deletedEvent.tryEmit(Unit)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(printerId: String): PrinterDetailViewModel
    }
}
