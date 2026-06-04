package com.ampairs.customer.domain

import dev.zacsweers.metro.Inject
import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.api.BulkImportRequest
import com.ampairs.customer.data.api.BulkImportResponse
import com.ampairs.customer.data.db.StateDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toDomainList
import com.ampairs.customer.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class StateStore(
    private val customerApi: CustomerApi,
    private val stateDao: StateDao
) {

    fun observeStates(): Flow<List<State>> =
        stateDao.getAllStates().map { it.toDomainList() }

    fun searchStatesFlow(query: String): Flow<List<State>> =
        stateDao.searchStates(query).map { it.toDomainList() }

    suspend fun getStateById(stateId: String): State? =
        stateDao.getStateById(stateId)?.toDomain()

    suspend fun syncStates(): Result<Int> {
        return try {
            val states = customerApi.getStates()
            stateDao.insertStates(states.map { it.toEntity() })
            Result.success(states.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importState(stateCode: String): Result<String> {
        return try {
            val result = customerApi.importState(stateCode)
            syncStates()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkImportStates(stateCodes: List<String>): Result<BulkImportResponse> {
        return try {
            val result = customerApi.bulkImportStates(BulkImportRequest(stateCodes))
            syncStates()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteState(stateId: String): Result<Unit> {
        return try {
            customerApi.deleteState(stateId)
            stateDao.deleteStateById(stateId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableStatesForImport(): Result<List<MasterState>> {
        return try {
            val masterStates = customerApi.getAvailableStatesForImport()
            Result.success(masterStates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
