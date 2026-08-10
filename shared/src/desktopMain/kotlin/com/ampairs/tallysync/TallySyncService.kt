package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.InventoryItemEntity
import com.ampairs.inventory.data.db.InventoryTransactionDao
import com.ampairs.inventory.data.db.InventoryTransactionEntity
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.PriceListEntity
import com.ampairs.pricing.data.db.entity.PriceListItemEntity
import com.ampairs.pricing.domain.model.PriceListStatus
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.dao.ProductStandardCostDao
import com.ampairs.product.db.entity.ProductStandardCostEntity
import com.ampairs.invoice.db.dao.InvoiceDao
import com.ampairs.invoice.db.dao.InvoiceItemDao
import com.ampairs.payment.data.repository.PartyBalanceRepository
import com.ampairs.payment.data.repository.PaymentLedgerPoster
import com.ampairs.payment.data.repository.PaymentVoucherRepository
import com.ampairs.payment.domain.AllocationTarget
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.payment.domain.Money
import com.ampairs.payment.domain.PurchaseLedgerPoster
import com.ampairs.purchase.db.dao.PurchaseDao
import com.ampairs.supplier.data.db.SupplierDao
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.model.master.Group
import com.ampairs.tallysync.TallyCustomerMapper.ENTITY_ACCOUNT_GROUP
import com.ampairs.tallysync.TallyCustomerMapper.ENTITY_LEDGER
import com.ampairs.tallysync.TallyCustomerMapper.ENTITY_SUPPLIER_LEDGER
import com.ampairs.tallysync.TallyCustomerMapper.LedgerPartyType
import com.ampairs.tallysync.TallyCustomerMapper.buildGroupParentIndex
import com.ampairs.tallysync.TallyCustomerMapper.ledgerPartyType
import com.ampairs.tallysync.TallyCustomerMapper.toCustomerEntity
import com.ampairs.tallysync.TallyCustomerMapper.toCustomerGroupEntity
import com.ampairs.tallysync.TallyCustomerMapper.toSupplierEntity
import com.ampairs.tallysync.TallyVoucherMapper.toMappedPurchase
import com.ampairs.tallysync.TallyInventoryMapper.ENTITY_STOCK_BALANCE
import com.ampairs.tallysync.TallyInventoryMapper.inventoryItemId
import com.ampairs.tallysync.TallyInventoryMapper.openingMovement
import com.ampairs.tallysync.TallyInventoryMapper.toInventoryItemEntity
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_CATEGORY
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_GROUP
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_ITEM
import com.ampairs.tallysync.TallyProductMapper.ENTITY_UNIT
import com.ampairs.tallysync.TallyProductMapper.TALLY_PRICE_LIST_ID
import com.ampairs.tallysync.TallyProductMapper.TALLY_PRICE_LIST_NAME
import com.ampairs.tallysync.TallyProductMapper.parseClosingQty
import com.ampairs.tallysync.TallyProductMapper.toCategoryEntity
import com.ampairs.tallysync.TallyProductMapper.toGroupEntity
import com.ampairs.tallysync.TallyProductMapper.toPriceListItemEntities
import com.ampairs.tallysync.TallyProductMapper.toProductEntity
import com.ampairs.tallysync.TallyProductMapper.toStandardCostEntities
import com.ampairs.tallysync.TallyProductMapper.toUnitConversionEntity
import com.ampairs.tallysync.TallyProductMapper.toUnitEntity
import com.ampairs.tallysync.TallyProductMapper.extractHsnCode
import com.ampairs.tallysync.TallyVoucherMapper.ENTITY_VOUCHER
import com.ampairs.tallysync.TallyVoucherMapper.Kind
import com.ampairs.tallysync.TallyVoucherMapper.classify
import com.ampairs.tallysync.TallyVoucherMapper.invoiceId
import com.ampairs.tallysync.TallyVoucherMapper.isInvoiceKind
import com.ampairs.tallysync.TallyVoucherMapper.isPaymentKind
import com.ampairs.tallysync.TallyVoucherMapper.resolvePartyName
import com.ampairs.tallysync.TallyVoucherMapper.toMappedAdjustment
import com.ampairs.tallysync.TallyVoucherMapper.toMappedInvoice
import com.ampairs.tallysync.TallyVoucherMapper.toMappedPayment
import com.ampairs.tally.model.master.StockItem
import com.ampairs.tally.model.voucher.Voucher
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.data.db.dao.UnitDao
import com.ampairs.unit.data.db.entity.UnitConversionEntity
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Clock

data class TallySyncResult(
    val groupsSynced: Int = 0,
    val categoriesSynced: Int = 0,
    val productsSynced: Int = 0,
    val pricesSynced: Int = 0,
    val standardCostsSynced: Int = 0,
    val unitConversionsSynced: Int = 0,
    val unitsSynced: Int = 0,
    val stockBalancesUpdated: Int = 0,
    val inventoryItemsSynced: Int = 0,
    val customerGroupsSynced: Int = 0,
    val customersSynced: Int = 0,
    val suppliersSynced: Int = 0,
    val invoicesSynced: Int = 0,
    val purchasesSynced: Int = 0,
    val paymentsSynced: Int = 0,
    val taxCodesFound: Int = 0,
    val taxCodesToImport: Int = 0,
    val error: String? = null,
) {
    val totalSynced get() = groupsSynced + categoriesSynced + productsSynced + pricesSynced + standardCostsSynced + unitConversionsSynced + unitsSynced + stockBalancesUpdated + inventoryItemsSynced + customerGroupsSynced + customersSynced + suppliersSynced + invoicesSynced + purchasesSynced + paymentsSynced
    val success get() = error == null
}

/**
 * A distinct HSN/SAC code discovered on Tally stock items, with whether the workspace already has a
 * matching tax code subscribed locally. Surfaces tax codes that still need to be imported.
 */
data class TallyTaxCodeCandidate(
    val hsnCode: String,
    val productCount: Int,
    val alreadySubscribed: Boolean,
)

/** Outcome of importing detected HSN codes into the workspace tax codes. */
data class TallyTaxImportResult(
    val matched: Int = 0,       // HSNs that matched a master tax code
    val subscribed: Int = 0,    // workspace tax codes successfully subscribed
    val unmatched: Int = 0,     // HSNs with no master in the catalogue
    val error: String? = null,
) {
    val success get() = error == null
}

