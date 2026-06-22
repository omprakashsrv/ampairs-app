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
 * Tests for [AustraliaGSTStrategy] — flat 10% GST with several zero-tax short circuits
 * (gst_free / input_taxed special conditions, EXPORT, and reverse-charged IMPORT).
 */
class AustraliaGSTStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = AustraliaGSTStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    private fun seedGst(state: String = "NSW") {
        ruleDao.seedRule(
            taxRule(
                taxCode = "GST",
                jurisdiction = state,
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(componentRef("au-gst", "GST", 10.0, order = 1)),
                        totalRate = 10.0,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = "au-gst", componentTypeId = "type-gst", rate = 10.0))
        typeDao.seed(componentType(id = "type-gst", componentCode = "GST"))
    }

    private fun request(
        baseAmount: Double = 100.0,
        quantity: Int = 1,
        transactionType: TransactionType = TransactionType.DOMESTIC,
        special: Map<String, String> = emptyMap(),
        reverseCharge: Boolean = false,
        state: String = "NSW",
    ) = TaxCalculationRequest(
        taxCode = "GST",
        taxCodeType = TaxCodeType.TAX_CATEGORY,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = "AU", state = state),
        destinationLocation = TaxJurisdiction(country = "AU", state = state),
        transactionType = transactionType,
        transactionContext = TransactionContext(isReverseCharge = reverseCharge, specialConditions = special),
    )

    @Test
    fun standardGst_isTenPercent() = runTest {
        seedGst()
        val r = strategy.calculateTax(request(baseAmount = 250.0, quantity = 4)).getOrThrow()
        assertEquals(1000.0, r.baseAmount, tol)           // 250 * 4
        assertEquals(100.0, r.totalTaxAmount, tol)        // 10%
        assertEquals(1100.0, r.totalAmount, tol)
        assertEquals("GST", r.taxComponents.single().componentName)
    }

    @Test
    fun gstFree_isZeroRated() = runTest {
        seedGst()
        val r = strategy.calculateTax(request(special = mapOf("gst_free" to "true"))).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertTrue(r.taxComponents.isEmpty())
        assertEquals("true", r.metadata["gst_free"])
    }

    @Test
    fun inputTaxed_isZeroRated() = runTest {
        seedGst()
        val r = strategy.calculateTax(request(special = mapOf("input_taxed" to "true"))).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["input_taxed"])
    }

    @Test
    fun export_isGstFree() = runTest {
        seedGst()
        val r = strategy.calculateTax(request(transactionType = TransactionType.EXPORT)).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["gst_free"])
    }

    @Test
    fun reverseChargedImport_isZeroTaxForSeller() = runTest {
        seedGst()
        val r = strategy.calculateTax(
            request(transactionType = TransactionType.IMPORT, reverseCharge = true),
        ).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["reverse_charge"])
    }

    @Test
    fun missingRule_isFailure() = runTest {
        val result = strategy.calculateTax(request())
        assertTrue(result.isFailure)
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("AU", strategy.countryCode)
        assertEquals("AUSTRALIA_GST", strategy.strategyName)
    }
}
