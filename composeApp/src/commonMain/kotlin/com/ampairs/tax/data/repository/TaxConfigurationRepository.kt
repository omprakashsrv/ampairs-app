package com.ampairs.tax.data.repository

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxConfigurationDao
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.data.db.entity.toDomain
import com.ampairs.tax.domain.model.TaxConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Tax Configuration Repository - Workspace tax settings
 */
@OptIn(ExperimentalTime::class)
class TaxConfigurationRepository(
    private val taxConfigApi: TaxConfigurationApi,
    private val taxConfigurationDao: TaxConfigurationDao,
) {

    /**
     * Observe workspace tax configuration (offline)
     */
    fun observeConfiguration(): Flow<TaxConfiguration?> {
        return taxConfigurationDao.observeConfiguration()
            .map { entity -> entity?.toDomain() }
    }

    /**
     * Get workspace tax configuration (offline)
     */
    suspend fun getConfiguration(): TaxConfiguration? {
        return taxConfigurationDao.getConfiguration()?.toDomain()
    }

    /**
     * Update workspace tax configuration (database-first with background sync)
     */
    suspend fun updateConfiguration(config: TaxConfiguration): Result<TaxConfiguration> {
        return try {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val configWithTimestamp = config.copy(
                syncedAt = now
            )

            // Save to local database first
            taxConfigurationDao.insert(configWithTimestamp.toEntity())

            // Try to sync with server in background
            try {
                val result = taxConfigApi.updateWorkspaceConfiguration(configWithTimestamp)

                if (result.isSuccess) {
                    val serverConfig = result.getOrThrow()
                    taxConfigurationDao.insert(serverConfig.toEntity())
                    Result.success(serverConfig)
                } else {
                    // Keep local version
                    Result.success(configWithTimestamp)
                }
            } catch (e: Exception) {
                // Network error - keep local version
                ErrorTracking.captureException(
                    e,
                    "TaxConfigurationRepository.updateConfiguration.sync"
                )
                Result.success(configWithTimestamp)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxConfigurationRepository.updateConfiguration")
            Result.failure(e)
        }
    }

    /**
     * Sync workspace tax configuration from server
     */
    suspend fun syncConfiguration(): Result<TaxConfiguration> {
        return try {
            val result = taxConfigApi.getWorkspaceConfiguration()

            if (result.isSuccess) {
                val config = result.getOrThrow()

                // Save to local database
                taxConfigurationDao.insert(config.toEntity())

                Result.success(config)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Sync failed"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxConfigurationRepository.syncConfiguration")
            Result.failure(e)
        }
    }

    /**
     * Update sync timestamp
     */
    suspend fun updateSyncTime() {
        try {
            val timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
            taxConfigurationDao.updateSyncTime(timestamp)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxConfigurationRepository.updateSyncTime")
        }
    }
}
