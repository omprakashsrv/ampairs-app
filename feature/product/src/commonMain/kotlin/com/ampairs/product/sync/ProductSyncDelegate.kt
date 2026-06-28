package com.ampairs.product.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.UnitApiModel
import com.ampairs.product.api.model.UnitConversionApiModel
import com.ampairs.product.data.api.ProductApi
import com.ampairs.product.db.dao.ProductDao
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.domain.asDatabaseModel
import com.ampairs.product.domain.asProductApiModel
import com.ampairs.product.util.ProductLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.unit.data.repository.UnitConversionData
import com.ampairs.unit.data.repository.UnitConversionSync
import com.ampairs.unit.data.repository.UnitLookup
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all product ↔ server traffic. The repository is local-only; this delegate is the single
 * place that talks to [ProductApi] — bulk push of unsynced rows, batched pull (permanently
 * deleting server-DELETED rows), and backend event refresh.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRODUCT)
class ProductSyncDelegate(
    private val productApi: ProductApi,
    private val productDao: ProductDao,
    private val syncStateDao: SyncStateDao,
    // Product-scoped unit conversions ride the product /sync feed — this bridge (impl in feature/unit)
    // reads them for push and writes server copies back on pull; UnitLookup resolves the nested unit
    // objects the conversion DTO requires.
    private val unitConversionSync: UnitConversionSync,
    private val unitLookup: UnitLookup,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRODUCT

    // PRODUCT_CATALOG (groups/brands/categories/sub-categories) must be on the server before
    // products can be inserted due to FK constraints.
    override val pushDependencies: List<SyncEntity> = listOf(SyncEntity.PRODUCT_CATALOG)

    // For pulls, products reference catalog, unit and tax — pull those first so a product's
    // unit/tax/category references resolve locally.
    override val dependsOn: List<SyncEntity> =
        listOf(SyncEntity.PRODUCT_CATALOG, SyncEntity.UNIT, SyncEntity.TAX)

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { refreshProductFromServer(entityId) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = productDao.unSyncedProducts()
        // Conversions ride the product feed, so a product whose only change is a dirty conversion must
        // also push. Pull those extra (otherwise-synced) products in by id and append them.
        val unsyncedIds = unsynced.mapTo(mutableSetOf()) { it.id }
        val extraIds = unitConversionSync.productIdsWithUnsyncedConversions().filter { it !in unsyncedIds }
        val extras = if (extraIds.isNotEmpty()) productDao.productsByIds(extraIds) else emptyList()
        val toPush = unsynced + extras
        if (toPush.isEmpty()) return@runCatching 0
        var pushed = 0
        var failed = 0
        var lastError: Throwable? = null
        // One failed batch must not abort the rest (reference pattern: CustomerSyncDelegate) —
        // remaining batches still push; failed rows stay synced=0 and retry next cycle.
        for (batch in toPush.chunked(100)) {
            val apiModels = batch.map { it.asProductApiModel() }.withConversions()
            productApi.bulkUpdateProducts(apiModels)
                .onSuccess {
                    batch.forEach { entity ->
                        // Soft-deleted rows are now confirmed on the server → hard-delete locally.
                        // Active rows just flip to synced.
                        if (entity.soft_deleted == 1) productDao.deleteById(entity.id)
                        else productDao.insert(entity.copy(synced = 1))
                        // Conversions were carried in the same payload → mark them synced too.
                        unitConversionSync.markConversionsSynced(entity.id)
                    }
                    pushed += batch.size
                }
                .onFailure { error ->
                    ProductLogger.w("ProductSyncDelegate", "Batch push failed", error)
                    failed += batch.size
                    lastError = error
                }
        }
        // Rule 2 (/offline-sync): nothing pushed + failures present must surface as failure,
        // not Success(0), so CentralSyncService marks the entity FAILED and retries on reconnect.
        if (pushed == 0 && failed > 0) {
            throw lastError ?: Exception("$failed product(s) failed to push — will retry on reconnect")
        }
        pushed
    }

    /**
     * Enriches a freshly-mapped [ProductApiModel] (whose `unitConversions` is empty) with this
     * product's active conversions, resolving the nested base/derived unit objects the DTO requires.
     * Conversions whose units can't be resolved locally are dropped (can't form a valid payload).
     */
    private suspend fun List<ProductApiModel>.withConversions(): List<ProductApiModel> {
        if (isEmpty()) return this
        val byProduct = unitConversionSync.conversionsForProducts(map { it.id })
        if (byProduct.isEmpty()) return this
        val unitIds = byProduct.values.flatten().flatMapTo(mutableSetOf()) { listOf(it.baseUnitId, it.derivedUnitId) }
        val units = unitIds.associateWith { id -> unitLookup.getUnitById(id)?.toUnitApiModel() }
        return map { model ->
            val convs = byProduct[model.id].orEmpty().mapNotNull { it.toApiModel(units) }
            if (convs.isEmpty()) model else model.copy(unitConversions = convs)
        }
    }

    private suspend fun pull(): Result<Int> = runCatching {
        // Incremental: only pull rows updated since the last successful pull checkpoint.
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.PRODUCT) ?: ""
        var page = 0
        var total = 0
        var maxServerTime = ""
        var hasNext: Boolean
        do {
            val pageResp = productApi.getProductsSync(
                lastSync = lastSync.ifBlank { null },
                page = page,
                size = 100,
                sortBy = "updatedAt",
                sortDir = "ASC",
            ).getOrThrow()
            val batch = pageResp.content
            // Permanently delete locally anything the server reports as DELETED.
            batch.filter { it.status?.equals("DELETED", ignoreCase = true) == true }
                .forEach { productDao.deleteById(it.id) }
            // Upsert the rest.
            val toUpsert = batch.filter { it.status?.equals("DELETED", ignoreCase = true) != true }
            if (toUpsert.isNotEmpty()) productDao.insertAll(reconcileWithLocal(toUpsert.asDatabaseModel()))
            // Mirror each product's conversions locally (local-unsynced conversions win; DELETED → cleared).
            batch.forEach { model ->
                val deleted = model.status?.equals("DELETED", ignoreCase = true) == true
                val convs = if (deleted) emptyList() else model.unitConversions.map { it.toConversionData(model.id) }
                unitConversionSync.replaceConversionsFromServer(model.id, convs)
            }
            val batchMaxTime = batch.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
            if (batchMaxTime > maxServerTime) maxServerTime = batchMaxTime
            total += batch.size
            hasNext = pageResp.hasNext
            page++
        } while (hasNext && total < 10000)
        // Advance the checkpoint so the next pull is incremental.
        if (maxServerTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.PRODUCT, maxServerTime)
        total
    }

    /**
     * Reconciles server rows with local state before upsert, keyed by the stable product id:
     * drops any row whose local copy is unsynced (synced == 0) — local edits win until pushed
     * (full sync pushes first, so a still-unsynced row means the push hasn't completed).
     */
    private suspend fun reconcileWithLocal(entities: List<ProductEntity>): List<ProductEntity> {
        if (entities.isEmpty()) return entities
        val existing = productDao.productsByIds(entities.map { it.id }).associateBy { it.id }
        return entities.filter { e ->
            val local = existing[e.id]
            local == null || local.synced != 0
        }
    }

    private suspend fun refreshProductFromServer(productId: String) {
        productApi.getProduct(productId)
            .onSuccess { model ->
                productDao.insertAll(reconcileWithLocal(listOf(model).asDatabaseModel()))
                val deleted = model.status?.equals("DELETED", ignoreCase = true) == true
                val convs = if (deleted) emptyList() else model.unitConversions.map { it.toConversionData(model.id) }
                unitConversionSync.replaceConversionsFromServer(model.id, convs)
                ProductLogger.i("ProductSyncDelegate", "✅ Refreshed product from server: $productId")
            }
            .onFailure { error ->
                ProductLogger.w("ProductSyncDelegate", "Product not found on server: $productId - ${error.message}")
            }
    }
}

