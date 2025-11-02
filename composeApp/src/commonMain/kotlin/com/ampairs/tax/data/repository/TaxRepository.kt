package com.ampairs.tax.data.repository

import com.ampairs.tax.data.api.TaxApi
import com.ampairs.tax.data.db.HsnCodeDao
import com.ampairs.tax.data.db.TaxRateDao
import com.ampairs.tax.data.db.toDomain
import com.ampairs.tax.data.db.toEntity
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TaxRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)

class TaxRepository(
    private val taxApi: TaxApi,
    private val hsnCodeDao: HsnCodeDao,
    private val taxRateDao: TaxRateDao
) {

    // HSN Code operations
    fun getAllHsnCodes(): Flow<List<HsnCode>> {
        return hsnCodeDao.getAllActiveHsnCodes()
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getHsnCodeById(id: String): HsnCode? {
        return hsnCodeDao.getHsnCodeById(id)?.toDomain()
    }

    suspend fun getHsnCodeByCode(hsnCode: String): HsnCode? {
        return hsnCodeDao.getHsnCodeByCode(hsnCode)?.toDomain()
    }

    suspend fun searchHsnCodes(query: String, limit: Int = 50): List<HsnCode> {
        return hsnCodeDao.searchHsnCodes(query, limit).map { it.toDomain() }
    }

    suspend fun getHsnCodesByCategory(category: String): List<HsnCode> {
        return hsnCodeDao.getHsnCodesByCategory(category).map { it.toDomain() }
    }

    suspend fun createHsnCode(hsnCode: HsnCode): Result<HsnCode> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val hsnCodeWithTimestamp = hsnCode.copy(
                createdAt = now,
                updatedAt = now
            )

            // Save to local database first
            hsnCodeDao.insertHsnCode(hsnCodeWithTimestamp.toEntity())

            // Try to sync with server
            try {
                val workspaceId = "current_workspace" // TODO: Get from workspace context
                val result = taxApi.createHsnCode(workspaceId, hsnCodeWithTimestamp)

                if (result.isSuccess) {
                    val serverHsnCode = result.getOrThrow()
                    hsnCodeDao.updateSyncStatus(serverHsnCode.id, "SYNCED", now)
                    Result.success(serverHsnCode)
                } else {
                    // Mark as pending sync
                    hsnCodeDao.updateSyncStatus(hsnCodeWithTimestamp.id, "PENDING", now)
                    Result.success(hsnCodeWithTimestamp)
                }
            } catch (e: Exception) {
                // Network error - mark as pending sync
                hsnCodeDao.updateSyncStatus(hsnCodeWithTimestamp.id, "PENDING", now)
                Result.success(hsnCodeWithTimestamp)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHsnCode(hsnCode: HsnCode): Result<HsnCode> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val updatedHsnCode = hsnCode.copy(updatedAt = now)

            // Update local database
            hsnCodeDao.updateHsnCode(updatedHsnCode.toEntity())

            // Try to sync with server
            try {
                val workspaceId = "current_workspace" // TODO: Get from workspace context
                val result = taxApi.updateHsnCode(workspaceId, hsnCode.id, updatedHsnCode)

                if (result.isSuccess) {
                    val serverHsnCode = result.getOrThrow()
                    hsnCodeDao.updateSyncStatus(serverHsnCode.id, "SYNCED", now)
                    Result.success(serverHsnCode)
                } else {
                    hsnCodeDao.updateSyncStatus(updatedHsnCode.id, "PENDING", now)
                    Result.success(updatedHsnCode)
                }
            } catch (e: Exception) {
                hsnCodeDao.updateSyncStatus(updatedHsnCode.id, "PENDING", now)
                Result.success(updatedHsnCode)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHsnCode(id: String): Result<Unit> {
        return try {
            // Mark as inactive locally
            hsnCodeDao.deactivateHsnCode(id)

            // Try to delete from server
            try {
                val workspaceId = "current_workspace" // TODO: Get from workspace context
                val result = taxApi.deleteHsnCode(workspaceId, id)

                if (result.isSuccess) {
                    hsnCodeDao.deleteHsnCodeById(id)
                    Result.success(Unit)
                } else {
                    val now = Clock.System.now().toEpochMilliseconds()
                    hsnCodeDao.updateSyncStatus(id, "DELETE_PENDING", now)
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                val now = Clock.System.now().toEpochMilliseconds()
                hsnCodeDao.updateSyncStatus(id, "DELETE_PENDING", now)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Tax Rate operations
    fun getAllTaxRates(): Flow<List<TaxRate>> {
        return taxRateDao.getAllActiveTaxRates()
            .map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun getTaxRateById(id: String): TaxRate? {
        return taxRateDao.getTaxRateById(id)?.toDomain()
    }

    suspend fun getEffectiveTaxRate(
        hsnCode: String,
        businessType: BusinessType,
        effectiveDate: Long = Clock.System.now().toEpochMilliseconds()
    ): TaxRate? {
        return taxRateDao.getEffectiveTaxRate(
            hsnCode = hsnCode,
            businessType = businessType.name,
            effectiveDate = effectiveDate
        )?.toDomain()
    }

    suspend fun getTaxRatesByHsnCode(hsnCode: String): List<TaxRate> {
        return taxRateDao.getTaxRatesByHsnCode(hsnCode).map { it.toDomain() }
    }

    suspend fun createTaxRate(taxRate: TaxRate): Result<TaxRate> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val taxRateWithTimestamp = taxRate.copy(
                createdAt = now,
                updatedAt = now
            )

            // Close any open tax rates for the same HSN and business type
            taxRateDao.closeOpenTaxRates(
                hsnCode = taxRate.hsnCode,
                businessType = taxRate.businessType.name,
                effectiveTo = taxRate.effectiveFrom - 1,
                excludeId = taxRate.id
            )

            // Save to local database
            taxRateDao.insertTaxRate(taxRateWithTimestamp.toEntity())

            // Try to sync with server
            try {
                val workspaceId = "current_workspace" // TODO: Get from workspace context
                val result = taxApi.createTaxRate(workspaceId, taxRateWithTimestamp)

                if (result.isSuccess) {
                    val serverTaxRate = result.getOrThrow()
                    taxRateDao.updateSyncStatus(serverTaxRate.id, "SYNCED", now)
                    Result.success(serverTaxRate)
                } else {
                    taxRateDao.updateSyncStatus(taxRateWithTimestamp.id, "PENDING", now)
                    Result.success(taxRateWithTimestamp)
                }
            } catch (e: Exception) {
                taxRateDao.updateSyncStatus(taxRateWithTimestamp.id, "PENDING", now)
                Result.success(taxRateWithTimestamp)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // Try server calculation first
            try {
                val workspaceId = "current_workspace" // TODO: Get from workspace context
                val result = taxApi.calculateTax(workspaceId, request)
                if (result.isSuccess) {
                    return result
                }
            } catch (e: Exception) {
                // Fall back to local calculation
            }

            // Local tax calculation
            val taxRate = getEffectiveTaxRate(
                hsnCode = request.hsnCode,
                businessType = request.businessType
            ) ?: return Result.failure(Exception("Tax rate not found for HSN code: ${request.hsnCode}"))

            val isIntraState = request.sourceState == request.destinationState
            val baseAmount = request.baseAmount * request.quantity

            val gstAmount = baseAmount * taxRate.ratePercentage / 100
            val cessAmount = taxRate.cessRate?.let { rate ->
                baseAmount * rate / 100
            } ?: taxRate.cessAmountPerUnit?.let { amount ->
                amount * request.quantity
            } ?: 0.0

            val result = if (isIntraState) {
                // CGST + SGST
                val cgstAmount = gstAmount / 2
                val sgstAmount = gstAmount / 2

                TaxCalculationResult(
                    hsnCode = request.hsnCode,
                    baseAmount = baseAmount,
                    quantity = request.quantity,
                    cgstAmount = cgstAmount,
                    sgstAmount = sgstAmount,
                    igstAmount = 0.0,
                    cessAmount = cessAmount,
                    totalTaxAmount = gstAmount + cessAmount,
                    totalAmount = baseAmount + gstAmount + cessAmount,
                    taxBreakdown = emptyList(), // TODO: Implement breakdown
                    transactionType = request.transactionType,
                    isIntraState = true
                )
            } else {
                // IGST
                TaxCalculationResult(
                    hsnCode = request.hsnCode,
                    baseAmount = baseAmount,
                    quantity = request.quantity,
                    cgstAmount = 0.0,
                    sgstAmount = 0.0,
                    igstAmount = gstAmount,
                    cessAmount = cessAmount,
                    totalTaxAmount = gstAmount + cessAmount,
                    totalAmount = baseAmount + gstAmount + cessAmount,
                    taxBreakdown = emptyList(), // TODO: Implement breakdown
                    transactionType = request.transactionType,
                    isIntraState = false
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncData(): Result<Unit> {
        return try {
            // Sync unsynced HSN codes
            val unsyncedHsnCodes = hsnCodeDao.getUnsyncedHsnCodes()
            unsyncedHsnCodes.forEach { entity ->
                val hsnCode = entity.toDomain()
                when (entity.syncStatus) {
                    "PENDING" -> {
                        if (entity.createdAt == entity.updatedAt) {
                            createHsnCode(hsnCode)
                        } else {
                            updateHsnCode(hsnCode)
                        }
                    }
                    "DELETE_PENDING" -> {
                        deleteHsnCode(hsnCode.id)
                    }
                }
            }

            // Sync unsynced tax rates
            val unsyncedTaxRates = taxRateDao.getUnsyncedTaxRates()
            unsyncedTaxRates.forEach { entity ->
                val taxRate = entity.toDomain()
                when (entity.syncStatus) {
                    "PENDING" -> {
                        createTaxRate(taxRate)
                    }
                    "DELETE_PENDING" -> {
                        // TODO: Implement delete tax rate
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}