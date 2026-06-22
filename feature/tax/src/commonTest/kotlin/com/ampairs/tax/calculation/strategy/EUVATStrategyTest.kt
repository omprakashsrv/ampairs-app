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
 * Tests for [EUVATStrategy] — harmonized VAT with: domestic/B2C destination VAT, B2B intra-EU
 * reverse charge (when buyer_vat_number present), and zero-rated export outside the EU.
 *
 * `isIntraEU = sellerCountry != buyerCountry && isEUCountry(buyerCountry)`. For B2C intra-EU the rule
 * is looked up at the buyer country; for domestic/B2B at the seller country.
 */
class EUVATStrategyTest {

    private val tol = 0.001

    private val ruleDao = FakeTaxRuleDao()
    private val componentDao = FakeTaxComponentDao()
    private val typeDao = FakeTaxComponentTypeDao()
    private val api = FakeTaxConfigurationApi()

    private val strategy = EUVATStrategy(
        taxRuleRepository = TaxRuleRepository(api, ruleDao),
        taxComponentRepository = TaxComponentRepository(api, typeDao, componentDao),
    )

    init {
        typeDao.seed(componentType(id = "type-vat", componentCode = "VAT"))
    }

    private fun seedVat(rate: Double, jurisdiction: String) {
        ruleDao.seedRule(
            taxRule(
                taxCode = "EUVAT",
                jurisdiction = jurisdiction,
                composition = mapOf(
                    "standard" to composition(
                        "standard",
                        listOf(componentRef("eu-vat-$jurisdiction", "VAT", rate, order = 1)),
                        totalRate = rate,
                    ),
                ),
            ),
        )
        componentDao.seed(workspaceComponent(id = "eu-vat-$jurisdiction", componentTypeId = "type-vat", rate = rate))
    }

    private fun request(
        baseAmount: Double = 100.0,
        quantity: Int = 1,
        sellerCountry: String = "DE",
        buyerCountry: String = "DE",
        transactionType: TransactionType = TransactionType.DOMESTIC,
        special: Map<String, String> = emptyMap(),
    ) = TaxCalculationRequest(
        taxCode = "EUVAT",
        taxCodeType = TaxCodeType.TAX_CATEGORY,
        baseAmount = baseAmount,
        quantity = quantity,
        sourceLocation = TaxJurisdiction(country = sellerCountry),
        destinationLocation = TaxJurisdiction(country = buyerCountry),
        transactionType = transactionType,
        transactionContext = TransactionContext(specialConditions = special),
    )

    @Test
    fun domesticStandardVat_isApplied() = runTest {
        seedVat(rate = 19.0, jurisdiction = "DE")   // German standard VAT
        val r = strategy.calculateTax(request(baseAmount = 200.0, sellerCountry = "DE", buyerCountry = "DE")).getOrThrow()
        assertEquals(38.0, r.totalTaxAmount, tol)   // 19% of 200
        assertEquals(238.0, r.totalAmount, tol)
        assertEquals("Standard Rate", r.taxComponents.single().description.substringBefore(" @"))
        assertEquals("false", r.metadata["intra_eu"])
    }

    @Test
    fun b2cIntraEu_usesBuyerCountryRate() = runTest {
        // B2C from DE to FR → destination principle → looks up rule at FR.
        seedVat(rate = 20.0, jurisdiction = "FR")
        val r = strategy.calculateTax(
            request(baseAmount = 100.0, sellerCountry = "DE", buyerCountry = "FR", transactionType = TransactionType.B2C),
        ).getOrThrow()
        assertEquals(20.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["intra_eu"])
        assertEquals("destination_principle", r.metadata["sourcing"])
    }

    @Test
    fun b2bIntraEuWithVatNumber_isReverseCharge() = runTest {
        seedVat(rate = 19.0, jurisdiction = "DE")
        val r = strategy.calculateTax(
            request(
                sellerCountry = "DE",
                buyerCountry = "FR",
                transactionType = TransactionType.B2B,
                special = mapOf("buyer_vat_number" to "FR123456789"),
            ),
        ).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertTrue(r.taxComponents.isEmpty())
        assertEquals("true", r.metadata["reverse_charge"])
    }

    @Test
    fun exportOutsideEu_isZeroRated() = runTest {
        seedVat(rate = 19.0, jurisdiction = "DE")
        val r = strategy.calculateTax(
            request(sellerCountry = "DE", buyerCountry = "US", transactionType = TransactionType.EXPORT),
        ).getOrThrow()
        assertEquals(0.0, r.totalTaxAmount, tol)
        assertEquals("true", r.metadata["zero_rated"])
    }

    @Test
    fun missingRule_isFailure() = runTest {
        val result = strategy.calculateTax(request())
        assertTrue(result.isFailure)
    }

    @Test
    fun countryCodeAndStrategyName() {
        assertEquals("EU", strategy.countryCode)
        assertEquals("EU_VAT", strategy.strategyName)
    }
}
