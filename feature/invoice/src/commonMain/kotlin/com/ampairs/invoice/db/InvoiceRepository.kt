package com.ampairs.invoice.db

import androidx.paging.PagingSource
import androidx.room3.Transaction
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.invoice.db.model.TaxInfoEntity
import com.ampairs.invoice.db.model.toDomainModel
import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.InvoiceStatus
import com.ampairs.invoice.spi.FinalizedInvoice
import com.ampairs.invoice.spi.InvoiceLifecycleListener
import com.ampairs.invoice.domain.asDatabaseModel as invoiceAsEntity
import com.ampairs.invoice.domain.asDomainModelSimple
import com.ampairs.product.data.ProductDataService
import com.ampairs.sequence.domain.SequenceNumberProvider
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
    val sequenceNumberProvider: SequenceNumberProvider,
    // Downstream reactions to a LOCAL invoice save (e.g. payment posts the receivable). Contributed
    // via Metro @ContributesIntoSet(WorkspaceScope) — the payment module always provides one.
    private val lifecycleListeners: Set<InvoiceLifecycleListener>,
) {
    suspend fun saveInvoice(invoiceEntity: InvoiceEntity, invoiceItems: List<InvoiceItemEntity>) {
        val previous = invoiceDao.selectById(invoiceEntity.id)
        val numbered = if (invoiceEntity.invoice_number.isBlank()) assignNumber(invoiceEntity) else invoiceEntity
        persist(numbered, invoiceItems)
        markPending()
        // Fire AFTER the DB write completes (and outside the Room transaction) — the listeners write
        // to a different database (the party ledger). Only local saves reach here; sync-pulled writes
        // go straight to the DAO via the sync delegate, so this never causes sync churn.
        notifyFinalizationChange(previous?.status, previous?.total_cost, numbered)
    }

    @Transaction
    private suspend fun persist(invoiceEntity: InvoiceEntity, invoiceItems: List<InvoiceItemEntity>) {
        invoiceDao.insert(invoiceEntity.copy(synced = 0))
        // Lines the user removed while billing are no longer in [invoiceItems]. Soft-delete the ones
        // that still exist in Room (active = 0) and keep them so the deletion rides along on the next
        // invoice /sync push (in-band delete); the backend then propagates it to every device.
        val currentIds = invoiceItems.map { it.id }.toSet()
        val removed = invoiceItemDao.getAllInvoiceItemsRaw(invoiceEntity.id)
            .filter { it.id !in currentIds && it.active == 1L }
            .map { it.copy(active = 0, soft_deleted = 1) }
        invoiceItemDao.insertAll(invoiceItems + removed)
    }

    /**
     * Bridge a local billed/un-billed transition — or a billed-amount edit — to downstream listeners
     * (spec 013, R9). A saved invoice counts as billed (post the receivable) unless it's an explicit
     * DRAFT — the app saves invoices as NEW, so gating on INVOICEED would never fire. Re-posting is
     * idempotent (deterministic LDG_<uid>), so an amount change refreshes the receivable.
     */
    private suspend fun notifyFinalizationChange(previousStatus: String?, previousTotal: Double?, entity: InvoiceEntity) {
        if (lifecycleListeners.isEmpty()) return
        fun billed(status: String?) = !status.isNullOrBlank() && !status.equals(InvoiceStatus.DRAFT.name, ignoreCase = true)
        val nowBilled = billed(entity.status)
        val wasBilled = billed(previousStatus)
        val totalChanged = previousTotal == null || previousTotal != entity.total_cost
        when {
            nowBilled && (!wasBilled || totalChanged) -> {
                if (entity.customer_id.isBlank()) return
                val info = FinalizedInvoice(
                    uid = entity.id,
                    partyUid = entity.customer_id,
                    invoiceNumber = entity.invoice_number,
                    invoiceDate = entity.invoice_date,
                    totalCost = entity.total_cost,
                )
                lifecycleListeners.forEach { it.onFinalized(info) }
            }
            wasBilled && !nowBilled ->
                lifecycleListeners.forEach { it.onReverted(entity.id) }
        }
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
        // Sequence-module number first: issued from this device's server-granted block for
        // entity type "invoice" (Settings → Document Numbering), conflict-free across devices.
        val issued = sequenceNumberProvider.next("invoice")
        if (!issued.provisional) {
            return entity.copy(
                series = issued.prefix ?: entity.series.ifBlank { DEFAULT_SERIES },
                sequence_number = issued.value,
                invoice_number = issued.formatted,
            )
        }
        // Offline with no block capacity: legacy local series counter (pre-sequence behavior;
        // backend UNIQUE(owner, series, sequence_number) remains the cross-device backstop).
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
        sequenceNumberProvider.peek("invoice")?.let { return it.formatted }
        val s = series?.ifBlank { null } ?: DEFAULT_SERIES
        val seq = (invoiceDao.maxSequenceForSeries(s) ?: 0L) + 1L
        return "$s/" + seq.toString().padStart(4, '0')
    }

    suspend fun getInvoice(id: String): Invoice {
        val entity = invoiceDao.selectById(id) ?: return Invoice()
        val itemEntities = invoiceItemDao.getInvoiceItems(id)

        val invoice = entity.asDomainModelSimple()
        // Re-attach the buyer from the catalog (keeps the snapshot when the row is gone).
        invoice.customer = invoice.customer?.uid?.takeIf { it.isNotBlank() }?.let { customerDataService.getById(it) }
            ?: invoice.customer

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
            // restore the stored per-line GST breakdown — drives the tax-invoice columns/summary
            item.taxInfos = itemEntity.tax_info?.takeIf { it.isNotBlank() }
                ?.let { Json.decodeFromString<List<TaxInfoEntity>>(it).toDomainModel() }
                ?: item.taxInfos
            // restore unit-of-measure state (spec 010 FR-014) + the price-override flag
            item.unitId = itemEntity.unit_id
            item.baseQuantity = itemEntity.base_quantity
            item.unitMultiplier =
                if (itemEntity.quantity > 0.0 && itemEntity.base_quantity > 0.0) itemEntity.base_quantity / itemEntity.quantity else 1.0
            item.variantSku = itemEntity.variant_sku
            // restore the spec 009 pricing snapshot so it survives an edit round-trip and re-push
            item.resolvedUnitPriceMinor = itemEntity.resolved_unit_price_minor
            item.currency = itemEntity.currency
            item.priceSource = itemEntity.price_source
            item.matchedPriceListUid = itemEntity.matched_price_list_uid
            item.appliedTierMinQty = itemEntity.applied_tier_min_qty
            item.belowMoq = itemEntity.below_moq == 1
            item.priceOverridden =
                kotlin.math.abs(itemEntity.selling_price - itemEntity.product_price * item.unitMultiplier) > 0.005
            item
        }.toMutableList()

        // Restore taxSpec from the persisted tax data (authoritative for re-opened documents).
        (invoice.taxInfos?.firstOrNull() ?: invoice.items.firstOrNull()?.taxInfos?.firstOrNull())
            ?.let { invoice.taxSpec = it.taxSpec }
        return invoice
    }

    /** Whether the row has reached the server (drives the invoice view's sync chip). */
    suspend fun isInvoiceSynced(id: String): Boolean = invoiceDao.selectById(id)?.synced == 1L

    /** Lightweight number lookup for cross-links (e.g. the order view's linked-invoice chip). */
    suspend fun getInvoiceNumber(id: String): String? =
        invoiceDao.selectById(id)?.invoice_number?.ifBlank { null }

    /** Invoice list v2: search (number/buyer/seller) + status / from-order / offline filters. */
    fun getInvoicesFiltered(
        searchText: String,
        status: String,
        fromOrderOnly: Boolean,
        unsyncedOnly: Boolean,
    ): PagingSource<Int, InvoiceEntity> = invoiceDao.getInvoicesFilteredPagingSource(
        searchText = searchText.trim(),
        status = status,
        fromOrderOnly = if (fromOrderOnly) 1 else 0,
        unsyncedOnly = if (unsyncedOnly) 1 else 0,
    )

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
