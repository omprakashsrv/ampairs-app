package com.ampairs.tax.calculation.strategy

import com.ampairs.tax.calculation.model.TaxCalculationRequest
import com.ampairs.tax.calculation.model.TaxJurisdiction
import com.ampairs.tax.calculation.model.TransactionContext
import com.ampairs.tax.calculation.model.TransactionType
import com.ampairs.tax.data.repository.TaxComponentRepository
import com.ampairs.tax.data.repository.TaxRuleRepository
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.testutil.FakeTaxComponentDao
import com.ampairs.tax.testutil.FakeTaxComponentTypeDao
import com.ampairs.tax.testutil.FakeTaxConfigurationApi
import com.ampairs.tax.testutil.FakeTaxRuleDao
import com.ampairs.tax.testutil.componentRef
import com.ampairs.tax.testutil.componentType
import com.ampairs.tax.testutil.composition
import com.ampairs.tax.testutil.taxRule
import com.ampairs.tax.testutil.workspaceComponent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [DefaultTaxStrategy] — the generic percentage fallback. It looks the rule up at
 * `state ?: country`, reads the "standard" composition, and resolves each component via the
 * workspace-component + component-type DAOs (skipping any component whose lookups miss).
 */
class DefaultTaxStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = DefaultTaxStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    /** Seed a single-component "standard" rule + the matching workspace component & type. */
    private fun seedStandard(rate: Double, jurisdiction: String = "ZZ", componentId: String = "vat-comp") {
        ruleDao.seedRule(
            taxRule(
                taxCode = "GENERIC",
                jurisdiction = jurisdiction,
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(componentRef(componentId, "VAT", rate, order = 1)),
                        totalRate = rate,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = componentId, componentTypeId = "type-vat", rate = rate))
        typeDao.seed(componentType(id = "type-vat", componentCode = "VAT"))
    }

    private fun request(
        baseAmount: Double = 100.0,
        quantity: Int = 1,
        state: String? = "ZZ",
        exemptionReason: String? = null,
    ) = TaxCalculationRequest(
        taxCode = "GENERIC",
        taxCodeType = TaxCodeType.TAX_CATEGORY,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = "ZZ", state = state),
        destinationLocation = TaxJurisdiction(country = "ZZ", state = state),
        transactionType = TransactionType.DOMESTIC,
        transactionContext = TransactionContext(exemptionReason = exemptionReason),
    )

    @Test
    fun standardPercentage_isAppliedToBase() = runTest {
        seedStandard(rate = 12.0)
        val r = strategy.calculateTax(request(baseAmount = 200.0, quantity = 3)).getOrThrow()
        assertEquals(600.0, r.baseAmount, tol)            // 200 * 3
        assertEquals(72.0, r.totalTaxAmount, tol)         // 12% of 600
        assertEquals(672.0, r.totalAmount, tol)
        assertEquals(1, r.taxComponents.size)
        assertEquals(600.0, r.taxComponents.single().taxableAmount, tol)
    }

    @Test
    fun zeroRate_producesNoTax() = runTest {
        seedStandard(rate = 0.0)
        val r = strategy.calculateTax(request(baseAmount = 500.0)).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals(500.0, r.totalAmount, tol)
    }

    @Test
    fun exemptionReason_shortCircuitsToZeroTaxWithNoComponents() = runTest {
        seedStandard(rate = 18.0)
        val r = strategy.calculateTax(request(baseAmount = 100.0, exemptionReason = "Charity")).getOrThrow()
        assertTrue(r.taxComponents.isEmpty())
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals(100.0, r.totalAmount, tol)
        assertEquals("true", r.metadata["exempt"])
    }

    @Test
    fun missingWorkspaceComponent_isSkipped_yieldingZeroTax() = runTest {
        // Rule has a component reference, but no workspace component seeded → strategy `continue`s.
        ruleDao.seedRule(
            taxRule(
                taxCode = "GENERIC",
                jurisdiction = "ZZ",
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(componentRef("ghost", "VAT", 10.0, order = 1)),
                        totalRate = 10.0,
                    ),
                ),
            ),
        )
        val r = strategy.calculateTax(request(baseAmount = 100.0)).getOrThrow()
        assertTrue(r.taxComponents.isEmpty())
        assertEquals(0.0, r.totalTaxAmount, tol)
    }

    @Test
    fun missingRule_isFailure() = runTest {
        val result = strategy.calculateTax(request())
        assertTrue(result.isFailure)
    }

    @Test
    fun missingStandardComposition_isFailure() = runTest {
        ruleDao.seedRule(
            taxRule(
                taxCode = "GENERIC",
                jurisdiction = "ZZ",
                composition = mapOf(
                    "other" to composition("other", listOf(componentRef("x", "VAT", 10.0)), totalRate = 10.0),
                ),
            ),
        )
        val result = strategy.calculateTax(request())
        assertTrue(result.isFailure)
    }

    @Test
    fun validateTaxCode_rejectsBlank() {
        assertTrue(strategy.validateTaxCode("ANY", "TAX_CATEGORY"))
        assertTrue(!strategy.validateTaxCode("", "TAX_CATEGORY"))
        assertTrue(!strategy.validateTaxCode("   ", "TAX_CATEGORY"))
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("DEFAULT", strategy.countryCode)
        assertEquals("DEFAULT_TAX", strategy.strategyName)
    }
}