// --- Conversion ↔ DTO mapping ----------------------------------------------------------------

/** unit-api Unit → the product DTO's nested unit object. */
private fun com.ampairs.unit.domain.model.Unit.toUnitApiModel(): UnitApiModel = UnitApiModel(
    id = uid,
    refId = refId,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    active = active,
    softDeleted = false,
)

/** [UnitConversionData] → push DTO; null when either nested unit couldn't be resolved locally. */
private fun UnitConversionData.toApiModel(units: Map<String, UnitApiModel?>): UnitConversionApiModel? {
    val base = units[baseUnitId] ?: return null
    val derived = units[derivedUnitId] ?: return null
    return UnitConversionApiModel(
        id = uid,
        productId = productId,
        baseUnit = base,
        derivedUnit = derived,
        multiplier = multiplier,
        active = active,
        softDeleted = !active,
    )
}

/** Pull DTO → [UnitConversionData] (falls back to [fallbackProductId] when the DTO omits it). */
private fun UnitConversionApiModel.toConversionData(fallbackProductId: String): UnitConversionData =
    UnitConversionData(
        uid = id,
        productId = productId.ifBlank { fallbackProductId },
        baseUnitId = baseUnit.id,
        derivedUnitId = derivedUnit.id,
        multiplier = multiplier,
        active = active && !softDeleted,
    )
