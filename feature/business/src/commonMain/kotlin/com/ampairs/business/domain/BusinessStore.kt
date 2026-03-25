package com.ampairs.business.domain

import com.ampairs.business.data.repository.BusinessRepository
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

@OptIn(ExperimentalStoreApi::class)
class BusinessStore(
    private val repository: BusinessRepository
) {

    private val store: Store<Unit, Business> = StoreBuilder
        .from(
            fetcher = Fetcher.of {
                repository.fetchFromRemote().getOrThrow()
            },
            sourceOfTruth = SourceOfTruth.of<Unit, Business, Business>(
                reader = {
                    repository.observeBusiness()
                },
                writer = { _, business ->
                    repository.saveLocal(business, markSynced = true)
                },
                deleteAll = {
                    repository.clearLocal()
                }
            )
        )
        .build()

    /**
     * Stream business profile with optional refresh flag.
     */
    fun streamBusiness(refresh: Boolean = false): Flow<StoreReadResponse<Business>> {
        return store.stream(StoreReadRequest.cached(Unit, refresh))
    }

    /**
     * Force refresh business profile from server.
     */
    suspend fun refresh(): Result<Business> {
        return repository.fetchFromRemote()
    }

    /**
     * Observe cached business profile without triggering network fetch.
     */
    fun observeBusiness(): Flow<Business?> = repository.observeBusiness()

    /**
     * Upsert business profile using offline-first repository.
     */
    suspend fun upsertBusiness(business: Business): Result<Business> {
        return repository.upsertBusiness(business)
    }

    /**
     * Attempt to sync any pending local changes.
     */
    suspend fun syncPending(): Result<Boolean> = repository.syncPending()

    /**
     * Clear cached store data. Useful when workspace context changes.
     * Clears database first to prevent race conditions during store clearing.
     */
    suspend fun clearCache() {
        repository.clearLocal()  // Clear database first
        store.clear()            // Then clear cache
    }
}
