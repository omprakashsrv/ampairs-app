package com.ampairs.pricing.data.repository

import com.ampairs.pricing.data.db.dao.OfferDao
import com.ampairs.pricing.data.db.entity.toEntity
import com.ampairs.pricing.data.db.entity.toOffer
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.util.PricingLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Local-only data access for offers/promotions. Writes are persisted to the workspace-scoped
 * OffersDatabase (`synced = false`) and flagged `PENDING_PUSH` via [SyncStateDao]; the
 * [com.ampairs.pricing.sync.OfferSyncDelegate] owns all server traffic (see /offline-sync). The
 * repository never touches the network.
 */
@Inject
class OfferRepository(
    private val offerDao: OfferDao,
    private val syncStateDao: SyncStateDao,
) {
    private val tag = "OfferRepository"

    fun observeOffers(): Flow<List<Offer>> =
        offerDao.getAllOffers().map { rows -> rows.map { it.toOffer() } }

    suspend fun getOffer(id: String): Offer? = offerDao.getOfferById(id)?.toOffer()

    /** Persist an offer locally (`synced = false`) and flag it for push. UID must be set by the ViewModel. */
    suspend fun saveOffer(offer: Offer): Result<Offer> {
        require(offer.uid.isNotBlank()) { "Offer UID must be set by the ViewModel" }
        return runCatching {
            offerDao.insertOffer(offer.toEntity().copy(synced = false))
            markPending()
            offer
        }.onFailure { PricingLogger.e(tag, "Failed to save offer", it) }
    }

    /**
     * Soft-delete an offer locally (`active = false, synced = false`) and flag it for push. The whole
     * [Offer] is re-encoded (not just the column) so the pushed payload — which the delegate decodes
     * from `payload_json` — also carries `active = false` and the deletion propagates to the server.
     */
    suspend fun deleteOffer(id: String): Result<Unit> = runCatching {
        offerDao.getOfferById(id)?.let { existing ->
            val deactivated = existing.toOffer().copy(active = false)
            offerDao.insertOffer(deactivated.toEntity().copy(synced = false))
            markPending()
        }
        Unit
    }.onFailure { PricingLogger.e(tag, "Failed to delete offer", it) }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.OFFER, Clock.System.now().toEpochMilliseconds())
}
