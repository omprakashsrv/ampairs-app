package com.ampairs.customer.domain

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.db.StateDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toDomainList
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.MasterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.StoreReadRequest
import com.ampairs.customer.util.CustomerLogger

class StateStore(
    private val customerApi: CustomerApi,
    private val stateDao: StateDao
) {
    val stateStore: Store<StateKey, List<State>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    emit(customerApi.getStates())
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { _: StateKey ->
                    stateDao.getAllStates().map { entities ->
                        entities.toDomainList()
                    }
                },
                writer = { _: StateKey, states: List<State> ->
                    stateDao.insertStates(states.map { it.toEntity() })
                }
            )
        )
        .build()

    fun searchStatesFlow(query: String): Flow<List<State>> {
        return stateDao.searchStates(query).map { entities ->
            entities.toDomainList()
        }
    }

    suspend fun getStateById(stateId: String): State? {
        return stateDao.getStateById(stateId)?.toDomain()
    }

    suspend fun importState(stateCode: String): Result<String> {
        return try {
            val result = customerApi.importState(stateCode)
            // Refresh states after import
            refreshStates()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bulkImportStates(stateCodes: List<String>): Result<com.ampairs.customer.data.api.BulkImportResponse> {
        return try {
            val request = com.ampairs.customer.data.api.BulkImportRequest(stateCodes)
            val result = customerApi.bulkImportStates(request)
            // Refresh states after import
            refreshStates()
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

    suspend fun getAvailableStatesForImport(workspaceId: String): Result<List<MasterState>> {
        return try {
            val masterStates = customerApi.getAvailableStatesForImport(workspaceId)
            Result.success(masterStates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun refreshStates() {
        try {
            val key = StateKey()
            stateStore.stream(StoreReadRequest.fresh(key))
        } catch (e: Exception) {
            // Log error but don't fail the import operation
            CustomerLogger.error("Failed to refresh states: ${e.message}")
        }
    }
}