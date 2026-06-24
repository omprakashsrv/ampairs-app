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
    suspend fun getPriceList(id: String): PriceList? {
        val header = priceListDao.getPriceListById(id) ?: return null
        val items = priceListItemDao.getItemsForPriceList(id).map { it.toPriceListItem() }
        return header.toPriceList(items)
    }

    // --- Writes (offline-first) -------------------------------------------------------------

    /** Persist a price-list aggregate (header + items) locally as unsynced and flag for push. */
    suspend fun savePriceList(priceList: PriceList): Result<PriceList> {
        require(priceList.uid.isNotBlank()) { "Price list UID must be set by the ViewModel" }
        return try {
            priceListDao.insertPriceList(priceList.toEntity().copy(synced = false))
            // Replace the item set for this list (simple aggregate write).
            priceListItemDao.deleteItemsForPriceList(priceList.uid)
            if (priceList.items.isNotEmpty()) {
                priceListItemDao.insertItems(
                    priceList.items.map { it.toEntity(priceList.uid).copy(synced = false) }
                )
            }
            markPending(SyncEntity.PRICE_LIST)
            Result.success(priceList)
        } catch (e: Exception) {
            PricingLogger.e(tag, "Failed to save price list", e)
            Result.failure(e)
        }
    }

    /** Soft-delete a price-list aggregate (header + items inactive + unsynced) and flag for push. */
    suspend fun deletePriceList(id: String): Result<Unit> {
        return try {
            val existing = priceListDao.getPriceListById(id)
            if (existing != null) {
                priceListDao.insertPriceList(existing.copy(active = false, synced = false))
                priceListItemDao.getItemsForPriceList(id).forEach {
                    priceListItemDao.insertItem(it.copy(active = false, synced = false))
                }
                markPending(SyncEntity.PRICE_LIST)
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
     * Resolve the effective unit price offline from the local read model. Mirrors the backend
     * precedence (per-customer > group/channel > product-group/brand/category > geo-zone/type >
     * predicate), tie-broken by list `priority`. Falls back to [fallbackUnitPrice] when no list
     * matches (CATALOG_FALLBACK).
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
            val tierPrice = item.priceForQuantity(quantity)
            val belowMoq = item.moq?.let { quantity < it } ?: false
            return PriceResolution(
                effectiveUnitPrice = tierPrice,
                currency = list.currency,
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
        geoZoneDao.getActiveGeoZones().map { it.toGeoZone() }.firstOrNull { it.matches(pincode) }?.uid

    private suspend fun bestItemFor(priceListId: String, productId: String, variantSku: String?): PriceListItem? {
        val items = priceListItemDao.getItemsForPriceList(priceListId)
            .map { it.toPriceListItem() }
            .filter { it.productId == productId }
        if (items.isEmpty()) return null
        // Variant match wins over a base (null-variant) item.
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

/** Effective unit price for a quantity: highest tier whose minQty <= qty, else the base unitPrice. */
internal fun PriceListItem.priceForQuantity(quantity: Double): Double {
    val applicable = tiers.filter { it.minQty <= quantity }.maxByOrNull { it.minQty }
    return applicable?.unitPrice ?: unitPrice
}

internal fun PriceListItem.appliedTierMinQty(quantity: Double): Double? =
    tiers.filter { it.minQty <= quantity }.maxByOrNull { it.minQty }?.minQty

/** Geo-zone membership: exact pincode, inclusive numeric range "a-b", or state-code match. */
internal fun GeoZone.matches(pincode: String): Boolean {
    val target = pincode.trim()
    val targetNum = target.toIntOrNull()
    return members.any { raw ->
        val m = raw.trim()
        when {
            m.equals(target, ignoreCase = true) -> true
            m.contains('-') && targetNum != null -> {
                val parts = m.split('-')
                val lo = parts.getOrNull(0)?.trim()?.toIntOrNull()
                val hi = parts.getOrNull(1)?.trim()?.toIntOrNull()
                lo != null && hi != null && targetNum in lo..hi
            }
            else -> false
        }
    }
}
