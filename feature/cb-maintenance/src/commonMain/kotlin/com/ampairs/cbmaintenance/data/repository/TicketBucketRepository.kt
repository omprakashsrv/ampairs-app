package com.ampairs.cbmaintenance.data.repository

import com.ampairs.cbmaintenance.data.db.dao.TicketBucketDao
import com.ampairs.cbmaintenance.data.db.entity.toTicketBucket
import com.ampairs.cbmaintenance.domain.model.TicketBucket
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local read access to the ticket-classification catalog. Reference data pulled by
 * [com.ampairs.cbmaintenance.sync.TicketBucketSyncDelegate]; the app never writes it, so this is a
 * read-only repository. The full active taxonomy is exposed; the raise-ticket ViewModel derives the
 * cascading Department → Category → Sub-category options from it.
 */
@Inject
class TicketBucketRepository(
    private val dao: TicketBucketDao,
) {
    fun observeBuckets(): Flow<List<TicketBucket>> =
        dao.getAllBuckets().map { list -> list.map { it.toTicketBucket() } }
}
