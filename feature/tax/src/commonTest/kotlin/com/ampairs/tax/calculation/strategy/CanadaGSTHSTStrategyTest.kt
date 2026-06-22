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
 * Tests for [CanadaGSTHSTStrategy] — province-based (destination) GST/HST/PST. Each component is
 * taxed on the base amount; component display names are derived from the component-type code.
 * zero_rated and exemptionReason short-circuit to zero tax with regime metadata.
 */
class CanadaGSTHSTStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = CanadaGSTHSTStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    init {
        typeDao.seed(componentType(id = "type-gst", componentCode = "GST"))
        typeDao.seed(componentType(id = "type-pst", componentCode = "PST"))
        typeDao.seed(componentType(id = "type-hst", componentCode = "HST"))
    }

    /** BC = GST 5% + PST 7% (calculated separately on base). */
    private fun seedBcGstPst() {
        ruleDao.seedRule(
            taxRule(
                taxCode = "GENERAL",
                jurisdiction = "BC",
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(
                            componentRef("ca-gst", "GST", 5.0, order = 1),
                            componentRef("ca-pst", "PST", 7.0, order = 2),
                        ),
                        totalRate = 12.0,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = "ca-gst", componentTypeId = "type-gst", jurisdiction = "BC", rate = 5.0))
        componentDao.seed(workspaceComponent(id = "ca-pst", componentTypeId = "type-pst", jurisdiction = "BC", rate = 7.0))
    }

    /** ON = HST 13%. */
    private fun seedOntarioHst() {
        ruleDao.seedRule(
            taxRule(
                taxCode = "GENERAL",
                jurisdiction = "ON",
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(componentRef("ca-hst", "HST", 13.0, order = 1)),
                        totalRate = 13.0,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = "ca-hst", componentTypeId = "type-hst", jurisdiction = "ON", rate = 13.0))
    }

    private fun request(
        baseAmount: Double = 100.0,
        quantity: Int = 1,
        province: String? = "BC",
        special: Map<String, String> = emptyMap(),
        exemptionReason: String? = null,
    ) = TaxCalculationRequest(
        taxCode = "GENERAL",
        taxCodeType = TaxCodeType.TAX_CATEGORY,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = "CA", state = "BC"),
        destinationLocation = TaxJurisdiction(country = "CA", state = province),
        transactionType = TransactionType.B2C,
        transactionContext = TransactionContext(specialConditions = special, exemptionReason = exemptionReason),
    )

    @Test
    fun bcGstPst_areBothOnBase() = runTest {
        seedBcGstPst()
        val r = strategy.calculateTax(request(baseAmount = 100.0, province = "BC")).getOrThrow()
        assertEquals(2, r.taxComponents.size)
        assertEquals(5.0, r.taxComponents.first { it.taxType == "GST" }.taxAmount, tol)
        assertEquals(7.0, r.taxComponents.first { it.taxType == "PST" }.taxAmount, tol)
        assertEquals(12.0, r.totalTaxAmount, tol)
        assertEquals(112.0, r.totalAmount, tol)
        assertEquals("GST+PST", r.metadata["tax_regime"])
    }

    @Test
    fun ontarioHst_isThirteenPercent() = runTest {
        seedOntarioHst()
        val r = strategy.calculateTax(request(baseAmount = 100.0, province = "ON")).getOrThrow()
        assertEquals(13.0, r.totalTaxAmount, tol)
        assertEquals("HST", r.metadata["tax_regime"])
    }

    @Test
    fun components_sumToTotalTax() = runTest {
        seedBcGstPst()
        val r = strategy.calculateTax(request(baseAmount = 333.0)).getOrThrow()
        assertEquals(r.totalTaxAmount, r.taxComponents.sumOf { it.taxAmount }, tol)
    }

    @Test
    fun zeroRatedSupply_isNoTax() = runTest {
        seedBcGstPst()
        val r = strategy.calculateTax(request(special = mapOf("zero_rated" to "true"))).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertTrue(r.taxComponents.isEmpty())
        assertEquals("true", r.metadata["zero_rated"])
    }

    @Test
    fun exemptSupply_isNoTax() = runTest {
        seedBcGstPst()
        val r = strategy.calculateTax(request(exemptionReason = "Residential rent")).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["exempt"])
    }

    @Test
    fun missingProvince_isFailure() = runTest {
        seedBcGstPst()
        val result = strategy.calculateTax(request(province = null))
        assertTrue(result.isFailure)
    }

    @Test
    fun missingRule_isFailure() = runTest {
        val result = strategy.calculateTax(request(province = "QC"))
        assertTrue(result.isFailure)
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("CA", strategy.countryCode)
        assertEquals("CANADA_GST_HST", strategy.strategyName)
    }
}
