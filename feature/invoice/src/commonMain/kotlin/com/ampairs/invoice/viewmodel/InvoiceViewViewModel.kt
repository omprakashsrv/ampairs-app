package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.common.di.AppScope
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
    val invoiceRepository: InvoiceRepository
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(invoiceId: String): InvoiceViewViewModel
    }

    var invoice by mutableStateOf(Invoice())
        private set
    var savingInvoice by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            invoice = invoiceRepository.getInvoice(invoiceId)
        }
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
