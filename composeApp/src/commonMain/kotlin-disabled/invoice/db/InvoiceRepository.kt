package com.ampairs.invoice.db

import androidx.paging.PagingSource
import com.ampairs.common.flower_core.Resource
import com.ampairs.customer.db.dao.CustomerDao
import com.ampairs.invoice.api.InvoiceApi
import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.domain.Invoice
import com.ampairs.product.db.dao.ProductDao
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(
    val invoiceDao: InvoiceDao,
    val productDao: ProductDao,
    val customerDao: CustomerDao,
    val invoiceApi: InvoiceApi,
) {
    
    // TODO: Implement all methods with Room DAOs
    // For now, providing minimal stubs to allow compilation
    
    suspend fun saveInvoice(invoiceEntity: InvoiceEntity, invoiceItems: List<InvoiceItemEntity>) {
        throw NotImplementedError("Invoice operations not yet implemented with Room")
    }

    suspend fun saveInvoice(invoice: Invoice?) {
        throw NotImplementedError("Invoice operations not yet implemented with Room")
    }

    fun getInvoice(id: String): Invoice {
        throw NotImplementedError("Invoice operations not yet implemented with Room")
    }

    fun getInvoiceResource(): Flow<Resource<List<InvoiceApiModel>>> {
        throw NotImplementedError("Invoice operations not yet implemented with Room")
    }

    suspend fun updateInvoices(invoices: List<InvoiceApiModel>) {
        throw NotImplementedError("Invoice operations not yet implemented with Room")
    }

    suspend fun updateInvoice(invoiceApiModel: InvoiceApiModel) {
        throw NotImplementedError("Invoice operations not yet implemented with Room")
    }

    fun getInvoicePaging(searchText: String): PagingSource<Int, InvoiceEntity> {
        return if (searchText.isBlank()) {
            invoiceDao.getAllInvoicesPagingSource()
        } else {
            invoiceDao.getInvoicesBySearchPagingSource(searchText)
        }
    }
}