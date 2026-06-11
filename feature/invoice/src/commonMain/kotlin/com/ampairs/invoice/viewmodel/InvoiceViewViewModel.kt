package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.editor.DocSyncUi
import com.ampairs.common.di.WorkspaceScope
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
    private val syncService: CentralSyncService,
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

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            invoice = invoiceRepository.getInvoice(invoiceId)
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

    fun retrySync() {
        syncService.emit(SyncEvent.TriggerPush(SyncEntity.INVOICE))
    }

    fun saveInvoice() {
        savingInvoice = true
        viewModelScope.launch(DispatcherProvider.io) {
            invoiceRepository.saveInvoice(invoice)
            invoice = invoiceRepository.getInvoice(invoiceId)
            viewModelScope.launch(Dispatchers.Main) {
                savingInvoice = false
            }
        }
    }
}
