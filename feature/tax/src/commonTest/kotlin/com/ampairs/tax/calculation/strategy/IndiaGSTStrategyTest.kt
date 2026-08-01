package com.ampairs.tax.calculation.strategy

import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxJurisdiction
import com.ampairs.tax.calculation.model.TransactionContext
import com.ampairs.tax.calculation.model.TransactionType
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.testutil.FakeTaxCodeDao
import com.ampairs.tax.testutil.FakeTaxComponentDao
import com.ampairs.tax.testutil.FakeTaxComponentTypeDao
import com.ampairs.tax.testutil.FakeTaxConfigurationApi
import com.ampairs.tax.testutil.FakeTaxRuleDao
import com.ampairs.tax.testutil.componentRef
import com.ampairs.tax.testutil.composition
import com.ampairs.tax.testutil.taxRule
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Calculation-correctness tests for [IndiaGSTStrategy].
 *
 * India GST reads the rate straight from the rule's `componentComposition[scenario]` (it does NOT do
 * the workspace-component/component-type lookup the other strategies use), so only the rule DAO needs
 * seeding. Scenario is INTRA when source.state == destination.state, INTER otherwise.
 */
class IndiaGSTStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = IndiaGSTStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    private fun seedCement() {
        ruleDao.seedRule(
            taxRule(
                taxCode = "cement",
                jurisdiction = "MH",
                composition = mapOf(
                    "INTRA_STATE" to composition(
                        "INTRA_STATE",
                        listOf(
                            componentRef("c", "CGST", 14.0, order = 1),
                            componentRef("s", "SGST", 14.0, order = 2),
                        ),
                        totalRate = 28.0,
                    ),
                    "INTER_STATE" to composition(
                        "INTER_STATE",
                        listOf(componentRef("i", "IGST", 28.0, order = 1)),
                        totalRate = 28.0,
                    ),
                ),
            ),
        )
    }

    private fun request(
        sourceState: String,
        destState: String,
        baseAmount: Double = 385.0,
        quantity: Int = 2,
        taxCode: String = "cement",
        country: String = "IN",
    ) = TaxCalculationRequest(
        taxCode = taxCode,
        taxCodeType = TaxCodeType.HSN_CODE,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = country, state = sourceState),
        destinationLocation = TaxJurisdiction(country = country, state = destState),
        transactionType = TransactionType.B2B,
        transactionContext = TransactionContext(),
    )

    @Test
    fun intraState_splitsCgstAndSgst() = runTest {
        seedCement()
        val result = strategy.calculateTax(request(sourceState = "MH", destState = "MH"))

        assertTrue(result.isSuccess)
        val r = result.getOrThrow()
        assertEquals(770.0, r.baseAmount, tol)            // 385 * 2
        assertEquals(2, r.taxComponents.size)
        assertEquals(107.80, r.taxComponents.first { it.componentName == "CGST" }.taxAmount, tol)
        assertEquals(107.80, r.taxComponents.first { it.componentName == "SGST" }.taxAmount, tol)
        assertEquals(215.60, r.totalTaxAmount, tol)
        assertEquals(985.60, r.totalAmount, tol)
        assertEquals("true", r.metadata["is_intra_state"])
    }

    @Test
    fun interState_singleIgst() = runTest {
        seedCement()
        val result = strategy.calculateTax(request(sourceState = "MH", destState = "KA"))

        assertTrue(result.isSuccess)
        val r = result.getOrThrow()
        assertEquals(1, r.taxComponents.size)
        assertEquals("IGST", r.taxComponents.single().componentName)
        assertEquals(215.60, r.taxComponents.single().taxAmount, tol)
        assertEquals(985.60, r.totalAmount, tol)
        assertEquals("false", r.metadata["is_intra_state"])
    }

    @Test
    fun components_sumToTotalTax() = runTest {
        seedCement()
        val r = strategy.calculateTax(request(sourceState = "MH", destState = "MH")).getOrThrow()
        assertEquals(r.totalTaxAmount, r.taxComponents.sumOf { it.taxAmount }, tol)
    }

    @Test
    fun missingSourceState_isFailure() = runTest {
        seedCement()
        val req = TaxCalculationRequest(
            taxCode = "cement",
            taxCodeType = TaxCodeType.HSN_CODE,
            baseAmount = 100.0,
            quantity = 1,
            sourceLocation = TaxJurisdiction(country = "IN", state = null),
            destinationLocation = TaxJurisdiction(country = "IN", state = "MH"),
            transactionType = TransactionType.B2B,
            transactionContext = TransactionContext(),
        )
        val result = strategy.calculateTax(req)
        assertTrue(result.isFailure)
    }

    @Test
    fun missingRule_isFailure() = runTest {
        // nothing seeded
        val result = strategy.calculateTax(request(sourceState = "MH", destState = "MH"))
        assertTrue(result.isFailure)
    }

    @Test
    fun fallsBackToCountryJurisdiction_whenStateRuleAbsent() = runTest {
        // Rule stored at country-code jurisdiction "IN", request uses state "MH" first.
        ruleDao.seedRule(
            taxRule(
                taxCode = "cement",
                jurisdiction = "IN",
                composition = mapOf(
                    "INTRA_STATE" to composition(
                        "INTRA_STATE",
                        listOf(componentRef("c", "CGST", 9.0, order = 1), componentRef("s", "SGST", 9.0, order = 2)),
                        totalRate = 18.0,
                    ),
                ),
            ),
        )
        val r = strategy.calculateTax(request(sourceState = "MH", destState = "MH", baseAmount = 100.0, quantity = 1))
        assertTrue(r.isSuccess)
        assertEquals(18.0, r.getOrThrow().totalTaxAmount, tol)
    }

    @Test
    fun interStateScenarioMissingFromComposition_isFailure() = runTest {
        // Only INTRA configured, but request is inter-state.
        ruleDao.seedRule(
            taxRule(
                taxCode = "cement",
                jurisdiction = "MH",
                composition = mapOf(
                    "INTRA_STATE" to composition(
                        "INTRA_STATE",
                        listOf(componentRef("c", "CGST", 9.0, order = 1)),
                        totalRate = 9.0,
                    ),
                ),
            ),
        )
        val result = strategy.calculateTax(request(sourceState = "MH", destState = "KA"))
        assertTrue(result.isFailure)
    }

    @Test
    fun validateTaxCode_hsnAndSac() {
        assertTrue(strategy.validateTaxCode("1234", "HSN_CODE"))
        assertTrue(strategy.validateTaxCode("12345678", "HSN_CODE"))
        assertTrue(strategy.validateTaxCode("998314", "SAC_CODE"))
        // HSN must be 4-8 digits
        assertTrue(!strategy.validateTaxCode("123", "HSN_CODE"))
        assertTrue(!strategy.validateTaxCode("123456789", "HSN_CODE"))
        // SAC must be exactly 6 digits
        assertTrue(!strategy.validateTaxCode("12345", "SAC_CODE"))
        assertTrue(!strategy.validateTaxCode("ABCD", "HSN_CODE"))
        assertTrue(!strategy.validateTaxCode("1234", "UNKNOWN"))
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("IN", strategy.countryCode)
        assertEquals("INDIA_GST", strategy.strategyName)
    }
}
