package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.invoice.db.InvoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InvoiceViewViewModel(val invoiceId: String, val invoiceRepository: InvoiceRepository) :
    ViewModel() {
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

    var invoice by mutableStateOf(invoiceRepository.getInvoice(invoiceId))
    var savingInvoice by mutableStateOf(false)

}