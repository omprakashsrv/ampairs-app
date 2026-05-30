package com.ampairs.tallysync

import co.touchlab.kermit.Logger
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_CATEGORY
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_GROUP
import com.ampairs.tallysync.TallyProductMapper.ENTITY_STOCK_ITEM
import com.ampairs.tallysync.TallyProductMapper.ENTITY_UNIT
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
    val error: String? = null,
) {
    val totalSynced get() = groupsSynced + categoriesSynced + productsSynced + unitsSynced
    val success get() = error == null
}

private val log = Logger.withTag("TallySyncService")

@Inject
class TallySyncService(
    private val engine: HttpClientEngine,
    private val groupDao: GroupDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val unitDao: UnitDao,
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

            log.i { "Tally sync complete — groups=$groupsSynced categories=$categoriesSynced units=$unitsSynced products=$productsSynced" }
            TallySyncResult(groupsSynced, categoriesSynced, productsSynced, unitsSynced)
        } catch (e: Exception) {
            log.e(e) { "Tally sync failed" }
            TallySyncResult(error = e.message ?: "Unknown error")
        }
    }

    private suspend fun syncGroups(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_GROUP).first()
        val response = repo.getStockGroups()
        val stockGroups = response.body?.data?.collection?.stockGroups ?: return 0

        val newItems = stockGroups.filter { it.alterId.toAlterLong() > lastAlterId }
        val entities = newItems.mapNotNull { it.toGroupEntity() }
        if (entities.isNotEmpty()) groupDao.insertAll(entities)

        val maxAlterId = stockGroups.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_GROUP, maxAlterId)

        return entities.size
    }

    private suspend fun syncCategories(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_CATEGORY).first()
        val response = repo.getStockCategories()
        val stockCategories = response.body?.data?.collection?.stockCategories ?: return 0

        val newItems = stockCategories.filter { it.alterId.toAlterLong() > lastAlterId }
        val entities = newItems.mapNotNull { it.toCategoryEntity() }
        if (entities.isNotEmpty()) categoryDao.insertAll(entities)

        val maxAlterId = stockCategories.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_CATEGORY, maxAlterId)

        return entities.size
    }

    private suspend fun syncUnits(repo: TallyRepository, workspaceSlug: String): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_UNIT).first()
        val response = repo.getUnits()
        val units = response.body?.data?.collection?.units ?: return 0

        val newItems = units.filter { it.alterId.toAlterLong() > lastAlterId }
        val entities = newItems.mapNotNull { it.toUnitEntity() }
        if (entities.isNotEmpty()) unitDao.insertUnits(entities)

        val maxAlterId = units.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_UNIT, maxAlterId)

        return entities.size
    }

    private suspend fun syncProducts(
        repo: TallyRepository,
        workspaceSlug: String,
        groupIdByName: Map<String, String>,
        categoryIdByName: Map<String, String>,
    ): Int {
        val lastAlterId = dataStore.getTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM).first()
        val response = repo.getStockItems()
        val stockItems = response.body?.data?.collection?.stockItems ?: return 0

        val newItems = stockItems.filter { it.alterId.toAlterLong() > lastAlterId }
        stockItems.firstOrNull()?.let { sample ->
            log.d { "StockItem price sample — name=${sample.name} standardPrice=${sample.standardPrice}" }
        }
        val entities = newItems.mapNotNull { it.toProductEntity(groupIdByName, categoryIdByName) }
        if (entities.isNotEmpty()) productDao.insertAll(entities)

        val maxAlterId = stockItems.maxOfOrNull { it.alterId.toAlterLong() } ?: lastAlterId
        if (maxAlterId > lastAlterId) dataStore.setTallyLastAlterId(workspaceSlug, ENTITY_STOCK_ITEM, maxAlterId)

        return entities.size
    }

    private suspend fun buildGroupNameIndex(): Map<String, String> =
        groupDao.getGroups().associate { it.name to it.id }

    private suspend fun buildCategoryNameIndex(): Map<String, String> =
        categoryDao.getCategories().associate { it.category.name to it.category.id }

    private fun String?.toAlterLong(): Long = this?.trim()?.toLongOrNull() ?: 0L
}
