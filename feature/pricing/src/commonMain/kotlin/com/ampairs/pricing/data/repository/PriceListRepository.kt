package com.ampairs.pricing.data.repository

import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toGeoZone
import com.ampairs.pricing.data.db.entity.toPriceList
import com.ampairs.pricing.data.db.entity.toPriceListItem
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.PriceCandidate
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListAggregate
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceResolution
import com.ampairs.pricing.domain.model.PriceResolutionTrace
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
import kotlin.time.Instant

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

    suspend fun getGeoZone(id: String): GeoZone? = geoZoneDao.getGeoZoneById(id)?.toGeoZone()

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
     *
     * [asOfDate] is the document date the price is resolved "as of" (the order/invoice date for
     * billing, Tally's "Applicable From"): items effective after it are dormant, and the most-recent
     * applicable version at that instant wins. Null/blank => now. ISO-8601 (UTC).
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
        asOfDate: String? = null,
    ): PriceResolution {
        val asOf = parseInstantOrNull(asOfDate) ?: Clock.System.now()
        val zoneId = pincode?.let { zoneIdForPincode(it) }
        val candidates = priceListDao.getActivePriceLists()
            .map { it.toPriceList() }
            .filter { it.channel == channel && it.matchesTargeting(customerId, customerGroupId, customerType, zoneId) }
            .sortedWith(compareByDescending<PriceList> { it.specificity() }.thenByDescending { it.priority })

        for (list in candidates) {
            val item = bestItemFor(list.uid, productId, variantSku, asOf) ?: continue
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

    /**
     * Like [resolve], but also returns the ranked candidate trace for the Price Tester's
     * "why this price won" panel. The winning list (if any) is reused from [resolve] so the trace
     * never diverges from the real resolution.
     */
    suspend fun resolveWithTrace(
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
        asOfDate: String? = null,
    ): PriceResolutionTrace {
        val resolution = resolve(
            channel, productId, quantity, fallbackUnitPrice, currency,
            variantSku, customerId, customerGroupId, customerType, pincode, asOfDate,
        )
        val asOf = parseInstantOrNull(asOfDate) ?: Clock.System.now()
        val zoneId = pincode?.let { zoneIdForPincode(it) }
        val active = priceListDao.getActivePriceLists()
            .map { it.toPriceList() }
            .filter { it.channel == channel }
            .sortedWith(compareByDescending<PriceList> { it.specificity() }.thenByDescending { it.priority })

        val candidates = active.map { list ->
            val matches = list.matchesTargeting(customerId, customerGroupId, customerType, zoneId)
            val hasItem = matches && bestItemFor(list.uid, productId, variantSku, asOf) != null
            val won = list.uid == resolution.matchedPriceListUid
            PriceCandidate(
                uid = list.uid,
                name = list.name,
                channel = list.channel,
                matched = matches,
                won = won,
                hasItemForProduct = hasItem,
                specificity = list.specificity(),
                priority = list.priority,
                reason = candidateReason(list, matches, hasItem, won),
            )
        }
        return PriceResolutionTrace(resolution, candidates, active.size)
    }

    private suspend fun zoneIdForPincode(pincode: String): String? =
        geoZoneDao.getActiveGeoZones().map { it.toGeoZone() }.firstOrNull { it.members.contains(pincode) }?.uid

    private suspend fun bestItemFor(
        priceListId: String,
        productId: String,
        variantSku: String?,
        asOf: Instant,
    ): PriceListItem? {
        val items = priceListItemDao.getItemsForPriceList(priceListId)
            .map { it.toPriceListItem() }
            .filter { it.productId == productId && it.isEffectiveAt(asOf) }
        if (items.isEmpty()) return null
        // Variant > base > any, then the most-recently-effective version at [asOf]
        // (mirrors backend PricingResolutionServiceImpl.pickItem).
        val variantMatches = if (variantSku != null) items.filter { it.variantSku == variantSku } else emptyList()
        val baseMatches = items.filter { it.variantSku == null }
        val pool = variantMatches.ifEmpty { baseMatches.ifEmpty { items } }
        return pool.maxByOrNull { parseInstantOrNull(it.effectiveFrom) ?: Instant.DISTANT_PAST }
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

/**
 * True when this item's effective window contains [asOf]: from `effectiveFrom` (inclusive) up to
 * `effectiveTo` (exclusive). A null/blank bound is open on that side, so a legacy item with no
 * effective dates is always effective. Unparseable bounds are treated as open (fail-open).
 */
@OptIn(ExperimentalTime::class)
internal fun PriceListItem.isEffectiveAt(asOf: Instant): Boolean {
    parseInstantOrNull(effectiveFrom)?.let { if (asOf < it) return false }
    parseInstantOrNull(effectiveTo)?.let { if (asOf >= it) return false }
    return true
}

/** Parse an ISO-8601 instant string, or null when blank/unparseable. */
@OptIn(ExperimentalTime::class)
internal fun parseInstantOrNull(iso: String?): Instant? =
    if (iso.isNullOrBlank()) null else runCatching { Instant.parse(iso) }.getOrNull()

/** Most specific targeting dimension set on this list, in plain language (for the Explain trace). */
internal fun PriceList.mostSpecificDimension(): String = when {
    !customerId.isNullOrBlank() -> "this customer"
    !customerGroupId.isNullOrBlank() -> "customer group"
    !productGroupId.isNullOrBlank() -> "product group"
    !brandId.isNullOrBlank() -> "brand"
    !categoryId.isNullOrBlank() -> "category"
    !geoZoneId.isNullOrBlank() -> "geo zone"
    !customerType.isNullOrBlank() -> "customer type"
    attributePredicates.isNotEmpty() -> "attribute rules"
    else -> "channel default"
}

/** Short, plain-language verdict for one candidate list in the resolution trace. */
internal fun candidateReason(list: PriceList, matched: Boolean, hasItem: Boolean, won: Boolean): String = when {
    won -> "Won — most specific match (${list.mostSpecificDimension()})."
    matched && !hasItem -> "Matches, but has no price for this product."
    matched -> "Matches, but a more specific list won."
    else -> "Excluded — targeting doesn't match this context."
}
