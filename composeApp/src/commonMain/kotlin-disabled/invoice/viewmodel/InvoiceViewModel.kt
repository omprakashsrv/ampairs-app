package com.ampairs.invoice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.db.UserRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.customer.db.CustomerRepository
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.asDomainModel
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.TaxInfo
import com.ampairs.invoice.domain.TaxSpec
import com.ampairs.invoice.domain.asDatabaseModel
import com.ampairs.product.domain.Product
import com.ampairs.product.ui.product.ProductViewModel
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.launch


class InvoiceViewModel(
    fromCustomerId: String?, toCustomerId: String?, id: String?,
    val customerRepository: CustomerRepository,
    val invoiceRepository: InvoiceRepository,
    val productRepository: ProductRepository,
    val userRepository: UserRepository,
    productViewModel: ProductViewModel,
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
        viewModelScope.launch(DispatcherProvider.io) {
            invoiceItems.forEach { invoiceItem ->
                if (invoiceItem.product != null && invoiceItem.product?.taxInfos == null) {
                    invoiceItem.product?.taxInfos =
                        invoiceItem.product?.taxCode?.let { productRepository.getTaxCode(it)?.taxInfos }
                }
                invoiceItem.taxInfos =
                    invoiceItem.product?.taxInfos?.filter { it.taxSpec.name == invoice.taxSpec.name }
                        ?.map {
                            TaxInfo(
                                id = it.id,
                                name = it.name,
                                formattedName = it.formattedName,
                                taxSpec = TaxSpec.valueOf(it.taxSpec.name),
                                percentage = it.percentage,
                                value = 0.0
                            )
                        } ?: arrayListOf()
            }
        }
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
                productViewModel.cartProducts =
                    invoiceItems.map { invoiceItem -> invoiceItem.product!! }.toMutableList()
            } else {
                fromCustomer =
                    fromCustomerId?.let { customerRepository.getCustomer(it)?.asDomainModel() }
                toCustomer =
                    toCustomerId?.let { customerRepository.getCustomer(it)?.asDomainModel() }
                invoice.fromCustomer = fromCustomer
                invoice.toCustomer = toCustomer
            }
        }
    }
}