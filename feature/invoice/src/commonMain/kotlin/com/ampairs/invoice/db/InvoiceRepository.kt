package com.ampairs.invoice.db

import androidx.paging.PagingSource
import androidx.room.Transaction
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.common.model.Response
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.invoice.api.InvoiceApi
import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.invoice.api.model.toInvoiceDatabaseModel
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.dto.asDatabaseModel
import com.ampairs.invoice.db.dto.asItemDatabaseModel
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.api.model.toApiModel
import com.ampairs.invoice.domain.asDomainModelSimple
import com.ampairs.product.data.ProductDataService
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json

@Inject
class InvoiceRepository(
    val invoiceDao: InvoiceDao,
    val invoiceItemDao: InvoiceItemDao,
    val productDataService: ProductDataService,
    val customerDataService: CustomerDataService,
    val invoiceApi: InvoiceApi,
) {
    @Transaction
    suspend fun saveInvoice(invoiceEntity: InvoiceEntity, invoiceItems: List<InvoiceItemEntity>) {
        invoiceDao.insert(invoiceEntity)
        invoiceItemDao.insertAll(invoiceItems)
        val invoice = getInvoice(invoiceEntity.id)
        saveInvoice(invoice)
    }

    suspend fun saveInvoice(invoice: Invoice?) {
        invoice?.toApiModel()?.let {
            val response = invoiceApi.updateInvoice(it)
            val updatedInvoice = response.data
            updatedInvoice?.toInvoiceDatabaseModel()?.let { entity -> invoiceDao.insert(entity) }
        }
    }

    suspend fun getInvoice(id: String): Invoice {
        val entity = invoiceDao.selectById(id) ?: return Invoice()
        val itemEntities = invoiceItemDao.getInvoiceItems(id)

        val invoice = entity.asDomainModelSimple()
        invoice.fromCustomer = invoice.fromCustomer?.uid?.let { customerDataService.getById(it) }
            ?: invoice.fromCustomer
        invoice.toCustomer = invoice.toCustomer?.uid?.let { customerDataService.getById(it) }
            ?: invoice.toCustomer

        val products = productDataService.getByIds(itemEntities.map { it.product_id })
        invoice.items = itemEntities.map { itemEntity ->
            val product = products.find { it.id == itemEntity.product_id }
            val item = InvoiceItem(product)
            item.id = itemEntity.id
            item.quantity = itemEntity.quantity
            item.price = itemEntity.selling_price
            item.productPrice = itemEntity.product_price
            item.mrp = itemEntity.mrp
            item.dp = itemEntity.dp
            item.totalCost = itemEntity.total_cost
            item.basePrice = itemEntity.base_price
            item.totalTax = itemEntity.total_tax
            item.active = itemEntity.active == 1L
            item.softDeleted = itemEntity.soft_deleted == 1L
            item.discount = itemEntity.discount?.let {
                Json.decodeFromString<List<Discount>>(it).toMutableList().let { list ->
                    val stateList = androidx.compose.runtime.mutableStateListOf<Discount>()
                    stateList.addAll(list)
                    stateList
                }
            } ?: item.discount
            item.discountPercent = item.discount.firstOrNull()?.percent ?: 0.0
            item
        }.toMutableList()

        return invoice
    }

    fun getInvoiceResource(): Flow<Resource<List<InvoiceApiModel>>> {
        return networkResource(shouldMakeNetworkRequest = { true }, makeNetworkRequest = {
            val sharedFlow = MutableSharedFlow<Response<List<InvoiceApiModel>>>(replay = 10)
            var fetchSize = 1000
            while (fetchSize == 1000) {
                val lastUpdated = invoiceDao.getMaxLastUpdated() ?: 0
                val response = invoiceApi.getInvoices(lastUpdated)
                val invoices = response.data
                if (invoices != null) {
                    invoiceDao.updateInvoices(invoices.asDatabaseModel())
                    val itemEntities = invoices.asItemDatabaseModel()
                    if (itemEntities.isNotEmpty()) {
                        invoiceItemDao.updateInvoiceItems(itemEntities)
                    }
                }
                fetchSize = response.data?.size ?: 0
                sharedFlow.emit(response)
            }
            sharedFlow
        }, processNetworkResponse = {}).flowOn(DispatcherProvider.io)
    }

    suspend fun updateInvoices(invoices: List<InvoiceApiModel>) {
        invoiceDao.updateInvoices(invoices.asDatabaseModel())
        val items = invoices.asItemDatabaseModel()
        if (items.isNotEmpty()) {
            invoiceItemDao.updateInvoiceItems(items)
        }
    }

    suspend fun updateInvoice(invoiceApiModel: InvoiceApiModel) {
        invoiceDao.insert(invoiceApiModel.toInvoiceDatabaseModel())
    }

    fun getInvoicePaging(searchText: String): PagingSource<Int, InvoiceEntity> {
        return if (searchText.isBlank()) {
            invoiceDao.getAllInvoicesPagingSource()
        } else {
            invoiceDao.getInvoicesBySearchPagingSource(searchText)
        }
    }
}
