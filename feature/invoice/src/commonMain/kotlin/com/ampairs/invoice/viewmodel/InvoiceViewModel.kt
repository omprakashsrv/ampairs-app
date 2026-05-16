package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.db.UserRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.domain.Customer
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.TaxInfo
import com.ampairs.invoice.domain.TaxSpec
import com.ampairs.invoice.domain.asDatabaseModel
import com.ampairs.product.domain.Product
import com.ampairs.product.data.repository.ProductRepository
import kotlinx.coroutines.launch


class InvoiceViewModel(
    fromCustomerId: String?, toCustomerId: String?, id: String?,
    val customerRepository: CustomerRepository,
    val invoiceRepository: InvoiceRepository,
    val productRepository: ProductRepository,
    val userRepository: UserRepository,
) :
    ViewModel() {
    fun updateInvoiceItems(products: List<Product>) {
        invoiceItems.removeAll(invoiceItems.filter { invoiceItem ->
            !products.map { it.id }.contains(invoiceItem.product?.id)
        })
        products.forEach { product ->
            val item = invoiceItems.find { invoiceItem -> invoiceItem.product?.id == product.id }
            if (item != null) {
                item.quantity = product.quantity
            } else {
                invoiceItems.add(InvoiceItem(product))
            }
        }
        invoiceItems.removeAll(invoiceItems.filter { invoiceItem -> invoiceItem.quantity <= 0 })
        invoice.items = invoiceItems
        updateTaxInfos()
    }

    fun saveInvoice(onInvoiceSaved: (String) -> Unit) {
        savingInvoice = true
        viewModelScope.launch(DispatcherProvider.io) {
            invoice.updateTaxes()
            invoice.updateDiscount()
            if (invoice.createdBy.isEmpty()) {
                invoice.createdBy = userRepository.getUser()?.id ?: ""
            }
            invoice.updatedBy = userRepository.getUser()?.id ?: ""
            val invoiceEntity = invoice.asDatabaseModel()
            invoiceRepository.saveInvoice(
                invoiceEntity,
                invoiceItems.asDatabaseModel(invoiceEntity.id)
            )
            onInvoiceSaved(invoiceEntity.id)
            savingInvoice = false
        }
    }

    fun updateTaxInfos() {
        // TODO: Re-implement tax info lookup - productRepository.getTaxCode() was removed
        // Need to integrate with TaxCodeDao/TaxCodeRepository for tax info resolution
    }

    var fromCustomer: Customer? = null
    var toCustomer: Customer? = null
    val invoiceItems = mutableStateListOf<InvoiceItem>()
    var selectedInvoiceItem by mutableStateOf<InvoiceItem?>(null)
    var savingInvoice by mutableStateOf(false)
    var invoice = Invoice()

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            if (!id.isNullOrEmpty()) {
                invoice = invoiceRepository.getInvoice(id)
                fromCustomer = invoice.fromCustomer
                toCustomer = invoice.toCustomer
                invoiceItems.addAll(invoice.items)
            } else {
                fromCustomer =
                    fromCustomerId?.let { customerRepository.getCustomer(it) }
                toCustomer =
                    toCustomerId?.let { customerRepository.getCustomer(it) }
                invoice.fromCustomer = fromCustomer
                invoice.toCustomer = toCustomer
            }
        }
    }
}
