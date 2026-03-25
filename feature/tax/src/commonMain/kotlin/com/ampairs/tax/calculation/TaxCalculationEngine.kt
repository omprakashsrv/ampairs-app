package com.ampairs.tax.calculation

import com.ampairs.common.sentry.ErrorTracking
import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import com.ampairs.tax.domain.model.TaxStrategy

/**
 * Tax Calculation Engine - Orchestrator for tax calculation
 * Selects appropriate strategy based on workspace configuration
 */
class TaxCalculationEngine(
    private val taxConfigRepository: TaxConfigurationRepository,
    private val strategies: Map<TaxStrategy, ITaxCalculationStrategy>
) {

    /**
     * Calculate tax using workspace-configured strategy
     */
    suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        return try {

            val configResult = taxConfigRepository.getConfiguration()
            if (configResult.isFailure) {
                return Result.failure(Exception("Workspace tax configuration not found"))
            }
            val config = configResult.getOrThrow()

            // 2. Get strategy for workspace country
            val strategy = strategies[config.taxStrategy]
                ?: return Result.failure(Exception("Tax strategy '${config.taxStrategy}' not implemented"))

            // 3. Validate tax code format
            if (!strategy.validateTaxCode(request.taxCode, request.taxCodeType.name)) {
                return Result.failure(Exception("Invalid tax code format: ${request.taxCode}"))
            }

            // 4. Calculate using strategy
            strategy.calculateTax(request)
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCalculationEngine.calculateTax")
            Result.failure(e)
        }
    }

    /**
     * Get current workspace tax strategy
     */
    suspend fun getCurrentStrategy(): TaxStrategy? {
        return try {
            val result = taxConfigRepository.getConfiguration()
            if (result.isSuccess) result.getOrNull()?.taxStrategy else null
        } catch (e: Exception) {
            ErrorTracking.captureException(e, "TaxCalculationEngine.getCurrentStrategy")
            null
        }
    }

    /**
     * Check if strategy is available for country
     */
    fun isStrategyAvailable(strategy: TaxStrategy): Boolean {
        return strategies.containsKey(strategy)
    }

    /**
     * Get list of supported strategies
     */
    fun getSupportedStrategies(): List<TaxStrategy> {
        return strategies.keys.toList()
    }
}
