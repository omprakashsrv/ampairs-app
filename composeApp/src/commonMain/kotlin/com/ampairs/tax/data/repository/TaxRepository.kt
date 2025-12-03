package com.ampairs.tax.data.repository

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.data.api.TaxApi
import com.ampairs.tax.data.db.HsnCodeDao
import com.ampairs.tax.data.db.TaxRateDao
import com.ampairs.tax.data.db.toDomain
import com.ampairs.tax.data.db.toEntity
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxBreakdownItem
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TaxRate
import com.ampairs.tax.domain.TaxType
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)

class TaxRepository(
    private val taxApi: TaxApi,
    private val hsnCodeDao: HsnCodeDao,
    private val taxRateDao: TaxRateDao
) {

    /**
     * Get current workspace ID from workspace context manager
     * @throws IllegalStateException if no workspace is selected
     */
    private fun getWorkspaceId(): String {
        return WorkspaceContextManager.getInstance().getCurrentWorkspaceId()
            ?: throw IllegalStateException("No workspace selected. Please select a workspace first.")
    }

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
                val workspaceId = getWorkspaceId()
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
                ErrorTracking.captureException(e, "TaxRepository.createHsnCode.sync")
                hsnCodeDao.updateSyncStatus(hsnCodeWithTimestamp.id, "PENDING", now)
                Result.success(hsnCodeWithTimestamp)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRepository.createHsnCode")
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
                val workspaceId = getWorkspaceId()
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
                ErrorTracking.captureException(e, "TaxRepository.updateHsnCode.sync")
                hsnCodeDao.updateSyncStatus(updatedHsnCode.id, "PENDING", now)
                Result.success(updatedHsnCode)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRepository.updateHsnCode")
            Result.failure(e)
        }
    }

    suspend fun deleteHsnCode(id: String): Result<Unit> {
        return try {
            // Mark as inactive locally
            hsnCodeDao.deactivateHsnCode(id)

            // Try to delete from server
            try {
                val workspaceId = getWorkspaceId()
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
                ErrorTracking.captureException(e, "TaxRepository.deleteHsnCode.sync")
                val now = Clock.System.now().toEpochMilliseconds()
                hsnCodeDao.updateSyncStatus(id, "DELETE_PENDING", now)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRepository.deleteHsnCode")
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
                val workspaceId = getWorkspaceId()
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
                ErrorTracking.captureException(e, "TaxRepository.createTaxRate.sync")
                taxRateDao.updateSyncStatus(taxRateWithTimestamp.id, "PENDING", now)
                Result.success(taxRateWithTimestamp)
            }
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRepository.createTaxRate")
            Result.failure(e)
        }
    }

    suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {
            // Try server calculation first
            try {
                val workspaceId = getWorkspaceId()
                val result = taxApi.calculateTax(workspaceId, request)
                if (result.isSuccess) {
                    return result
                }
            } catch (e: Exception) {
                // Fall back to local calculation - log but continue
                ErrorTracking.captureException(e, "TaxRepository.calculateTax.server")
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

                // Build tax breakdown
                val breakdown = buildList {
                    // CGST
                    add(TaxBreakdownItem(
                        taxType = TaxType.CGST,
                        ratePercentage = taxRate.ratePercentage / 2,
                        taxableAmount = baseAmount,
                        taxAmount = cgstAmount,
                        description = "CGST @ ${String.format("%.2f", taxRate.ratePercentage / 2)}%"
                    ))

                    // SGST
                    add(TaxBreakdownItem(
                        taxType = TaxType.SGST,
                        ratePercentage = taxRate.ratePercentage / 2,
                        taxableAmount = baseAmount,
                        taxAmount = sgstAmount,
                        description = "SGST @ ${String.format("%.2f", taxRate.ratePercentage / 2)}%"
                    ))

                    // CESS (if applicable)
                    if (cessAmount > 0) {
                        val cessDescription = if (taxRate.cessRate != null) {
                            "Cess @ ${String.format("%.2f", taxRate.cessRate)}%"
                        } else {
                            "Cess ₹${String.format("%.2f", taxRate.cessAmountPerUnit)} per unit"
                        }
                        add(TaxBreakdownItem(
                            taxType = TaxType.CESS,
                            ratePercentage = taxRate.cessRate ?: 0.0,
                            taxableAmount = baseAmount,
                            taxAmount = cessAmount,
                            description = cessDescription
                        ))
                    }
                }

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
                    taxBreakdown = breakdown,
                    transactionType = request.transactionType,
                    isIntraState = true
                )
            } else {
                // IGST
                // Build tax breakdown
                val breakdown = buildList {
                    // IGST
                    add(TaxBreakdownItem(
                        taxType = TaxType.IGST,
                        ratePercentage = taxRate.ratePercentage,
                        taxableAmount = baseAmount,
                        taxAmount = gstAmount,
                        description = "IGST @ ${String.format("%.2f", taxRate.ratePercentage)}%"
                    ))

                    // CESS (if applicable)
                    if (cessAmount > 0) {
                        val cessDescription = if (taxRate.cessRate != null) {
                            "Cess @ ${String.format("%.2f", taxRate.cessRate)}%"
                        } else {
                            "Cess ₹${String.format("%.2f", taxRate.cessAmountPerUnit)} per unit"
                        }
                        add(TaxBreakdownItem(
                            taxType = TaxType.CESS,
                            ratePercentage = taxRate.cessRate ?: 0.0,
                            taxableAmount = baseAmount,
                            taxAmount = cessAmount,
                            description = cessDescription
                        ))
                    }
                }

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
                    taxBreakdown = breakdown,
                    transactionType = request.transactionType,
                    isIntraState = false
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxRepository.calculateTax")
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
            ErrorTracking.captureException(e, "TaxRepository.syncData")
            Result.failure(e)
        }
    }
}