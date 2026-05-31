package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
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
import com.ampairs.unit.data.db.dao.UnitDao
import dev.zacsweers.metro.Inject
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.flow.first

data class TallySyncResult(
    val groupsSynced: Int = 0,
    val categoriesSynced: Int = 0,
    val productsSynced: Int = 0,
    val unitsSynced: Int = 0,
    val stockBalancesUpdated: Int = 0,
    val customerGroupsSynced: Int = 0,
    val customersSynced: Int = 0,
    val error: String? = null,
) {
    val totalSynced get() = groupsSynced + categoriesSynced + productsSynced + unitsSynced + stockBalancesUpdated + customerGroupsSynced + customersSynced
    val success get() = error == null
}

private val log = Logger.withTag("TallySyncService")
private const val BATCH_SIZE = 100

@Inject
class TallySyncService(
    private val engine: HttpClientEngine,
    private val groupDao: GroupDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val unitDao: UnitDao,
    private val customerDao: CustomerDao,
    private val customerGroupDao: CustomerGroupDao,
    private val dataStore: AppPreferencesDataStore,
) {
    suspend fun sync(workspaceSlug: String): TallySyncResult {
        val host = dataStore.getTallyHost(workspaceSlug).first()
        if (host.isBlank()) {
            return TallySyncResult(error = "Tally host not configured")
        }
        val port = dataStore.getTallyPort(workspaceSlug).first()
        val baseUrl = "http://$host:$port"
        log.i { "Starting Tally sync from $baseUrl" }

        val repo = TallyRepository(TallyApiImpl(engine, baseUrl))

        return try {
            val groupsSynced = syncGroups(repo, workspaceSlug)
            val categoriesSynced = syncCategories(repo, workspaceSlug)
            val unitsSynced = syncUnits(repo, workspaceSlug)
            // Build lookup maps from freshly upserted groups/categories
            val groupIdByName = buildGroupNameIndex()
            val categoryIdByName = buildCategoryNameIndex()
            val productsSynced = syncProducts(repo, workspaceSlug, groupIdByName, categoryIdByName)
            val stockBalancesUpdated = syncStockBalances(repo)

            val customerGroupsSynced = syncCustomerGroups(repo, workspaceSlug)
            val customerGroupIdByName = buildCustomerGroupNameIndex()
            val customersSynced = syncCustomers(repo, workspaceSlug, customerGroupIdByName)

            log.i { "Tally sync complete — groups=$groupsSynced categories=$categoriesSynced units=$unitsSynced products=$productsSynced stockBalances=$stockBalancesUpdated customerGroups=$customerGroupsSynced customers=$customersSynced" }
            TallySyncResult(groupsSynced, categoriesSynced, productsSynced, unitsSynced, stockBalancesUpdated, customerGroupsSynced, customersSynced)
        } catch (e: Exception) {
            log.e(e) { "Tally sync failed" }
            TallySyncResult(error = e.message ?: "Unknown error")
        }
    }

    private suspend fun syncGroups(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_GROUP).first()
        val stockGroups = repo.getStockGroups().body?.data?.collection?.stockGroups ?: return 0
        val entities = stockGroups
            .filter { it.alterId.toAlterLong() > lastAlterId }
            .mapNotNull { it.toGroupEntity() }
        entities.chunked(BATCH_SIZE).forEach { groupDao.insertAll(it) }
        val maxAlterId = stockGroups.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_GROUP, maxAlterId)
        log.d { "syncGroups: ${entities.size} new (batches=${entities.size / BATCH_SIZE + 1})" }
        return entities.size
    }

    private suspend fun syncCategories(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_CATEGORY).first()
        val stockCategories = repo.getStockCategories().body?.data?.collection?.stockCategories ?: return 0
        val entities = stockCategories
            .filter { it.alterId.toAlterLong() > lastAlterId }
            .mapNotNull { it.toCategoryEntity() }
        entities.chunked(BATCH_SIZE).forEach { categoryDao.insertAll(it) }
        val maxAlterId = stockCategories.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_CATEGORY, maxAlterId)
        log.d { "syncCategories: ${entities.size} new" }
        return entities.size
    }

    private suspend fun syncUnits(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_UNIT).first()
        val units = repo.getUnits().body?.data?.collection?.units ?: return 0
        val entities = units
            .filter { it.alterId.toAlterLong() > lastAlterId }
            .mapNotNull { it.toUnitEntity() }
        entities.chunked(BATCH_SIZE).forEach { unitDao.insertUnits(it) }
        val maxAlterId = units.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_UNIT, maxAlterId)
        log.d { "syncUnits: ${entities.size} new" }
        return entities.size
    }

    private suspend fun syncProducts(
        repo: TallyRepository,
        workspaceSlug: String,
        groupIdByName: Map<String, String>,
        categoryIdByName: Map<String, String>,
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM).first()
        val stockItems = repo.getStockItems().body?.data?.collection?.stockItems ?: return 0
        val entities = stockItems
            .filter { it.alterId.toAlterLong() > lastAlterId }
            .mapNotNull { it.toProductEntity(groupIdByName, categoryIdByName) }
        var batchNum = 0
        entities.chunked(BATCH_SIZE).forEach { batch ->
            productDao.insertAll(batch)
            batchNum++
            log.d { "syncProducts batch $batchNum: inserted ${batch.size}" }
        }
        val maxAlterId = stockItems.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM, maxAlterId)
        log.d { "syncProducts: ${entities.size} new across $batchNum batches" }
        return entities.size
    }

    private suspend fun syncStockBalances(repo: TallyRepository): Int {
        val items = repo.getStockBalances().body?.data?.collection?.stockItems ?: return 0
        var updated = 0
        items.chunked(BATCH_SIZE).forEach { batch ->
            batch.forEach { item ->
                val qty = item.closingBalance?.parseClosingQty() ?: return@forEach
                val id = if (!item.guid.isNullOrBlank()) item.guid!!
                         else if (!item.name.isNullOrBlank()) "TALLY_${item.name!!.hashCode()}"
                         else return@forEach
                productDao.updateStockQuantity(id, qty)
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
        val entities = groups
            .filter { it.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId } }
            .mapNotNull { it.toCustomerGroupEntity() }
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
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_LEDGER).first()
        val ledgers = repo.getLedgers().body?.data?.collection?.ledgers ?: return 0
        log.d { "syncCustomers: API returned ${ledgers.size} total, lastAlterId=$lastAlterId" }
        val entities = ledgers
            .filter { it.isBillWiseOn != "No" }
            .filter { it.alterId.toAlterLong().let { id -> id == 0L || id > lastAlterId } }
            .mapNotNull { it.toCustomerEntity(customerGroupIdByName) }
        entities.chunked(BATCH_SIZE).forEach { customerDao.insertCustomers(it) }
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
        categoryDao.getCategories().associate { it.category.name to it.category.id }

    private suspend fun buildCustomerGroupNameIndex(): Map<String, String> =
        customerGroupDao.getAllCustomerGroups().first().associate { it.name to it.id }

    private fun String?.toAlterLong(): Long = this?.trim()?.toLongOrNull() ?: 0L
}
