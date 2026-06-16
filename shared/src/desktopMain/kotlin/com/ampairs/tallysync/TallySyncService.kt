package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import com.ampairs.tallysync.TallyCustomerMapper.ENTITY_ACCOUNT_GROUP
import com.ampairs.tallysync.TallyCustomerMapper.ENTITY_LEDGER
import com.ampairs.tallysync.TallyCustomerMapper.toCustomerEntity
import com.ampairs.tallysync.TallyCustomerMapper.toCustomerGroupEntity
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_CATEGORY
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_GROUP
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_ITEM
import com.ampairs.tallysync.TallyProductMapper.ENTITY_UNIT
import com.ampairs.tallysync.TallyProductMapper.parseClosingQty
import com.ampairs.tallysync.TallyProductMapper.toCategoryEntity
import com.ampairs.tallysync.TallyProductMapper.toGroupEntity
import com.ampairs.tallysync.TallyProductMapper.toProductEntity
import com.ampairs.tallysync.TallyProductMapper.toUnitEntity
import com.ampairs.tallysync.TallyProductMapper.extractHsnCode
import com.ampairs.connector.data.api.ConnectorApi
import com.ampairs.connector.domain.ConnectorConfigProvider
import com.ampairs.connector.domain.SparseUpsertRow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.ampairs.tally.model.master.StockItem
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class TallySyncResult(
    val groupsSynced: Int = 0,
    val categoriesSynced: Int = 0,
    val productsSynced: Int = 0,
    val unitsSynced: Int = 0,
    val stockBalancesUpdated: Int = 0,
    val customerGroupsSynced: Int = 0,
    val customersSynced: Int = 0,
    val taxCodesFound: Int = 0,
    val taxCodesToImport: Int = 0,
    /** True when the rows were pushed to the backend connector (sparse upsert) this cycle. */
    val pushedViaConnector: Boolean = false,
    val error: String? = null,
) {
    val totalSynced get() = groupsSynced + categoriesSynced + productsSynced + unitsSynced + stockBalancesUpdated + customerGroupsSynced + customersSynced
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
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao,
    private val customerGroupDao: CustomerGroupDao,
    private val taxCodeRepository: TaxCodeRepository,
    private val taxRuleRepository: TaxRuleRepository,
    private val taxComponentRepository: TaxComponentRepository,
    private val taxConfigurationRepository: TaxConfigurationRepository,
    private val dataStore: AppPreferencesDataStore,
    private val connectorConfigProvider: ConnectorConfigProvider,
    private val connectorApi: ConnectorApi,
) {
    /** Set per [sync] cycle: the backend Tally connector installation uid, or null if not installed. */
    private var connectorInstallationUid: String? = null

    /**
     * When a Tally connector is installed on the backend, push the just-mapped rows to it via the
     * sparse-upsert endpoint (mapped-fields-only, non-destructive) instead of the legacy full-upsert
     * `/sync` path. Non-fatal: failures are logged and the local DB write still stands.
     */
    private suspend fun pushToConnector(entityType: String, rows: List<SparseUpsertRow>) {
        val uid = connectorInstallationUid ?: return
        if (rows.isEmpty()) return
        runCatching {
            rows.chunked(BATCH_SIZE).forEach { connectorApi.upsert(uid, entityType, it) }
            log.d { "Connector: pushed ${rows.size} '$entityType' rows" }
        }.onFailure { log.w(it) { "Connector push failed for '$entityType' (non-fatal)" } }
    }

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    /** Reactive, human-readable log of recent sync activity (capped to the last [MAX_LOG_LINES] lines). */
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    private val _taxCodeCandidates = MutableStateFlow<List<TallyTaxCodeCandidate>>(emptyList())
    /** Distinct HSN/SAC codes found on Tally stock items, flagged with local subscription status. */
    val taxCodeCandidates: StateFlow<List<TallyTaxCodeCandidate>> = _taxCodeCandidates.asStateFlow()

    fun clearLog() {
        _logLines.value = emptyList()
    }

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
        // Resolve the backend Tally connector installation (if installed). Rows are then pushed to it
        // via the sparse-upsert endpoint instead of the legacy full-upsert /sync path, and the
        // backend connector config becomes the source of truth for host/port (FR-H03) — falling back
        // to local DataStore only when the backend has no value.
        val installationUid = runCatching { connectorConfigProvider.installation()?.uid }.getOrNull()
        connectorInstallationUid = installationUid
        val backendConfig = installationUid?.let {
            runCatching { connectorConfigProvider.config(it) }.getOrNull()
        }

        val host = backendConfig?.nonSecretValues?.get("host")?.takeIf { it.isNotBlank() }
            ?: dataStore.getTallyHost(workspaceSlug).first()
        if (host.isBlank()) {
            emit("Tally host not configured")
            return TallySyncResult(error = "Tally host not configured")
        }
        val port = backendConfig?.nonSecretValues?.get("port")?.trim()?.toIntOrNull()
            ?: dataStore.getTallyPort(workspaceSlug).first()
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
            val productsSynced = syncProducts(repo, workspaceSlug, groupIdByName, categoryIdByName, unitIdByName)
            emit("Products: $productsSynced new")
            val stockBalancesUpdated = syncStockBalances(repo)
            emit("Stock balances: $stockBalancesUpdated updated")

            val customerGroupsSynced = syncCustomerGroups(repo, workspaceSlug)
            emit("Customer groups: $customerGroupsSynced upserted")
            val customerGroupIdByName = buildCustomerGroupNameIndex()
            val customersSynced = syncCustomers(repo, workspaceSlug, customerGroupIdByName)
            emit("Customers: $customersSynced upserted")

            // Clean up any previously stored invalid values (pre-sanitizer data)
            customerDao.nullifyInvalidPhones()
            customerDao.nullifyInvalidPincodes()

            val candidates = _taxCodeCandidates.value
            val toImport = candidates.count { !it.alreadySubscribed }
            val result = TallySyncResult(
                groupsSynced = groupsSynced,
                categoriesSynced = categoriesSynced,
                productsSynced = productsSynced,
                unitsSynced = unitsSynced,
                stockBalancesUpdated = stockBalancesUpdated,
                customerGroupsSynced = customerGroupsSynced,
                customersSynced = customersSynced,
                pushedViaConnector = connectorInstallationUid != null,
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
        pushToConnector("product_group", entities.map { g ->
            SparseUpsertRow(refId = g.ref_id, uid = g.id, values = buildJsonObject { put("name", g.name) })
        })
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
        pushToConnector("product_category", entities.map { c ->
            SparseUpsertRow(refId = c.ref_id, uid = c.id, values = buildJsonObject { put("name", c.name) })
        })
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

    private suspend fun syncProducts(
        repo: TallyRepository,
        workspaceSlug: String,
        groupIdByName: Map<String, String>,
        categoryIdByName: Map<String, String>,
        unitIdByName: Map<String, String>,
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM).first()
        val stockItems = repo.getStockItems().body?.data?.collection?.stockItems ?: return 0
        val filtered = stockItems.filter { it.alterId.toAlterLong() > lastAlterId }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            productDao.getProductsByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        var batchNum = 0
        val entities = filtered.mapNotNull { si ->
            val id = si.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("PRD")
            si.toProductEntity(id, groupIdByName, categoryIdByName, unitIdByName)
        }
        entities.chunked(BATCH_SIZE).forEach { batch ->
            productDao.insertAll(batch)
            batchNum++
            log.d { "syncProducts batch $batchNum: inserted ${batch.size}" }
        }
        pushToConnector("product", entities.map { p ->
            SparseUpsertRow(
                refId = p.ref_id,
                uid = p.id,
                values = buildJsonObject {
                    put("name", p.name)
                    put("sellingPrice", p.selling_price)
                    put("mrp", p.mrp)
                    put("costPrice", p.dp)
                    put("taxCode", p.tax_code)
                    p.category_id?.let { put("categoryId", it) }
                    p.group_id?.let { put("groupId", it) }
                },
            )
        })
        // Scan the full catalogue (not just alterId-filtered items) so the tax-code panel always
        // reflects every HSN in Tally, even when no products are newly changed this cycle.
        scanTaxCodeCandidates(stockItems)
        scanUnitConversions(stockItems)

        val maxAlterId = stockItems.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM, maxAlterId)
        log.d { "syncProducts: ${entities.size} new across $batchNum batches" }
        return entities.size
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

    private suspend fun syncStockBalances(repo: TallyRepository): Int {
        val items = repo.getStockBalances().body?.data?.collection?.stockItems ?: return 0
        var updated = 0
        items.chunked(BATCH_SIZE).forEach { batch ->
            batch.forEach { item ->
                val qty = item.closingBalance?.parseClosingQty() ?: return@forEach
                val guid = item.guid?.takeIf { it.isNotBlank() } ?: return@forEach
                productDao.updateStockQuantityByTallyRef(guid, qty)
                updated++
            }
        }
        log.d { "syncStockBalances: $updated updated" }
        return updated
    }

    private suspend fun syncCustomerGroups(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_ACCOUNT_GROUP).first()
        val groups = repo.getGroups().body?.data?.collection?.groups ?: return 0
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
        pushToConnector("customer_group", entities.map { g ->
            SparseUpsertRow(
                refId = g.ref_id,
                uid = g.id,
                values = buildJsonObject {
                    put("name", g.name)
                    g.description?.let { put("description", it) }
                },
            )
        })
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
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_LEDGER).first()
        val ledgers = repo.getLedgers().body?.data?.collection?.ledgers ?: return 0
        log.d { "syncCustomers: API returned ${ledgers.size} total, lastAlterId=$lastAlterId" }
        val filtered = ledgers
            .filter { it.isBillWiseOn != "No" }
            .filter { it.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId } }

        val guids = filtered.mapNotNull { it.guid?.takeIf { it.isNotBlank() } }
        val existingIdByGuid = mutableMapOf<String, String>()
        for (chunk in guids.chunked(BATCH_SIZE)) {
            customerDao.getCustomersByTallyRefIds(chunk).forEach { e -> e.ref_id?.let { existingIdByGuid[it] = e.id } }
        }

        val entities = filtered.mapNotNull { l ->
            val id = l.guid?.takeIf { it.isNotBlank() }?.let { existingIdByGuid[it] } ?: UidGenerator.generateUid("CUS")
            l.toCustomerEntity(id, customerGroupIdByName)
        }
        entities.chunked(BATCH_SIZE).forEach { customerDao.insertCustomers(it) }
        pushToConnector("customer", entities.map { c ->
            SparseUpsertRow(
                refId = c.ref_id,
                uid = c.id,
                values = buildJsonObject {
                    put("name", c.name)
                    c.phone?.let { put("phone", it) }
                    c.landline?.let { put("landline", it) }
                    c.gstNumber?.let { put("gstNumber", it) }
                    c.address?.let { put("address", it) }
                    c.street?.let { put("street", it) }
                    c.city?.let { put("city", it) }
                    c.state?.let { put("state", it) }
                    c.pincode?.let { put("pincode", it) }
                    c.customer_group?.let { put("customerGroup", it) }
                },
            )
        })
        if (entities.isNotEmpty()) {
            val maxAlterId = ledgers.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
            if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_LEDGER, maxAlterId)
        }
        log.d { "syncCustomers: ${entities.size} upserted" }
        return entities.size
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
