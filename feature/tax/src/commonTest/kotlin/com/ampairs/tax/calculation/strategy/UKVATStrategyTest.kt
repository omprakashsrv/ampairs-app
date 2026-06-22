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
 * Tests for [UKVATStrategy] — standard 20% / reduced 5% / zero 0% VAT, with reverse-charge,
 * export and exemption short circuits (each returns zero tax with distinct metadata).
 */
class UKVATStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = UKVATStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    private fun seedVat(rate: Double, jurisdiction: String = "UK") {
        ruleDao.seedRule(
            taxRule(
                taxCode = "VATCODE",
                jurisdiction = jurisdiction,
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(componentRef("uk-vat", "VAT", rate, order = 1)),
                        totalRate = rate,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = "uk-vat", componentTypeId = "type-vat", rate = rate))
        typeDao.seed(componentType(id = "type-vat", componentCode = "VAT"))
    }

    private fun request(
        baseAmount: Double = 100.0,
        quantity: Int = 1,
        transactionType: TransactionType = TransactionType.DOMESTIC,
        reverseCharge: Boolean = false,
        exemptionReason: String? = null,
        state: String? = null,
    ) = TaxCalculationRequest(
        taxCode = "VATCODE",
        taxCodeType = TaxCodeType.TAX_CATEGORY,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = "GB", state = state),
        destinationLocation = TaxJurisdiction(country = "GB", state = state),
        transactionType = transactionType,
        transactionContext = TransactionContext(isReverseCharge = reverseCharge, exemptionReason = exemptionReason),
    )

    @Test
    fun standardRate_isTwentyPercent() = runTest {
        seedVat(rate = 20.0)
        val r = strategy.calculateTax(request(baseAmount = 50.0, quantity = 2)).getOrThrow()
        assertEquals(100.0, r.baseAmount, tol)
        assertEquals(20.0, r.totalTaxAmount, tol)
        assertEquals(120.0, r.totalAmount, tol)
        assertEquals("Standard Rate", r.metadata["vat_category"])
    }

    @Test
    fun reducedRate_isFivePercent() = runTest {
        seedVat(rate = 5.0)
        val r = strategy.calculateTax(request(baseAmount = 200.0)).getOrThrow()
        assertEquals(10.0, r.totalTaxAmount, tol)
        assertEquals("Reduced Rate", r.metadata["vat_category"])
    }

    @Test
    fun zeroRate_producesNoTax() = runTest {
        seedVat(rate = 0.0)
        val r = strategy.calculateTax(request(baseAmount = 200.0)).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("Zero Rated", r.metadata["vat_category"])
    }

    @Test
    fun reverseCharge_shortCircuitsToZeroTax() = runTest {
        seedVat(rate = 20.0)
        val r = strategy.calculateTax(request(reverseCharge = true)).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertTrue(r.taxComponents.isEmpty())
        assertEquals("true", r.metadata["reverse_charge"])
    }

    @Test
    fun export_isZeroRated() = runTest {
        seedVat(rate = 20.0)
        val r = strategy.calculateTax(request(transactionType = TransactionType.EXPORT)).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["zero_rated"])
    }

    @Test
    fun exemptSupply_isZeroTax() = runTest {
        seedVat(rate = 20.0)
        val r = strategy.calculateTax(request(exemptionReason = "Insurance")).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["exempt"])
    }

    @Test
    fun northernIreland_jurisdictionUsesWindsorFramework() = runTest {
        seedVat(rate = 20.0, jurisdiction = "NI")
        val r = strategy.calculateTax(request(baseAmount = 100.0, state = "NI")).getOrThrow()
        assertEquals(20.0, r.totalTaxAmount, tol)
        assertEquals("Windsor Framework applies", r.metadata["scheme"])
    }

    @Test
    fun missingRule_isFailure() = runTest {
        val result = strategy.calculateTax(request())
        assertTrue(result.isFailure)
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("GB", strategy.countryCode)
        assertEquals("UK_VAT", strategy.strategyName)
    }
}
