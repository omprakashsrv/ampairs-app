package com.ampairs.tax.calculation

import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxCalculationResult
import com.ampairs.tax.calculation.model.TaxJurisdiction
import com.ampairs.tax.calculation.model.TransactionContext
import com.ampairs.tax.calculation.model.TransactionType
import com.ampairs.tax.data.repository.TaxConfigurationRepository
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.domain.model.TaxStrategy
import com.ampairs.tax.testutil.FakeTaxConfigurationApi
import com.ampairs.tax.testutil.FakeTaxConfigurationDao
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tests for [TaxCalculationEngine] — the orchestrator that selects a strategy from the workspace's
 * configured [TaxStrategy], validates the tax-code format, and delegates the calculation.
 *
 * The strategy collaborators are faked via the [ITaxCalculationStrategy] interface (recording
 * fakes), and the configuration repository is the real one over an in-memory config DAO.
 */
@OptIn(ExperimentalTime::class)
class TaxCalculationEngineTest {

    private val configDao = FakeTaxConfigurationDao()
    private val api = FakeTaxConfigurationApi()
    private val configRepository = TaxConfigurationRepository(api, configDao)

    private fun seedConfig(strategy: TaxStrategy) {
        configDao.seed(
            TaxConfiguration(
                id = "cfg",
                countryCode = "IN",
                taxStrategy = strategy,
                defaultTaxCodeSystem = TaxCodeType.HSN_CODE,
                syncedAt = Instant.fromEpochMilliseconds(0),
            ),
        )
    }

    private fun request() = TaxCalculationRequest(
        taxCode = "1234",
        taxCodeType = TaxCodeType.HSN_CODE,
        baseAmount = 100.0,
        quantity = 1,
        sourceLocation = TaxJurisdiction(country = "IN", state = "MH"),
        destinationLocation = TaxJurisdiction(country = "IN", state = "MH"),
        transactionType = TransactionType.B2B,
        transactionContext = TransactionContext(),
    )

    private fun result(): TaxCalculationResult = TaxCalculationResult(
        taxCode = "1234",
        codeType = TaxCodeType.HSN_CODE,
        baseAmount = 100.0,
        quantity = 1,
        taxComponents = emptyList(),
        totalTaxAmount = 18.0,
        totalAmount = 118.0,
        jurisdiction = TaxJurisdiction(country = "IN", state = "MH"),
        transactionContext = TransactionContext(),
        countryCode = "IN",
    )

    @Test
    fun delegatesToConfiguredStrategy() = runTest {
        seedConfig(TaxStrategy.INDIA_GST)
        val fake = RecordingStrategy(result = Result.success(result()))
        val engine = TaxCalculationEngine(configRepository, mapOf(TaxStrategy.INDIA_GST to fake))

        val r = engine.calculateTax(request())

        assertTrue(r.isSuccess)
        assertEquals(18.0, r.getOrThrow().totalTaxAmount, 0.001)
        assertEquals(1, fake.calculateCallCount)
    }

    @Test
    fun missingConfiguration_isFailure_andSkipsStrategy() = runTest {
        // No config seeded; API also returns failure → repo.getConfiguration() fails.
        val fake = RecordingStrategy(result = Result.success(result()))
        val engine = TaxCalculationEngine(configRepository, mapOf(TaxStrategy.INDIA_GST to fake))

        val r = engine.calculateTax(request())

        assertTrue(r.isFailure)
        assertEquals(0, fake.calculateCallCount)
    }

    @Test
    fun unsupportedStrategy_isFailure() = runTest {
        seedConfig(TaxStrategy.USA_SALES_TAX)   // configured strategy not present in the map
        val engine = TaxCalculationEngine(configRepository, mapOf(TaxStrategy.INDIA_GST to RecordingStrategy()))

        val r = engine.calculateTax(request())

        assertTrue(r.isFailure)
    }

    @Test
    fun invalidTaxCodeFormat_isFailure_andSkipsCalculate() = runTest {
        seedConfig(TaxStrategy.INDIA_GST)
        val fake = RecordingStrategy(validates = false, result = Result.success(result()))
        val engine = TaxCalculationEngine(configRepository, mapOf(TaxStrategy.INDIA_GST to fake))

        val r = engine.calculateTax(request())

        assertTrue(r.isFailure)
        assertEquals(0, fake.calculateCallCount)
    }

    @Test
    fun strategyFailurePropagates() = runTest {
        seedConfig(TaxStrategy.INDIA_GST)
        val fake = RecordingStrategy(result = Result.failure(Exception("boom")))
        val engine = TaxCalculationEngine(configRepository, mapOf(TaxStrategy.INDIA_GST to fake))

        val r = engine.calculateTax(request())

        assertTrue(r.isFailure)
        assertEquals(1, fake.calculateCallCount)
    }

    @Test
    fun getCurrentStrategy_returnsConfigured() = runTest {
        seedConfig(TaxStrategy.UK_VAT)
        val engine = TaxCalculationEngine(configRepository, emptyMap())
        assertEquals(TaxStrategy.UK_VAT, engine.getCurrentStrategy())
    }

    @Test
    fun getCurrentStrategy_nullWhenNoConfig() = runTest {
        val engine = TaxCalculationEngine(configRepository, emptyMap())
        assertEquals(null, engine.getCurrentStrategy())
    }

    @Test
    fun isStrategyAvailable_andSupportedList() {
        val engine = TaxCalculationEngine(
            configRepository,
            mapOf(TaxStrategy.INDIA_GST to RecordingStrategy(), TaxStrategy.UK_VAT to RecordingStrategy()),
        )
        assertTrue(engine.isStrategyAvailable(TaxStrategy.INDIA_GST))
        assertFalse(engine.isStrategyAvailable(TaxStrategy.EU_VAT))
        assertEquals(setOf(TaxStrategy.INDIA_GST, TaxStrategy.UK_VAT), engine.getSupportedStrategies().toSet())
    }
}

/** Hand-written [ITaxCalculationStrategy] recording fake (MockK is JVM-only / not KMP-safe). */
private class RecordingStrategy(
    private val validates: Boolean = true,
    private val result: Result<TaxCalculationResult> =
        Result.failure(Exception("not configured")),
) : ITaxCalculationStrategy {
    var calculateCallCount = 0
    override val countryCode: String = "TEST"
    override val strategyName: String = "TEST_STRATEGY"

    override suspend fun calculateTax(request: TaxCalculationRequest): Result<TaxCalculationResult> {
        calculateCallCount++
        return result
    }

    override fun validateTaxCode(code: String, codeType: String): Boolean = validates
}
