package com.ampairs.invoice.db

import androidx.paging.PagingSource
import androidx.room.Transaction
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
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
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Local-only invoice repository (offline-first, spec 010). Writes go to Room and flag the entity
 * PENDING_PUSH; the [com.ampairs.invoice.sync.InvoiceSyncDelegate] owns all invoice ↔ server traffic
 * (bulk push, batched pull). The list refresh is driven by CentralSyncService (TriggerPull); this
 * repository never touches the network.
 */
@Inject
class InvoiceRepository(
    val invoiceDao: InvoiceDao,
    val invoiceItemDao: InvoiceItemDao,
    val productDataService: ProductDataService,
    val customerDataService: CustomerDataService,
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

    /**
     * Live "number on save" preview for the editor header (spec 010 v2): the next strictly
     * sequential number in [series] formatted exactly as [assignNumber] will assign it.
     */
    suspend fun nextNumberPreview(series: String?): String {
        val s = series?.ifBlank { null } ?: DEFAULT_SERIES
        val seq = (invoiceDao.maxSequenceForSeries(s) ?: 0L) + 1L
        return "$s/" + seq.toString().padStart(4, '0')
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
            // restore unit-of-measure state (spec 010 FR-014) + the price-override flag
            item.unitId = itemEntity.unit_id
            item.baseQuantity = itemEntity.base_quantity
            item.unitMultiplier =
                if (itemEntity.quantity > 0.0 && itemEntity.base_quantity > 0.0) itemEntity.base_quantity / itemEntity.quantity else 1.0
            item.variantSku = itemEntity.variant_sku
            item.priceOverridden =
                kotlin.math.abs(itemEntity.selling_price - itemEntity.product_price * item.unitMultiplier) > 0.005
            item
        }.toMutableList()

        return invoice
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
