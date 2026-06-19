package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.AddressRequest
import com.ampairs.ecom.data.api.EcomApi
import com.ampairs.ecom.data.db.dao.AddressDao
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.domain.EcomLogger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Saved delivery addresses — offline-first CRUD with push/pull (contract §6, §8).
 */
@Inject
class AddressRepository(
    private val api: EcomApi,
    private val addressDao: AddressDao,
) {
    fun observeAddresses(): Flow<List<CustomerAddressEntity>> = addressDao.observeAll()

    /** Database-first write: persist locally as unsynced; CentralSyncService pushes it. */
    suspend fun saveLocally(address: CustomerAddressEntity): Result<CustomerAddressEntity> {
        require(address.uid.isNotBlank()) { "UID must be set by ViewModel" }
        addressDao.upsert(address.copy(synced = 0))
        return Result.success(address)
    }

    suspend fun markDeletedLocally(uid: String): Result<Unit> {
        val existing = addressDao.byId(uid) ?: return Result.success(Unit)
        addressDao.upsert(existing.copy(active = 0, synced = 0))
        return Result.success(Unit)
    }

    suspend fun pullFromServer(): Result<Int> {
        val result = api.getAddresses()
        return result.fold(
            onSuccess = { list ->
                addressDao.upsertAll(list.map { it.toEntity(synced = 1) })
                // Server is authoritative: drop synced rows it no longer returns (heals stale
                // duplicates from older client-uid divergence). Only prune when the server
                // actually returned addresses, to avoid wiping on a transient empty response.
                if (list.isNotEmpty()) addressDao.deleteSyncedNotIn(list.map { it.uid })
                Result.success(list.size)
            },
            onFailure = {
                EcomLogger.w("Address", "pull failed", it)
                Result.failure(it)
            },
        )
    }

    suspend fun pushPendingToServer(): Result<Int> {
        val pending = addressDao.unsynced()
        if (pending.isEmpty()) return Result.success(0)
        var pushed = 0
        var failures = 0
        for (row in pending) {
            val outcome = if (row.active == 0) {
                api.deleteAddress(row.uid).onSuccess { addressDao.hardDelete(row.uid) }
            } else {
                val request = row.toRequest()
                // Try update first, fall back to create — mirrors the standard push pattern.
                api.updateAddress(row.uid, request)
                    .recoverCatching { api.createAddress(request).getOrThrow() }
                    .onSuccess { resp ->
                        if (resp.uid.isNotBlank() && resp.uid != row.uid) {
                            // Server assigned a different uid (legacy data created before the server
                            // honored client uids). Adopt it so the next pull upserts the same row
                            // instead of inserting a duplicate.
                            addressDao.hardDelete(row.uid)
                            addressDao.upsert(row.copy(uid = resp.uid, synced = 1))
                        } else {
                            addressDao.upsert(row.copy(synced = 1))
                        }
                    }
                    .map { }
            }
            outcome.onSuccess { pushed++ }.onFailure { failures++ }
        }
        return if (pushed == 0 && failures > 0) {
            Result.failure(Exception("Failed to push pending addresses — will retry"))
        } else {
            Result.success(pushed)
        }
    }

    private fun CustomerAddressEntity.toRequest() = AddressRequest(
        uid = uid,
        label = label,
        addressLine1 = address_line1,
        addressLine2 = address_line2,
        city = city,
        state = state,
        pinCode = pin_code,
        country = country,
        phone = phone,
        isDefault = is_default == 1,
        latitude = latitude,
        longitude = longitude,
    )
}
