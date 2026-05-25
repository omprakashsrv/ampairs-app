package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.domain.Customer
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.TaxInfo
import com.ampairs.invoice.domain.TaxSpec
import com.ampairs.invoice.domain.asDatabaseModel
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.launch


@AssistedInject
class InvoiceViewModel(
    @Assisted fromCustomerId: String?,
    @Assisted toCustomerId: String?,
    @Assisted id: String?,
    val customerDataService: CustomerDataService,
    val invoiceRepository: InvoiceRepository,
    val productDataService: ProductDataService,
    val tokenRepository: TokenRepository,
) :
    ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(fromCustomerId: String?, toCustomerId: String?, id: String?): InvoiceViewModel
    }
    fun updateInvoiceItems(products: List<ProductSummary>) {
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
            val userId = tokenRepository.getCurrentUserId() ?: ""
            if (invoice.createdBy.isEmpty()) {
                invoice.createdBy = userId
            }
            invoice.updatedBy = userId
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
                    fromCustomerId?.let { customerDataService.getById(it) }
                toCustomer =
                    toCustomerId?.let { customerDataService.getById(it) }
                invoice.fromCustomer = fromCustomer
                invoice.toCustomer = toCustomer
            }
        }
    }
}
