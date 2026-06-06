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
import com.ampairs.invoice.domain.asDatabaseModel as invoiceAsEntity
import com.ampairs.invoice.domain.asDomainModelSimple
import com.ampairs.product.data.ProductDataService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Local-only invoice repository (offline-first, spec 010). Writes go to Room and flag the entity
 * PENDING_PUSH; the [com.ampairs.invoice.sync.InvoiceSyncDelegate] owns all invoice ↔ server traffic.
 *
 * NOTE (intermediate): [getInvoiceResource]/[updateInvoices]/[updateInvoice] (legacy list pull,
 * replaced by syncService TriggerPull in A3) still reference [invoiceApi]. The create/edit path no
 * longer touches the network.
 */
@Inject
class InvoiceRepository(
    val invoiceDao: InvoiceDao,
    val invoiceItemDao: InvoiceItemDao,
    val productDataService: ProductDataService,
    val customerDataService: CustomerDataService,
    val invoiceApi: InvoiceApi,
    val syncStateDao: SyncStateDao,
) {
    @Transaction
    suspend fun saveInvoice(invoiceEntity: InvoiceEntity, invoiceItems: List<InvoiceItemEntity>) {
        val numbered = if (invoiceEntity.invoice_number.isBlank()) assignNumber(invoiceEntity) else invoiceEntity
        invoiceDao.insert(numbered.copy(synced = 0))
        invoiceItemDao.insertAll(invoiceItems)
        markPending()
    }

    suspend fun saveInvoice(invoice: Invoice?) {
        val inv = invoice ?: return
        saveInvoice(inv.invoiceAsEntity(), inv.items.invoiceAsEntity(inv.id))
    }

    /**
     * Client-assigned sequential GST invoice number (spec 010 C4/C5): "{series}/{seq padded}".
     * Sequence = max for the series + 1 (per-workspace DB). Series defaults to [DEFAULT_SERIES] until
     * business-settings-driven per-device/branch/FY prefixes land; the backend's
     * UNIQUE(owner, series, sequence_number) is the cross-device backstop.
     */
    private suspend fun assignNumber(entity: InvoiceEntity): InvoiceEntity {
        val series = entity.series.ifBlank { DEFAULT_SERIES }
        val seq = (invoiceDao.maxSequenceForSeries(series) ?: 0L) + 1L
        val number = "$series/" + seq.toString().padStart(4, '0')
        return entity.copy(series = series, sequence_number = seq, invoice_number = number)
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.INVOICE, Clock.System.now().toEpochMilliseconds())

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

    private companion object {
        const val DEFAULT_SERIES = "INV"
    }
}
