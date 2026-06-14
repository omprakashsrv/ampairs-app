package com.ampairs.tax.data.repository

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.data.api.TaxConfigurationApi
import com.ampairs.tax.data.db.dao.TaxConfigurationDao
import dev.zacsweers.metro.Inject
import com.ampairs.tax.data.db.entity.toEntity
import com.ampairs.tax.data.db.entity.toDomain
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxStrategy
import com.ampairs.tax.domain.model.TaxCodeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

/**
 * Tax Configuration Repository - Workspace tax settings
 */
@Inject
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
     * Get workspace tax configuration (offline + sync fallback)
     */
    suspend fun getConfiguration(): Result<TaxConfiguration> {
        return try {
            // Try local database first
            val localConfig = taxConfigurationDao.getConfiguration()?.toDomain()
            if (localConfig != null) {
                return Result.success(localConfig)
            }

            // If not found locally, try to fetch from server
            val result = taxConfigApi.getWorkspaceConfiguration()
            if (result.isSuccess) {
                val serverConfig = result.getOrThrow()
                taxConfigurationDao.insert(serverConfig.toEntity())
                Result.success(serverConfig)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Configuration not found"))
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxConfigurationRepository.getConfiguration")
            Result.failure(e)
        }
    }

    /**
     * Create workspace tax configuration (database-first with background sync)
     */
    suspend fun createConfiguration(
        countryCode: String,
        taxStrategy: TaxStrategy,
        defaultTaxCodeSystem: TaxCodeType,
        industry: String? = null,
        autoSubscribeNewCodes: Boolean = false
    ): Result<TaxConfiguration> {
        return try {
            val now = kotlin.time.Clock.System.now()
            val config = TaxConfiguration(
                id = "",  // Server will generate
                countryCode = countryCode,
                taxStrategy = taxStrategy,
                defaultTaxCodeSystem = defaultTaxCodeSystem,
                taxJurisdictions = emptyList(),
                industry = industry,
                autoSubscribeNewCodes = autoSubscribeNewCodes,
                syncedAt = now
            )

            // Save to local database first
            taxConfigurationDao.insert(config.toEntity())

            // Try to create on server
            try {
                val result = taxConfigApi.createWorkspaceConfiguration(config)
                if (result.isSuccess) {
                    val serverConfig = result.getOrThrow()
                    taxConfigurationDao.insert(serverConfig.toEntity())
                    Result.success(serverConfig)
                } else {
                    // Keep local version but mark as failed to sync
                    Result.success(config)
                }
            } catch (e: Exception) {
                // Network error - keep local version
                ErrorTracking.captureException(
                    e,
                    "TaxConfigurationRepository.createConfiguration.sync"
                )
                Result.success(config)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxConfigurationRepository.createConfiguration")
            Result.failure(e)
        }
    }

    /**
     * Update workspace tax configuration (database-first with background sync)
     */
    suspend fun updateConfiguration(
        countryCode: String,
        taxStrategy: TaxStrategy,
        defaultTaxCodeSystem: TaxCodeType,
        industry: String? = null,
        autoSubscribeNewCodes: Boolean = false
    ): Result<TaxConfiguration> {
        return try {
            val now = kotlin.time.Clock.System.now()
            // Get existing config to preserve ID
            val existingConfig = taxConfigurationDao.getConfiguration()
            val config = TaxConfiguration(
                id = existingConfig?.id ?: "",
                countryCode = countryCode,
                taxStrategy = taxStrategy,
                defaultTaxCodeSystem = defaultTaxCodeSystem,
                taxJurisdictions = emptyList(),
                industry = industry,
                autoSubscribeNewCodes = autoSubscribeNewCodes,
                syncedAt = now
            )

            // Save to local database first
            taxConfigurationDao.insert(config.toEntity())

            // Try to sync with server in background
            try {
                val result = taxConfigApi.updateWorkspaceConfiguration(config)

                if (result.isSuccess) {
                    val serverConfig = result.getOrThrow()
                    taxConfigurationDao.insert(serverConfig.toEntity())
                    Result.success(serverConfig)
                } else {
                    // Keep local version
                    Result.success(config)
                }
            } catch (e: Exception) {
                // Network error - keep local version
                ErrorTracking.captureException(
                    e,
                    "TaxConfigurationRepository.updateConfiguration.sync"
                )
                Result.success(config)
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
            val timestamp = kotlin.time.Clock.System.now()
            taxConfigurationDao.updateSyncTime(timestamp)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxConfigurationRepository.updateSyncTime")
        }
    }
}
