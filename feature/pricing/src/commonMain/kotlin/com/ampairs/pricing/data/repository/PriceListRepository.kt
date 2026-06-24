package com.ampairs.pricing.data.repository

import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toGeoZone
import com.ampairs.pricing.data.db.entity.toPriceList
import com.ampairs.pricing.data.db.entity.toPriceListItem
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListAggregate
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceResolution
import com.ampairs.pricing.domain.model.PriceSource
import com.ampairs.pricing.domain.model.SalesChannel
import com.ampairs.pricing.util.PricingLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for price lists, items, and geo zones. The [PricingApi] lives in the
 * sync delegates; writes here persist to Room as unsynced and flag PENDING_PUSH so CentralSyncService
 * runs the automatic bulk push. Also hosts the offline [resolve] used by order/invoice entry.
 *
 * Headers and items are separate syncable resources (PRICE_LIST / PRICE_LIST_ITEM), matching the
 * backend two-feed contract. Money is held in minor units; [resolve] returns major units for the
 * order/invoice line seam.
 */
@OptIn(ExperimentalTime::class)
@Inject
class PriceListRepository(
    private val priceListDao: PriceListDao,
    private val priceListItemDao: PriceListItemDao,
    private val geoZoneDao: GeoZoneDao,
    private val syncStateDao: SyncStateDao,
) {

    private val tag = "PriceListRepository"

    // --- Reads ------------------------------------------------------------------------------

    fun observePriceLists(): Flow<List<PriceList>> =
        priceListDao.getAllPriceLists().map { rows -> rows.map { it.toPriceList() } }

    fun observeGeoZones(): Flow<List<GeoZone>> =
        geoZoneDao.getAllGeoZones().map { rows -> rows.map { it.toGeoZone() } }

    /** Full aggregate (header + items) for editing. */
    suspend fun getPriceList(id: String): PriceListAggregate? {
        val header = priceListDao.getPriceListById(id)?.toPriceList() ?: return null
        val items = priceListItemDao.getItemsForPriceList(id).map { it.toPriceListItem() }
        return PriceListAggregate(header, items)
    }

    // --- Writes (offline-first) -------------------------------------------------------------

    /** Persist a price-list aggregate (header + items) locally as unsynced and flag both feeds. */
    suspend fun savePriceList(aggregate: PriceListAggregate): Result<PriceListAggregate> {
        val header = aggregate.header
        require(header.uid.isNotBlank()) { "Price list UID must be set by the ViewModel" }
        return try {
            priceListDao.insertPriceList(header.toEntity().copy(synced = false))
            priceListItemDao.deleteItemsForPriceList(header.uid)
            if (aggregate.items.isNotEmpty()) {
                priceListItemDao.insertItems(
                    aggregate.items.map { it.toEntity(header.uid).copy(synced = false) }
                )
            }
            markPending(SyncEntity.PRICE_LIST)
            markPending(SyncEntity.PRICE_LIST_ITEM)
            Result.success(aggregate)
        } catch (e: Exception) {
            PricingLogger.e(tag, "Failed to save price list", e)
            Result.failure(e)
        }
    }

    /** Soft-delete a price-list aggregate (header + items inactive + unsynced) and flag both feeds. */
    suspend fun deletePriceList(id: String): Result<Unit> {
        return try {
            val existing = priceListDao.getPriceListById(id)
            if (existing != null) {
                priceListDao.insertPriceList(existing.copy(active = false, synced = false))
                priceListItemDao.getItemsForPriceList(id).forEach {
                    priceListItemDao.insertItem(it.copy(active = false, synced = false))
                }
                markPending(SyncEntity.PRICE_LIST)
                markPending(SyncEntity.PRICE_LIST_ITEM)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            PricingLogger.e(tag, "Failed to delete price list", e)
            Result.failure(e)
        }
    }

    suspend fun saveGeoZone(geoZone: GeoZone): Result<GeoZone> {
        require(geoZone.uid.isNotBlank()) { "Geo zone UID must be set by the ViewModel" }
        return try {
            geoZoneDao.insertGeoZone(geoZone.toEntity().copy(synced = false))
            markPending(SyncEntity.GEO_ZONE)
            Result.success(geoZone)
        } catch (e: Exception) {
            PricingLogger.e(tag, "Failed to save geo zone", e)
            Result.failure(e)
        }
    }

    suspend fun deleteGeoZone(id: String): Result<Unit> {
        return try {
            val existing = geoZoneDao.getGeoZoneById(id)
            if (existing != null) {
                geoZoneDao.insertGeoZone(existing.copy(active = false, synced = false))
                markPending(SyncEntity.GEO_ZONE)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            PricingLogger.e(tag, "Failed to delete geo zone", e)
            Result.failure(e)
        }
    }

    // --- Offline resolution -----------------------------------------------------------------

    /**
     * Resolve the effective unit price offline from the local read model. Mirrors backend precedence
     * (per-customer > group/channel > product-group/brand/category > geo-zone/type > predicate),
     * tie-broken by `priority`. Returns major units; falls back to [fallbackUnitPrice] (CATALOG_FALLBACK).
     */
    suspend fun resolve(
        channel: SalesChannel,
        productId: String,
        quantity: Double,
        fallbackUnitPrice: Double,
        currency: String = "INR",
        variantSku: String? = null,
        customerId: String? = null,
        customerGroupId: String? = null,
        customerType: String? = null,
        pincode: String? = null,
    ): PriceResolution {
        val zoneId = pincode?.let { zoneIdForPincode(it) }
        val candidates = priceListDao.getActivePriceLists()
            .map { it.toPriceList() }
            .filter { it.channel == channel && it.matchesTargeting(customerId, customerGroupId, customerType, zoneId) }
            .sortedWith(compareByDescending<PriceList> { it.specificity() }.thenByDescending { it.priority })

        for (list in candidates) {
            val item = bestItemFor(list.uid, productId, variantSku) ?: continue
            val tierMinor = item.priceMinorForQuantity(quantity)
            val belowMoq = item.moq?.let { quantity < it } ?: false
            return PriceResolution(
                effectiveUnitPrice = tierMinor / 100.0,
                currency = item.currency ?: list.currency,
                source = PriceSource.PRICE_LIST,
                matchedPriceListUid = list.uid,
                appliedTierMinQty = item.appliedTierMinQty(quantity),
                belowMoq = belowMoq,
            )
        }
        return PriceResolution(
            effectiveUnitPrice = fallbackUnitPrice,
            currency = currency,
            source = PriceSource.CATALOG_FALLBACK,
        )
    }

    private suspend fun zoneIdForPincode(pincode: String): String? =
        geoZoneDao.getActiveGeoZones().map { it.toGeoZone() }.firstOrNull { it.members.contains(pincode) }?.uid

    private suspend fun bestItemFor(priceListId: String, productId: String, variantSku: String?): PriceListItem? {
        val items = priceListItemDao.getItemsForPriceList(priceListId)
            .map { it.toPriceListItem() }
            .filter { it.productId == productId }
        if (items.isEmpty()) return null
        return items.firstOrNull { variantSku != null && it.variantSku == variantSku }
            ?: items.firstOrNull { it.variantSku == null }
            ?: items.first()
    }

    private suspend fun markPending(entity: SyncEntity) {
        syncStateDao.markPendingPush(entity, Clock.System.now().toEpochMilliseconds())
    }
}

// --- Pure helpers (testable, no IO) ---------------------------------------------------------

/** Specificity score for precedence: more specific targeting dimensions outrank broader ones. */
internal fun PriceList.specificity(): Int {
    var score = 0
    if (!customerId.isNullOrBlank()) score += 100
    if (!customerGroupId.isNullOrBlank()) score += 40
    if (!productGroupId.isNullOrBlank()) score += 20
    if (!brandId.isNullOrBlank()) score += 15
    if (!categoryId.isNullOrBlank()) score += 15
    if (!geoZoneId.isNullOrBlank()) score += 10
    if (!customerType.isNullOrBlank()) score += 10
    if (attributePredicates.isNotEmpty()) score += 1
    return score
}

/** A list matches when each of its set targeting dimensions matches the provided context. */
internal fun PriceList.matchesTargeting(
    customerId: String?,
    customerGroupId: String?,
    customerType: String?,
    zoneId: String?,
): Boolean {
    if (!this.customerId.isNullOrBlank() && this.customerId != customerId) return false
    if (!this.customerGroupId.isNullOrBlank() && this.customerGroupId != customerGroupId) return false
    if (!this.customerType.isNullOrBlank() && this.customerType != customerType) return false
    if (!this.geoZoneId.isNullOrBlank() && this.geoZoneId != zoneId) return false
    return true
}

/** Effective unit price (minor) for a quantity: highest tier whose minQty <= qty, else base. */
internal fun PriceListItem.priceMinorForQuantity(quantity: Double): Long {
    val applicable = tiers.filter { it.minQty <= quantity }.maxByOrNull { it.minQty }
    return applicable?.unitPriceMinor ?: unitPriceMinor
}

internal fun PriceListItem.appliedTierMinQty(quantity: Double): Double? =
    tiers.filter { it.minQty <= quantity }.maxByOrNull { it.minQty }?.minQty
