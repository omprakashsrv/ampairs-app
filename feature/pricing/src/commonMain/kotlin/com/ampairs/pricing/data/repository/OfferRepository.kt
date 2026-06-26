package com.ampairs.pricing.data.repository

import com.ampairs.pricing.data.db.dao.OfferDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toOffer
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.util.PricingLogger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local-only data access for offers/promotions. There is no backend offer resource yet, so unlike
 * the price-list repository this one does NOT flag entities for sync — writes are persisted locally
 * in the workspace-scoped OffersDatabase only. When the backend lands, add SyncStateDao +
 * markPendingPush(SyncEntity.OFFER) and an OfferSyncDelegate (see /offline-sync).
 */
@Inject
class OfferRepository(
    private val offerDao: OfferDao,
) {
    private val tag = "OfferRepository"

    fun observeOffers(): Flow<List<Offer>> =
        offerDao.getAllOffers().map { rows -> rows.map { it.toOffer() } }

    suspend fun getOffer(id: String): Offer? = offerDao.getOfferById(id)?.toOffer()

    /** Persist an offer locally. UID must be set by the ViewModel. */
    suspend fun saveOffer(offer: Offer): Result<Offer> {
        require(offer.uid.isNotBlank()) { "Offer UID must be set by the ViewModel" }
        return runCatching {
            offerDao.insertOffer(offer.toEntity())
            offer
        }.onFailure { PricingLogger.e(tag, "Failed to save offer", it) }
    }

    /** Soft-delete an offer locally (active = false). */
    suspend fun deleteOffer(id: String): Result<Unit> = runCatching {
        offerDao.getOfferById(id)?.let { offerDao.insertOffer(it.copy(active = false)) }
        Unit
    }.onFailure { PricingLogger.e(tag, "Failed to delete offer", it) }
}
