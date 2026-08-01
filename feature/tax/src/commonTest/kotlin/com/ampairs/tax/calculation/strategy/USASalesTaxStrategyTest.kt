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
 * Tests for [USASalesTaxStrategy] — destination-based, cascading State|County|City rates. The
 * strategy filters each component by `workspaceComponent.jurisdiction` (pipe-delimited
 * "STATE|COUNTY|CITY"); a component whose county/city is set but mismatches the request is dropped.
 */
class USASalesTaxStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = USASalesTaxStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    init {
        typeDao.seed(componentType(id = "type-state", componentCode = "STATE_TAX"))
        typeDao.seed(componentType(id = "type-county", componentCode = "COUNTY_TAX"))
        typeDao.seed(componentType(id = "type-city", componentCode = "CITY_TAX"))
    }

    /**
     * Seed a multi-jurisdiction rule for state CA, county LA, city LA-City.
     * Each workspace component carries a pipe-delimited jurisdiction string for matching.
     */
    private fun seedCascading() {
        ruleDao.seedRule(
            taxRule(
                taxCode = "GENERAL",
                jurisdiction = "CA",
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(
                            componentRef("us-state", "California State Tax", 6.0, order = 1),
                            componentRef("us-county", "LA County Tax", 1.0, order = 2),
                            componentRef("us-city", "LA City Tax", 1.5, order = 3),
                        ),
                        totalRate = 8.5,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = "us-state", componentTypeId = "type-state", jurisdiction = "CA"))
        componentDao.seed(workspaceComponent(id = "us-county", componentTypeId = "type-county", jurisdiction = "CA|LA"))
        componentDao.seed(workspaceComponent(id = "us-city", componentTypeId = "type-city", jurisdiction = "CA|LA|LA-City"))
    }

    private fun request(
        baseAmount: Double = 100.0,
        quantity: Int = 1,
        state: String? = "CA",
        county: String? = "LA",
        city: String? = "LA-City",
        transactionType: TransactionType = TransactionType.B2C,
        exemptionReason: String? = null,
    ) = TaxCalculationRequest(
        taxCode = "GENERAL",
        taxCodeType = TaxCodeType.TAX_CATEGORY,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = "US", state = "WA"),
        destinationLocation = TaxJurisdiction(country = "US", state = state, county = county, city = city),
        transactionType = transactionType,
        transactionContext = TransactionContext(exemptionReason = exemptionReason),
    )

    @Test
    fun allLevelsMatch_sumsStateCountyCity() = runTest {
        seedCascading()
        val r = strategy.calculateTax(request(baseAmount = 100.0)).getOrThrow()
        assertEquals(3, r.taxComponents.size)
        // 6% + 1% + 1.5% = 8.5% of 100
        assertEquals(8.5, r.totalTaxAmount, tol)
        assertEquals(108.5, r.totalAmount, tol)
    }

    @Test
    fun cityMismatch_dropsCityComponent() = runTest {
        seedCascading()
        // Different city → the CA|LA|LA-City component should be filtered out.
        val r = strategy.calculateTax(request(city = "Pasadena")).getOrThrow()
        assertEquals(2, r.taxComponents.size)
        assertEquals(7.0, r.totalTaxAmount, tol)   // state 6 + county 1
    }

    @Test
    fun countyMismatch_dropsCountyAndCity() = runTest {
        seedCascading()
        val r = strategy.calculateTax(request(county = "Orange", city = "Anaheim")).getOrThrow()
        assertEquals(1, r.taxComponents.size)      // only the state-level component matches
        assertEquals(6.0, r.totalTaxAmount, tol)
    }

    @Test
    fun b2bWithExemptionCertificate_isZeroTax() = runTest {
        seedCascading()
        val r = strategy.calculateTax(
            request(transactionType = TransactionType.B2B, exemptionReason = "Resale certificate"),
        ).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertTrue(r.taxComponents.isEmpty())
        assertEquals("true", r.metadata["exempt"])
    }

    @Test
    fun missingDestinationState_isFailure() = runTest {
        seedCascading()
        val result = strategy.calculateTax(request(state = null))
        assertTrue(result.isFailure)
    }

    @Test
    fun missingRule_isFailure() = runTest {
        val result = strategy.calculateTax(request())
        assertTrue(result.isFailure)
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("US", strategy.countryCode)
        assertEquals("USA_SALES_TAX", strategy.strategyName)
    }
}
