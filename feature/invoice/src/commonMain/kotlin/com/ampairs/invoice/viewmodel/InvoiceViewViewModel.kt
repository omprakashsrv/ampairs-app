package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.editor.DocSyncUi
import com.ampairs.invoice.print.InvoicePrintValueProvider
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.service.PrintCoordinator
import com.ampairs.unit.data.repository.UnitLookup
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AssistedInject
class InvoiceViewViewModel(
    @Assisted val invoiceId: String,
    val invoiceRepository: InvoiceRepository,
    private val unitLookup: UnitLookup,
    private val syncService: CentralSyncService,
    private val printCoordinator: PrintCoordinator,
    private val invoicePrintProvider: InvoicePrintValueProvider,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(invoiceId: String): InvoiceViewViewModel
    }

    var invoice by mutableStateOf(Invoice())
        private set
    var savingInvoice by mutableStateOf(false)
        private set

    /** Live document sync chip: this row's synced flag + the INVOICE entity's sync activity. */
    var syncUi by mutableStateOf(DocSyncUi.NONE)
        private set

    /** Transient result of the last thermal print attempt (shown then dismissed by the screen). */
    var printMessage by mutableStateOf<String?>(null)
        private set
    var printing by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            invoice = invoiceRepository.getInvoice(invoiceId)
            resolveUnitNames()
            refreshSyncFlag()
        }
        syncService.observeEntity(SyncEntity.INVOICE)
            .onEach { state ->
                when (state?.status) {
                    is SyncStatus.Syncing -> syncUi = DocSyncUi.SYNCING
                    is SyncStatus.Failed -> {
                        refreshSyncFlag()
                        if (syncUi == DocSyncUi.OFFLINE) syncUi = DocSyncUi.FAILED
                    }
                    else -> refreshSyncFlag()
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshSyncFlag() {
        if (invoiceId.isBlank()) return
        syncUi = if (invoiceRepository.isInvoiceSynced(invoiceId)) DocSyncUi.SYNCED else DocSyncUi.OFFLINE
    }

    /** unitName is transient (display-only) — resolve it from the unit catalog after load. */
    private suspend fun resolveUnitNames() {
        invoice.items.forEach { item ->
            if (item.unitName.isBlank() && item.unitId.isNotBlank()) {
                unitLookup.getUnitById(item.unitId)?.let { unit ->
                    item.unitName = unit.shortName.ifBlank { unit.name }
                }
            }
        }
    }

    fun retrySync() {
        syncService.emit(SyncEvent.TriggerPush(SyncEntity.INVOICE))
    }

    /**
     * Print this invoice to the routed thermal printer via the offline-first spool. The idempotency
     * key (one per tap) makes a retry safe — the spool will never double-print (§19).
     */
    fun printThermal() {
        if (printing) return
        printing = true
        viewModelScope.launch(DispatcherProvider.io) {
            val key = UidGenerator.generateUid("PJB")
            val result = printCoordinator.print(invoicePrintProvider, invoiceId, key)
            val message = result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        SendOutcome.CONFIRMED -> "Printed"
                        SendOutcome.SENT_UNCONFIRMED -> "Sent to printer"
                        SendOutcome.TRANSIENT_FAILURE -> "Printer unavailable — queued for retry"
                        SendOutcome.PERMANENT_FAILURE -> "No printer configured for invoices"
                    }
                },
                onFailure = { it.message ?: "Print failed" },
            )
            viewModelScope.launch(Dispatchers.Main) {
                printMessage = message
                printing = false
            }
        }
    }

    fun clearPrintMessage() {
        printMessage = null
    }

    fun saveInvoice() {
        savingInvoice = true
        viewModelScope.launch(DispatcherProvider.io) {
            invoiceRepository.saveInvoice(invoice)
            invoice = invoiceRepository.getInvoice(invoiceId)
            resolveUnitNames()
            viewModelScope.launch(Dispatchers.Main) {
                savingInvoice = false
            }
        }
    }
}