private val log = Logger.withTag("TallySyncService")
private const val BATCH_SIZE = 100
private const val MAX_LOG_LINES = 2000

@Inject
class TallySyncService(
    private val engine: HttpClientEngine,
    private val groupDao: GroupDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val productStandardCostDao: ProductStandardCostDao,
    private val priceListDao: PriceListDao,
    private val priceListItemDao: PriceListItemDao,
    private val unitDao: UnitDao,
    private val unitConversionDao: UnitConversionDao,
    private val inventoryItemDao: InventoryItemDao,
    private val inventoryTransactionDao: InventoryTransactionDao,
    private val customerDao: CustomerDao,
    private val customerGroupDao: CustomerGroupDao,
    private val supplierDao: SupplierDao,
    private val purchaseDao: PurchaseDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val ledgerPoster: PaymentLedgerPoster,
    private val purchaseLedgerPoster: PurchaseLedgerPoster,
    private val partyBalanceRepository: PartyBalanceRepository,
    private val paymentVoucherRepository: PaymentVoucherRepository,
    private val taxCodeRepository: TaxCodeRepository,
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
    private val taxConfigurationRepository: TaxConfigurationRepository,
    private val dataStore: AppPreferencesDataStore,
) {
    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    /** Reactive, human-readable log of recent sync activity (capped to the last [MAX_LOG_LINES] lines). */
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    private val _taxCodeCandidates = MutableStateFlow<List<TallyTaxCodeCandidate>>(emptyList())
    /** Distinct HSN/SAC codes found on Tally stock items, flagged with local subscription status. */
    val taxCodeCandidates: StateFlow<List<TallyTaxCodeCandidate>> = _taxCodeCandidates.asStateFlow()

    fun clearLog() {
        _logLines.value = emptyList()
    }

    /** Appends a line to the shared Tally log panel (used by the invoice push service). */
    fun appendLog(line: String) = emit(line)

    private fun emit(line: String) {
        log.i { line }
        _logLines.update { (it + "${timestamp()} $line").takeLast(MAX_LOG_LINES) }
    }

    private fun timestamp(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hh = now.hour.toString().padStart(2, '0')
        val mm = now.minute.toString().padStart(2, '0')
        val ss = now.second.toString().padStart(2, '0')
        return "$hh:$mm:$ss"
    }

    /** Imports every detected HSN that is not yet subscribed. */
    suspend fun importDetectedTaxCodes(): TallyTaxImportResult =
        importCandidates(_taxCodeCandidates.value.filter { !it.alreadySubscribed })

    /** Imports a single detected HSN code (no-op if already subscribed or unknown). */
    suspend fun importTaxCode(hsnCode: String): TallyTaxImportResult =
        importCandidates(_taxCodeCandidates.value.filter { it.hsnCode == hsnCode && !it.alreadySubscribed })

    /**
     * Matches each pending HSN against the server master tax-code catalogue (exact code match),
     * bulk-subscribes the matches, then pulls tax rules + components so tax calculation can resolve
     * the new codes. The product's tax_code stays the HSN string — resolution is keyed by it.
     */
    private suspend fun importCandidates(pending: List<TallyTaxCodeCandidate>): TallyTaxImportResult {
        if (pending.isEmpty()) {
            emit("Tax import: nothing to import")
            return TallyTaxImportResult()
        }
        val countryCode = resolveCountryCode()
        emit("Tax import: matching ${pending.size} HSN code(s) against master catalogue (country=$countryCode)")

        return try {
            val matchedMasterIds = mutableListOf<String>()
            var unmatched = 0
            for (candidate in pending) {
                val hsn = candidate.hsnCode
                val master = taxCodeRepository
                    .searchMasterTaxCodes(query = hsn, countryCode = countryCode)
                    .getOrNull()
                    ?.firstOrNull { it.code.trim() == hsn }
                if (master != null) {
                    matchedMasterIds += master.id
                } else {
                    unmatched++
                    emit("  no master tax code found for HSN $hsn")
                }
            }

            var subscribed = 0
            if (matchedMasterIds.isNotEmpty()) {
                val sub = taxCodeRepository.bulkSubscribeTaxCodes(matchedMasterIds)
                subscribed = sub.getOrDefault(0)
                if (sub.isFailure) emit("  bulk subscribe failed: ${sub.exceptionOrNull()?.message}")
            }

            if (subscribed > 0) {
                // Pull rules + components so tax calculation can resolve the newly subscribed codes.
                taxRuleRepository.syncTaxRules()
                taxComponentRepository.syncWorkspaceComponents()
            }

            refreshCandidateSubscriptionStatus()
            emit("Tax import complete — matched=${matchedMasterIds.size}, subscribed=$subscribed, no master=$unmatched")
            TallyTaxImportResult(matched = matchedMasterIds.size, subscribed = subscribed, unmatched = unmatched)
        } catch (e: Exception) {
            emit("Tax import FAILED — ${e.message}")
            log.e(e) { "Tax import failed" }
            TallyTaxImportResult(error = e.message ?: "Unknown error")
        }
    }

    private suspend fun refreshCandidateSubscriptionStatus() {
        _taxCodeCandidates.value = _taxCodeCandidates.value.map {
            it.copy(alreadySubscribed = taxCodeRepository.getByCode(it.hsnCode) != null)
        }
    }

    private suspend fun resolveCountryCode(): String =
        taxConfigurationRepository.getConfiguration().getOrNull()?.countryCode?.takeIf { it.isNotBlank() }
            ?: "IN"

    suspend fun sync(workspaceSlug: String): TallySyncResult {
        val host = dataStore.getTallyHost(workspaceSlug).first()
        if (host.isBlank()) {
            emit("Tally host not configured")
            return TallySyncResult(error = "Tally host not configured")
        }
        val port = dataStore.getTallyPort(workspaceSlug).first()
        val baseUrl = "http://$host:$port"
        emit("Tally sync started — $baseUrl")

        val repo = TallyRepository(TallyApiImpl(engine, baseUrl))

        return try {
            val groupsSynced = syncGroups(repo, workspaceSlug)
            emit("Stock groups: $groupsSynced new")
            val categoriesSynced = syncCategories(repo, workspaceSlug)
            emit("Stock categories: $categoriesSynced new")
            val unitsSynced = syncUnits(repo, workspaceSlug)
            emit("Units: $unitsSynced new")
            val groupIdByName = buildGroupNameIndex()
            val categoryIdByName = buildCategoryNameIndex()
            val unitIdByName = buildUnitNameIndex()
            val productCounts = syncProducts(repo, workspaceSlug, groupIdByName, categoryIdByName, unitIdByName)
            emit("Products: ${productCounts.products} new")
            emit("Prices: ${productCounts.prices} effective-dated entries; standard costs: ${productCounts.costs}; unit conversions: ${productCounts.conversions}")
            val stockCounts = syncStockBalances(repo, workspaceSlug)
            emit("Stock balances: ${stockCounts.stockBalancesUpdated} updated; inventory items: ${stockCounts.inventoryItems}")

            // Account groups drive both the customer-group catalogue and the debtor/creditor split
            // used to route ledgers to customers vs suppliers. Fetched once and reused.
            val accountGroups = repo.getGroups().body?.data?.collection?.groups ?: emptyList<Group>()
            val groupParentIndex = buildGroupParentIndex(accountGroups)

            // Fetch vouchers once (reused for purchase-party detection + the voucher sync). A purchase
            // voucher's party is a supplier by definition, so its name feeds syncSuppliers — that picks
            // up real suppliers kept under Sundry Debtors or a custom group, not just Sundry Creditors.
            val vouchers = repo.getVouchers().body?.data?.collection?.voucher ?: emptyList()
            val purchaseParties = vouchers
                .filter { it.classify() == Kind.PURCHASE }
                .mapNotNull { it.resolvePartyName()?.trim()?.takeIf { n -> n.isNotBlank() } }
                .toSet()

            val customerGroupsSynced = syncCustomerGroups(workspaceSlug, accountGroups)
            emit("Customer groups: $customerGroupsSynced upserted")
            val customerGroupIdByName = buildCustomerGroupNameIndex()
            val customersSynced = syncCustomers(repo, workspaceSlug, customerGroupIdByName, groupParentIndex)
            emit("Customers: $customersSynced upserted")
            val suppliersSynced = syncSuppliers(repo, workspaceSlug, groupParentIndex, purchaseParties)
            emit("Suppliers: $suppliersSynced upserted")

            // Clean up any previously stored invalid values (pre-sanitizer data)
            customerDao.nullifyInvalidPhones()
            customerDao.nullifyInvalidPincodes()

            // Vouchers depend on customers/suppliers (party lookup) and products (line-item lookup),
            // so they run last. Sales → invoices (customers); Purchases → purchases (suppliers).
            val (invoicesSynced, paymentsSynced, purchasesSynced) = syncVouchers(workspaceSlug, vouchers)
            emit("Invoices: $invoicesSynced upserted")
            emit("Purchases: $purchasesSynced upserted")
            emit("Payments: $paymentsSynced upserted")

            val candidates = _taxCodeCandidates.value
            val toImport = candidates.count { !it.alreadySubscribed }
            val result = TallySyncResult(
                groupsSynced = groupsSynced,
                categoriesSynced = categoriesSynced,
                productsSynced = productCounts.products,
                pricesSynced = productCounts.prices,
                standardCostsSynced = productCounts.costs,
                unitConversionsSynced = productCounts.conversions,
                unitsSynced = unitsSynced,
                stockBalancesUpdated = stockCounts.stockBalancesUpdated,
                inventoryItemsSynced = stockCounts.inventoryItems,
                customerGroupsSynced = customerGroupsSynced,
                customersSynced = customersSynced,
                suppliersSynced = suppliersSynced,
                invoicesSynced = invoicesSynced,
                purchasesSynced = purchasesSynced,
                paymentsSynced = paymentsSynced,
                taxCodesFound = candidates.size,
                taxCodesToImport = toImport,
            )
            emit("Tally sync complete — total=${result.totalSynced}, tax codes to import=$toImport")
            result
        } catch (e: Exception) {
            emit("Tally sync FAILED — ${e.message}")
            log.e(e) { "Tally sync failed" }
            TallySyncResult(error = e.message ?: "Unknown error")
        }
    }

    private suspend fun syncGroups(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_GROUP).first()
        val stockGroups = repo.getStockGroups().body?.data?.collection?.stockGroups ?: return 0
        val filtered = stockGroups.filter { it.alterId.toAlterLong() > lastAlterId }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            groupDao.getGroupsByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        val entities = filtered.mapNotNull { sg ->
            val id = sg.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("GRP")
            sg.toGroupEntity(id)
        }
        entities.chunked(BATCH_SIZE).forEach { groupDao.insertAll(it) }
        val maxAlterId = stockGroups.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_GROUP, maxAlterId)
        log.d { "syncGroups: ${entities.size} new (batches=${entities.size / BATCH_SIZE + 1})" }
        return entities.size
    }

    private suspend fun syncCategories(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_CATEGORY).first()
        val stockCategories = repo.getStockCategories().body?.data?.collection?.stockCategories ?: return 0
        val filtered = stockCategories.filter { it.alterId.toAlterLong() > lastAlterId }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            categoryDao.getCategoriesByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        val entities = filtered.mapNotNull { sc ->
            val id = sc.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("CAT")
            sc.toCategoryEntity(id)
        }
        entities.chunked(BATCH_SIZE).forEach { categoryDao.insertAll(it) }
        val maxAlterId = stockCategories.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_CATEGORY, maxAlterId)
        log.d { "syncCategories: ${entities.size} new" }
        return entities.size
    }

    private suspend fun syncUnits(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_UNIT).first()
        val units = repo.getUnits().body?.data?.collection?.units
        if (units.isNullOrEmpty()) {
            emit("Units: API returned none")
            return 0
        }
        emit("Units: API returned ${units.size} total, lastAlterId=$lastAlterId")
        // Simple units often have alterId 0/blank — include them (== 0L), like customers/groups.
        val filtered = units.filter { it.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId } }
        if (filtered.isEmpty()) {
            val s = units.first()
            emit("  no new units after filter. sample: name='${s.name}', unitName='${s.unitName}', alterId='${s.alterId}', simple='${s.isSimpleUnit}'")
        }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            unitDao.getUnitsByTallyRefIds(chunk).forEach { e -> e.refId?.let { existingIdByGuid[it] = e.id } }
        }

        val entities = filtered.mapNotNull { u ->
            val guid = u.guid?.takeIf { it.isNotBlank() }
            val name = (u.name ?: u.unitName)?.trim()?.takeIf { it.isNotBlank() }
            // Reuse existing id by GUID, else by name (simple units have no GUID — avoids duplicates).
            val existingId = guid?.let { existingIdByGuid[it] } ?: name?.let { unitDao.getUnitByName(it)?.id }
            val id = existingId ?: UidGenerator.generateUid("UNT")
            u.toUnitEntity(id)
        }
        entities.chunked(BATCH_SIZE).forEach { unitDao.insertUnits(it) }
        val maxAlterId = units.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_UNIT, maxAlterId)
        log.d { "syncUnits: ${entities.size} upserted" }
        return entities.size
    }

    /**
     * Diagnostic only (for now): reports how many stock items carry an alternate/compound unit and
     * dumps a sample so we can confirm the conversion semantics (base ↔ additional, conversion,
     * denominator) before mapping them into product-scoped UnitConversion records.
     */
    private fun scanUnitConversions(stockItems: List<StockItem>) {
        fun hasAlt(si: StockItem): Boolean {
            val au = si.additionalUnits?.trim()
            return !au.isNullOrBlank() && au != "Not Applicable"
        }
        val withAlt = stockItems.count(::hasAlt)
        emit("Unit conversions: $withAlt stock items have an alternate unit")
        if (withAlt > 0) {
            stockItems.firstOrNull(::hasAlt)?.let { s ->
                emit("  sample '${s.name}': base='${s.baseUnits}', additional='${s.additionalUnits}', conversion='${s.conversion}', denominator='${s.denominator}'")
            }
        }
    }

    /** Counts returned by [syncProducts] — products plus the effective-dated child feeds. */
    private data class ProductSyncCounts(
        val products: Int = 0,
        val prices: Int = 0,
        val costs: Int = 0,
        val conversions: Int = 0,
    )

    private suspend fun syncProducts(
        repo: TallyRepository,
        workspaceSlug: String,
        groupIdByName: Map<String, String>,
        categoryIdByName: Map<String, String>,
        unitIdByName: Map<String, String>,
    ): ProductSyncCounts {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM).first()
        val stockItems = repo.getStockItems().body?.data?.collection?.stockItems ?: return ProductSyncCounts()
        val filtered = stockItems.filter { it.alterId.toAlterLong() > lastAlterId }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            productDao.getProductsByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        // Retain the (stockItem, resolved productId) pairing so the effective-dated child records
        // (price/cost/conversion) reuse the SAME product id we just persisted.
        val pairs = filtered.mapNotNull { si ->
            val id = si.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("PRD")
            si.toProductEntity(id, groupIdByName, categoryIdByName, unitIdByName)?.let { si to it }
        }
        var batchNum = 0
        pairs.map { it.second }.chunked(BATCH_SIZE).forEach { batch ->
            productDao.insertAll(batch)
            batchNum++
            log.d { "syncProducts batch $batchNum: inserted ${batch.size}" }
        }

        // Effective-dated standard price → price list items, standard cost → product_standard_cost,
        // compound unit → product-scoped unit conversion. Built per item using the resolved product id.
        val priceItems = mutableListOf<PriceListItemEntity>()
        val costRows = mutableListOf<ProductStandardCostEntity>()
        val conversions = mutableListOf<UnitConversionEntity>()
        for ((si, product) in pairs) {
            priceItems += si.toPriceListItemEntities(product.id)
            costRows += si.toStandardCostEntities(product.id)
            si.toUnitConversionEntity(product.id, unitIdByName)?.let { conversions += it }
        }
        if (priceItems.isNotEmpty()) {
            ensureTallyPriceListHeader()
            priceItems.chunked(BATCH_SIZE).forEach { priceListItemDao.insertItems(it) }
        }
        costRows.chunked(BATCH_SIZE).forEach { productStandardCostDao.insertAll(it) }
        if (conversions.isNotEmpty()) unitConversionDao.insertUnitConversions(conversions)

        // Scan the full catalogue (not just alterId-filtered items) so the tax-code panel always
        // reflects every HSN in Tally, even when no products are newly changed this cycle.
        scanTaxCodeCandidates(stockItems)
        scanUnitConversions(stockItems)

        val maxAlterId = stockItems.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM, maxAlterId)
        log.d { "syncProducts: ${pairs.size} products, ${priceItems.size} prices, ${costRows.size} costs, ${conversions.size} conversions across $batchNum batches" }
        return ProductSyncCounts(pairs.size, priceItems.size, costRows.size, conversions.size)
    }

    /**
     * Ensures the single workspace-wide "Tally Standard Price" price-list header exists so the
     * effective-dated [PriceListItemEntity] rows have a parent. RETAIL channel, no targeting → it acts
     * as the channel default in the offline price resolver. Deterministic id → idempotent (REPLACE).
     */
    private suspend fun ensureTallyPriceListHeader() {
        if (priceListDao.getPriceListById(TALLY_PRICE_LIST_ID) != null) return
        priceListDao.insertPriceList(
            PriceListEntity(
                id = TALLY_PRICE_LIST_ID,
                name = TALLY_PRICE_LIST_NAME,
                channel = SalesChannel.RETAIL.name,
                currency = "INR",
                priority = 0,
                status = PriceListStatus.ACTIVE.name,
                active = true,
                synced = false,
            )
        )
    }

    /**
     * Builds the list of distinct HSN/SAC codes present on Tally stock items, marking each with
     * whether the workspace already has a matching tax code subscribed (local DB check only — no
     * network). Surfaced via [taxCodeCandidates] so the UI can show which tax codes still need to be
     * imported (manually or, later, automatically).
     */
    private suspend fun scanTaxCodeCandidates(stockItems: List<StockItem>) {
        val withGst = stockItems.count { !it.gstDetailList.isNullOrEmpty() }
        val withHsn = stockItems.count { !it.hsnDetailList.isNullOrEmpty() }
        emit("Tax scan: ${stockItems.size} stock items — $withGst with GST details, $withHsn with HSN details")

        val hsnCounts = stockItems
            .mapNotNull { it.extractHsnCode() }
            .groupingBy { it }
            .eachCount()
        val candidates = hsnCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { (hsn, count) ->
                TallyTaxCodeCandidate(
                    hsnCode = hsn,
                    productCount = count,
                    alreadySubscribed = taxCodeRepository.getByCode(hsn) != null,
                )
            }
        _taxCodeCandidates.value = candidates
        emit("Tax codes: ${candidates.size} distinct HSN found, ${candidates.count { !it.alreadySubscribed }} not yet subscribed")

        // Diagnostics: if nothing was detected, dump a sample item's raw GST/HSN fields so we can
        // see exactly what Tally returned and adjust extraction/request accordingly.
        if (candidates.isEmpty() && stockItems.isNotEmpty()) {
            val sample = stockItems.firstOrNull { !it.gstDetailList.isNullOrEmpty() || !it.hsnDetailList.isNullOrEmpty() }
                ?: stockItems.first()
            emit("Tax scan: no HSN detected. Sample '${sample.name}' gstApplicable=${sample.gstApplicable}")
            sample.gstDetailList?.firstOrNull()?.let {
                emit("  GST detail → hsnCode='${it.hsnCode}', hsnMasterName='${it.hsnMasterName}', taxability='${it.taxability}'")
            }
            sample.hsnDetailList?.firstOrNull()?.let {
                emit("  HSN detail → hsnCode='${it.hsnCode}', hsnMasterName='${it.hsnMasterName}'")
            }
        }
    }

    /** Counts returned by [syncStockBalances] — product stock updates plus inventory rows. */
    private data class StockSyncCounts(
        val stockBalancesUpdated: Int = 0,
        val inventoryItems: Int = 0,
    )

    /**
     * Updates each product's on-hand from the Tally closing balance, and mirrors it into the inventory
     * module: one [InventoryItemEntity] per product with stock plus a single deterministic opening
     * [InventoryTransactionEntity] (STOCK_IN / OPENING). Both are id'd from the Tally GUID so re-sync
     * upserts in place (no duplicates / no accumulation).
     *
     * Incremental via the item's own [ENTITY_STOCK_BALANCE] alterId checkpoint (kept separate from
     * [ENTITY_STOCK_ITEM] so this filter doesn't race with the stock-item master sync). Note this
     * tracks changes to the stock item object itself; CLOSINGBALANCE is a live aggregate over that
     * item's voucher postings, so a balance can still move without the item's ALTERID advancing.
     */
    private suspend fun syncStockBalances(repo: TallyRepository, workspaceSlug: String): StockSyncCounts {
        val items = repo.getStockBalances().body?.data?.collection?.stockItems ?: return StockSyncCounts()
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_BALANCE).first()
        val filtered = items.filter { it.alterId.toAlterLong() > lastAlterId }
        // guid → product (for inventory metadata); products carry the Tally GUID in ref_id.
        val productByGuid = productDao.getProducts().mapNotNull { p -> p.ref_id?.let { it to p } }.toMap()

        var updated = 0
        val invItems = mutableListOf<InventoryItemEntity>()
        val invMovements = mutableListOf<InventoryTransactionEntity>()
        filtered.forEach { item ->
            val qty = item.closingBalance?.parseClosingQty() ?: return@forEach
            val guid = item.guid?.takeIf { it.isNotBlank() } ?: return@forEach
            productDao.updateStockQuantityByTallyRef(guid, qty)
            updated++

            val product = productByGuid[guid] ?: return@forEach
            val itemId = inventoryItemId(guid)
            invItems += product.toInventoryItemEntity(itemId, qty)
            invMovements += openingMovement(guid, itemId, qty, product.dp)
        }
        invItems.chunked(BATCH_SIZE).forEach { inventoryItemDao.insertItems(it) }
        invMovements.chunked(BATCH_SIZE).forEach { inventoryTransactionDao.insertMovements(it) }

        val maxAlterId = items.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_BALANCE, maxAlterId)

        log.d { "syncStockBalances: $updated stock updates, ${invItems.size} inventory items" }
        return StockSyncCounts(updated, invItems.size)
    }

    private suspend fun syncCustomerGroups(workspaceSlug: String, groups: List<Group>): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_ACCOUNT_GROUP).first()
        log.d { "syncCustomerGroups: API returned ${groups.size} total, lastAlterId=$lastAlterId" }
        val filtered = groups.filter { it.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId } }

        val names = filtered.mapNotNull { it.name?.takeIf { it.isNotBlank() } }
        val existingIdByName = mutableMapOf<String, String>()
        for (chunk in names.chunked(BATCH_SIZE)) {
            customerGroupDao.getCustomerGroupsByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByName[it] = e.id } }
        }

        val entities = filtered.mapNotNull { g ->
            val name = g.name?.takeIf { it.isNotBlank() }
            val id = name?.let { existingIdByName[it] } ?: UidGenerator.generateUid("CGP")
            g.toCustomerGroupEntity(id)
        }
        entities.chunked(BATCH_SIZE).forEach { customerGroupDao.insertCustomerGroups(it) }
        if (entities.isNotEmpty()) {
            val maxAlterId = groups.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
            if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_ACCOUNT_GROUP, maxAlterId)
        }
        log.d { "syncCustomerGroups: ${entities.size} upserted" }
        return entities.size
    }

    private suspend fun syncCustomers(
        repo: TallyRepository,
        workspaceSlug: String,
        customerGroupIdByName: Map<String, String>,
        groupParentIndex: Map<String, String>,
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_LEDGER).first()
        val ledgers = repo.getLedgers().body?.data?.collection?.ledgers ?: return 0
        log.d { "syncCustomers: API returned ${ledgers.size} total, lastAlterId=$lastAlterId" }
        val filtered = ledgers
            // Customers are ONLY ledgers whose lineage roots at Sundry Debtors. Non-party ledgers
            // (bank, cash, Duties & Taxes/GST, Profit & Loss) are OTHER and must be skipped — they
            // previously leaked in because the old check defaulted unknown lineages to "customer".
            .filter { ledgerPartyType(it.parent, groupParentIndex) == LedgerPartyType.DEBTOR }
            .filter { it.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId } }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            customerDao.getCustomersByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        val pairs = filtered.mapNotNull { l ->
            val id = l.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("CUS")
            l.toCustomerEntity(id, customerGroupIdByName)?.let { l to it }
        }
        val entities = pairs.map { it.second }
        entities.chunked(BATCH_SIZE).forEach { customerDao.insertCustomers(it) }
        // Opening balance per party (Dr = receivable, Cr = payable) → PartyBalance, recomputed locally.
        for ((ledger, entity) in pairs) syncOpeningBalance(entity.id, ledger.openingBalance)
        if (entities.isNotEmpty()) {
            val maxAlterId = ledgers.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
            if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_LEDGER, maxAlterId)
        }
        log.d { "syncCustomers: ${entities.size} upserted" }
        return entities.size
    }

    /**
     * Syncs Tally creditor ledgers (Sundry Creditors lineage) into suppliers — the buy-side mirror of
     * [syncCustomers]. Keeps its own [ENTITY_SUPPLIER_LEDGER] alterId checkpoint so it stays
     * independent of the customer ledger checkpoint (both read the same getLedgers() collection).
     */
    private suspend fun syncSuppliers(
        repo: TallyRepository,
        workspaceSlug: String,
        groupParentIndex: Map<String, String>,
        purchaseParties: Set<String>,
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_SUPPLIER_LEDGER).first()
        val ledgers = repo.getLedgers().body?.data?.collection?.ledgers ?: return 0
        val filtered = ledgers.filter { l ->
            // A supplier is any ledger under Sundry Creditors (new/changed since the checkpoint) OR any
            // ledger that is the party on a purchase voucher — the latter catches real suppliers kept
            // under Sundry Debtors or a custom group. Purchase parties are always included (idempotent
            // upsert by GUID) so they're created even when the ledger itself is unchanged.
            val name = l.name?.trim()
            val type = ledgerPartyType(l.parent, groupParentIndex)
            // A purchase party becomes a supplier UNLESS it's a debtor: a debtor is already synced as a
            // CUSTOMER, and its purchases post onto that same customer record — so the payment module
            // shows ONE netted party, not a customer + supplier duplicate.
            val isPurchaseParty = name != null && name in purchaseParties && type != LedgerPartyType.DEBTOR
            val isNewCreditor = type == LedgerPartyType.CREDITOR &&
                l.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId }
            isPurchaseParty || isNewCreditor
        }
        log.d { "syncSuppliers: ${ledgers.size} ledgers, ${filtered.size} suppliers after filter (${purchaseParties.size} purchase parties), lastAlterId=$lastAlterId" }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            supplierDao.getSuppliersByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        val pairs = filtered.mapNotNull { l ->
            val id = l.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("SUP")
            l.toSupplierEntity(id)?.let { l to it }
        }
        val entities = pairs.map { it.second }
        entities.chunked(BATCH_SIZE).forEach { supplierDao.insertSuppliers(it) }
        // Opening balance per supplier (Cr = payable "To Pay", Dr = advance) → PartyBalance. Safe for
        // all of them: the filter above excludes debtors, so no supplier here is also a customer — there
        // is no party whose opening could be double-counted across a customer + supplier record.
        for ((ledger, entity) in pairs) syncOpeningBalance(entity.id, ledger.openingBalance)
        if (entities.isNotEmpty()) {
            val maxAlterId = ledgers.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
            if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_SUPPLIER_LEDGER, maxAlterId)
        }
        log.d { "syncSuppliers: ${entities.size} upserted" }
        return entities.size
    }

    /**
     * Sets a party's (customer or supplier) opening balance from its Tally ledger opening figure.
     * Tally's XML LEDGER.OPENINGBALANCE (and CLOSINGBALANCE) sign convention is the reverse of
     * ordinary accounting intuition: negative = Debit (receivable → [Direction.DR]), positive =
     * Credit (payable → [Direction.CR]) — a well-known Tally export quirk, distinct from voucher
     * ledger-entry amounts. No-op when zero/blank. Idempotent per party (the repository reuses the
     * existing PartyBalance row by party), recomputes the closing balance, and flags it for push.
     */
    private suspend fun syncOpeningBalance(partyUid: String, openingBalanceRaw: String?) {
        val amount = openingBalanceRaw.parseTallyAmount()
        if (amount == 0.0) return
        partyBalanceRepository.setOpeningBalance(
            partyUid = partyUid,
            uid = UidGenerator.generateUid("PBL"),
            openingBalance = Money.fromDouble(abs(amount)),
            openingDirection = if (amount < 0.0) Direction.DR else Direction.CR,
            openingAsOf = "",
        ).onFailure { log.w { "Opening balance for $partyUid failed: ${it.message}" } }
    }

    /** Tally amount strings: "-15000.00", " 1250.50 " — keep digits, sign and decimal point. */
    private fun String?.parseTallyAmount(): Double {
        val cleaned = this?.trim()?.filter { it.isDigit() || it == '.' || it == '-' } ?: return 0.0
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /**
     * Syncs Tally vouchers into invoices + payments. Sales/Purchase/Credit-Note/Debit-Note become
     * [com.ampairs.invoice.db.entity.InvoiceEntity] (with line items and a ledger posting via
     * [ledgerPoster]); Receipt/Payment become payment vouchers (via [paymentVoucherRepository], which
     * posts the money-movement ledger entry and recomputes the party balance). Invoices are processed
     * first so payments can match their bill references to the just-synced invoice ids.
     *
     * Purchase vouchers become [com.ampairs.purchase.db.entity.PurchaseEntity] (status RECEIVED) keyed
     * to a synced supplier, with a buy-side PURCHASE_BILL ledger posting that surfaces the supplier
     * payable ("To Pay") via [purchaseLedgerPoster].
     *
     * @return Triple(invoicesSynced, paymentsSynced, purchasesSynced)
     */
    private suspend fun syncVouchers(
        workspaceSlug: String,
        vouchers: List<Voucher>,
    ): Triple<Int, Int, Int> {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_VOUCHER).first()
        emit("Vouchers: ${vouchers.size} total, lastAlterId=$lastAlterId")
        val filtered = vouchers.filter { it.alterId.toAlterLong() > lastAlterId }

        // Diagnostic: classify() only recognizes sales/purchase/credit-debit-note/receipt/payment
        // keywords — anything else (e.g. a Tally "Journal" voucher used for discounts/promotions/
        // write-offs) falls to Kind.OTHER and is dropped below without posting any ledger entry.
        // Log a breakdown so an unhandled voucher-type name can be identified from the sync log.
        val kindCounts = filtered.groupingBy { it.classify() }.eachCount()
        emit("Vouchers by kind: " + Kind.entries.joinToString(", ") { k -> "${k.name}=${kindCounts[k] ?: 0}" })
        val otherByType = filtered.filter { it.classify() == Kind.OTHER }
            .groupingBy { (it.voucherTypeName ?: it.vchType ?: "<blank>").trim() }
            .eachCount()
        if (otherByType.isNotEmpty()) {
            emit(
                "Excluded (OTHER, not synced) voucher types: " +
                    otherByType.entries.sortedByDescending { it.value }.joinToString(", ") { (name, count) -> "\"$name\"=$count" },
            )
        }

        // Sales party matches a synced customer; purchase party matches a synced supplier (both were
        // synced earlier this cycle — purchase parties are now always synced as suppliers). Line items
        // match by stock-item name to a synced product.
        val customerIdByName = customerDao.getAllCustomers().first().associate { it.name.trim() to it.id }
        val supplierIdByName = supplierDao.getAllSuppliers().first().associate { it.name.trim() to it.id }
        val products = productDao.getProducts()
        val productIdByName = products.associate { it.name.trim() to it.id }
        // name → tax_code (HSN) so invoice/purchase line items carry the product's tax code.
        val productTaxByName = products.associate { it.name.trim() to it.tax_code }

        // --- Invoices first (payments reference them by bill number) ---
        // Vouchers WE pushed into Tally (order → invoice) carry REMOTEID = the local invoice id.
        // Skip re-importing them: the local invoice is the authoritative copy (app-assigned number,
        // full line detail) and re-mapping would either duplicate it as INVTLY<guid> or overwrite the
        // richer local row with the Tally-derived one.
        val pushedInvoiceIds = dataStore.getTallyPushedInvoiceIds(workspaceSlug).first()
        var invoicesSynced = 0
        var skippedInvoiceNoParty = 0
        var skippedPushedBack = 0
        for (voucher in filtered) {
            val kind = voucher.classify()
            // PURCHASE is the buy-side and is handled separately (→ purchases + supplier payable).
            if (!kind.isInvoiceKind || kind == Kind.PURCHASE) continue
            val remoteId = voucher.remoteId?.trim()
            if (!remoteId.isNullOrBlank() && remoteId in pushedInvoiceIds) {
                skippedPushedBack++
                continue
            }
            val partyName = voucher.resolvePartyName()
            // Unified party id (customer wins, else supplier). A party is either a customer or a supplier
            // record (never both), so a sale to a supplier-party nets onto that ONE record instead of
            // being dropped — the payment module shows a single ledger for the party, not two.
            val customerId = partyName?.let { customerIdByName[it] ?: supplierIdByName[it] }
            if (customerId == null) {
                skippedInvoiceNoParty++
                continue
            }
            val mapped = voucher.toMappedInvoice(kind, customerId, partyName, productIdByName, productTaxByName) ?: continue
            invoiceDao.insert(mapped.entity)
            invoiceItemDao.replaceInvoiceItems(mapped.entity.id, mapped.items)
            // Deterministic LDG_<id> entry — re-posting on a changed voucher upserts in place.
            ledgerPoster.postDocumentEntry(
                partyUid = customerId,
                sourceType = mapped.sourceType,
                sourceUid = mapped.entity.id,
                entryType = mapped.entryType,
                direction = mapped.entryType.naturalDirection,
                amount = Money.fromDouble(mapped.entity.total_cost),
                entryDate = mapped.entity.invoice_date,
                voucherNo = mapped.entity.invoice_number,
            )
            invoicesSynced++
        }
        if (skippedInvoiceNoParty > 0) emit("Invoices: skipped $skippedInvoiceNoParty (party not a known customer)")
        if (skippedPushedBack > 0) emit("Invoices: skipped $skippedPushedBack (pushed from this app — kept local copy)")

        // --- Purchases (party = supplier; posts the buy-side PURCHASE_BILL payable → "To Pay") ---
        var purchasesSynced = 0
        val skippedPurchaseParties = mutableListOf<String>()
        for (voucher in filtered) {
            if (voucher.classify() != Kind.PURCHASE) continue
            // An earlier app version (before the Purchase module) mapped purchase vouchers as INVOICES.
            // That stale invoice (deterministic INVTLY<guid>) + its SALES_INVOICE ledger entry still
            // exist and show as a duplicate on the party. Remove them so only the PURCHASE_BILL remains.
            voucher.invoiceId()?.let { staleInvoiceId ->
                val stale = invoiceDao.selectById(staleInvoiceId)
                if (stale != null) {
                    invoiceDao.insert(stale.copy(soft_deleted = 1, synced = 0)) // syncs the deletion
                    invoiceItemDao.softDeleteByInvoiceId(staleInvoiceId)
                    ledgerPoster.reverseDocumentEntry(staleInvoiceId) // drop SALES_INVOICE + recompute party
                }
            }
            val partyName = voucher.resolvePartyName()
            // Unified party id (customer wins, else supplier) — the SAME rule as the invoice loop, so a
            // party's sales and purchases always net onto one record. A debtor you also buy from keeps a
            // single customer balance; a supplier-party keeps a single supplier balance. The payable + its
            // backfill both post to this id.
            val supplierId = partyName?.let { customerIdByName[it] ?: supplierIdByName[it] }
            if (supplierId == null) {
                skippedPurchaseParties += (partyName ?: "<no party>")
                continue
            }
            val mapped = voucher.toMappedPurchase(supplierId, partyName, productIdByName, productTaxByName) ?: continue
            purchaseDao.savePurchaseWithItems(mapped.entity, mapped.items)
            // Deterministic LDG_<purchase.id> CR entry — re-posting on a changed voucher upserts in place.
            purchaseLedgerPoster.postForReceivedPurchase(
                purchaseUid = mapped.entity.id,
                partyUid = supplierId,
                purchaseNumber = mapped.entity.purchase_number,
                purchaseDate = mapped.entity.purchase_date,
                totalCost = mapped.entity.total_cost,
                status = mapped.entity.status,
            )
            purchasesSynced++
        }
        if (skippedPurchaseParties.isNotEmpty()) {
            emit("Purchases: skipped ${skippedPurchaseParties.size} (party not a known supplier): ${skippedPurchaseParties.distinct().joinToString(", ")}")
        }

        // --- Payments / receipts (ONLY for customer or supplier parties) ---
        // Bill refs resolve against invoices (customer receipts) or purchases (supplier payments).
        // Receipts/payments against non-party ledgers — bank contras, tax adjustments, P&L/expense
        // payments like rent or salary — have no customer/supplier party and are skipped here.
        val invoiceIdByNumber = invoiceDao.selectAll().associate { it.invoice_number to it.id }
        val purchaseIdByNumber = purchaseDao.selectAll().associate { it.purchase_number to it.id }
        // Vouchers WE pushed into Tally (collection → Receipt, see TallyPaymentPushService) carry
        // REMOTEID = the local payment voucher uid. Skip re-importing them for the same reason
        // invoices are skipped above: toMappedPayment derives a deterministic PVCHTLY<guid> id from
        // the Tally GUID, which is NOT our local uid, so without this guard every push would come
        // back as a duplicate payment voucher on the very next pull.
        val pushedPaymentIds = dataStore.getTallyPushedPaymentIds(workspaceSlug).first()
        var paymentsSynced = 0
        var skippedPaymentNoParty = 0
        var skippedPaymentPushedBack = 0
        for (voucher in filtered) {
            val kind = voucher.classify()
            if (!kind.isPaymentKind) continue
            val remoteId = voucher.remoteId?.trim()
            if (!remoteId.isNullOrBlank() && remoteId in pushedPaymentIds) {
                skippedPaymentPushedBack++
                continue
            }
            val partyName = voucher.resolvePartyName()
            val customerId = partyName?.let { customerIdByName[it] }
            val supplierId = partyName?.let { supplierIdByName[it] }
            if (partyName == null || (customerId == null && supplierId == null)) {
                skippedPaymentNoParty++
                continue
            }
            // One netted balance per party: a dual-role party's purchases post onto its CUSTOMER record,
            // so route its receipts AND payments there too (customer id wins). Receipt (money IN) settles
            // a receivable → invoices; Payment (money OUT) settles a payable → purchases.
            val partyUid = (customerId ?: supplierId)!! // guard above guarantees one is non-null
            val isPaymentOut = kind == Kind.PAYMENT
            val targetType = if (isPaymentOut) AllocationTarget.PURCHASE else AllocationTarget.INVOICE
            val targetIdByRef = if (isPaymentOut) purchaseIdByNumber else invoiceIdByNumber
            val mapped = voucher.toMappedPayment(kind, partyUid, partyName, targetType, targetIdByRef) ?: continue
            // The repository rejects allocations whose sum exceeds the voucher total — drop them
            // defensively (rounding/sign quirks) rather than losing the whole receipt.
            val allocatedSum = mapped.allocations.fold(Money.ZERO) { acc, a -> acc + a.amount }
            val allocations = if (allocatedSum > mapped.voucher.totalAmount) {
                emit("  payment ${mapped.voucher.voucherNo}: allocations exceed total — recording without allocations")
                emptyList()
            } else {
                mapped.allocations
            }
            paymentVoucherRepository.save(mapped.voucher, allocations)
                .onSuccess { paymentsSynced++ }
                .onFailure { emit("  payment ${mapped.voucher.voucherNo} failed: ${it.message}") }
        }
        if (skippedPaymentNoParty > 0) emit("Payments: skipped $skippedPaymentNoParty (party not a known customer/supplier)")
        if (skippedPaymentPushedBack > 0) emit("Payments: skipped $skippedPaymentPushedBack (pushed from this app — kept local copy)")

        // --- Journal entries against a known party (discount/write-off/other adjustments Tally
        // records via a plain Journal rather than a dedicated voucher type — see toMappedAdjustment).
        // Journals with no resolvable customer/supplier party (bank contras, depreciation, provisions,
        // ...) are skipped, same as payments above.
        var adjustmentsSynced = 0
        var skippedJournalNoParty = 0
        for (voucher in filtered) {
            if (voucher.classify() != Kind.JOURNAL) continue
            val partyName = voucher.resolvePartyName()
            val partyUid = partyName?.let { customerIdByName[it] ?: supplierIdByName[it] }
            if (partyUid == null) {
                skippedJournalNoParty++
                continue
            }
            val mapped = voucher.toMappedAdjustment(partyName) ?: continue
            ledgerPoster.postDocumentEntry(
                partyUid = partyUid,
                sourceType = LedgerSourceType.ADJUSTMENT,
                sourceUid = mapped.sourceUid,
                entryType = mapped.entryType,
                direction = mapped.direction,
                amount = Money.fromDouble(mapped.amount),
                entryDate = mapped.entryDate,
                voucherNo = mapped.voucherNo,
                narration = mapped.narration,
            )
            adjustmentsSynced++
        }
        if (adjustmentsSynced > 0) emit("Journal adjustments: $adjustmentsSynced posted to party ledgers")
        if (skippedJournalNoParty > 0) emit("Journals: skipped $skippedJournalNoParty (no customer/supplier party on the voucher)")

        val maxAlterId = vouchers.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_VOUCHER, maxAlterId)
        log.d { "syncVouchers: $invoicesSynced invoices, $purchasesSynced purchases, $paymentsSynced payments, $adjustmentsSynced adjustments" }
        return Triple(invoicesSynced, paymentsSynced, purchasesSynced)
    }

    private suspend fun buildGroupNameIndex(): Map<String, String> =
        groupDao.getGroups().associate { it.name to it.id }

    private suspend fun buildCategoryNameIndex(): Map<String, String> =
        categoryDao.getCategories().associate { it.name to it.id }

    private suspend fun buildUnitNameIndex(): Map<String, String> =
        unitDao.getAllUnits().first().associate { it.name.trim() to it.id }

    private suspend fun buildCustomerGroupNameIndex(): Map<String, String> =
        customerGroupDao.getAllCustomerGroups().first().associate { it.name to it.id }

    private fun String?.toAlterLong(): Long = this?.trim()?.toLongOrNull() ?: 0L
}
