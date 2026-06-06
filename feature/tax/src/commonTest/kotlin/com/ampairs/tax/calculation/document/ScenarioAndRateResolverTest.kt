package com.ampairs.tax.calculation.document

import com.ampairs.tax.domain.model.ComponentComposition
import com.ampairs.tax.domain.model.ComponentReference
import com.ampairs.tax.domain.model.TaxRule
import kotlin.test.Test
import kotlin.test.assertEquals

class ScenarioResolverTest {

    @Test
    fun stateCodeFromGstin() {
        assertEquals(29, ScenarioResolver.stateCodeFromGstin("29ABCDE1234F1Z5"))
        assertEquals(27, ScenarioResolver.stateCodeFromGstin("27ABCDE5678G1Z2"))
        assertEquals(null, ScenarioResolver.stateCodeFromGstin(""))
        assertEquals(null, ScenarioResolver.stateCodeFromGstin(null))
        assertEquals(null, ScenarioResolver.stateCodeFromGstin("AB"))
    }

    @Test
    fun sameState_isIntra_differentState_isInter() {
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolve(29, 29))
        assertEquals(TaxScenario.INTER, ScenarioResolver.resolve(29, 27))
    }

    @Test
    fun missingBuyer_defaultsToIntra() {
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolve(29, null))
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolveFromGstins("29ABCDE1234F1Z5", null))
    }

    @Test
    fun resolveFromGstins_usesFallbackThenGstin() {
        assertEquals(TaxScenario.INTER, ScenarioResolver.resolveFromGstins("27AAA", "29BBB"))
        assertEquals(TaxScenario.INTRA, ScenarioResolver.resolveFromGstins("27AAA", null, buyerStateFallback = 27))
        assertEquals(TaxScenario.INTER, ScenarioResolver.resolveFromGstins("27AAA", null, buyerStateFallback = 29))
    }
}

class TaxRuleRateResolverTest {

    private val rule = TaxRule(
        taxCode = "cement",
        componentComposition = mapOf(
            "INTRA_STATE" to ComponentComposition(
                scenario = "INTRA_STATE",
                components = listOf(
                    ComponentReference(id = "c", name = "CGST", rate = 14.0, order = 1),
                    ComponentReference(id = "s", name = "SGST", rate = 14.0, order = 2),
                ),
                totalRate = 28.0,
            ),
            "INTER_STATE" to ComponentComposition(
                scenario = "INTER_STATE",
                components = listOf(ComponentReference(id = "i", name = "IGST", rate = 28.0, order = 1)),
                totalRate = 28.0,
            ),
        ),
    )

    @Test
    fun resolvesIntraComposition() {
        val r = TaxRuleRateResolver.resolve(rule, TaxScenario.INTRA)
        assertEquals(28.0, r.totalRate, 0.001)
        assertEquals(listOf("CGST", "SGST"), r.components.map { it.name })
    }

    @Test
    fun resolvesInterComposition() {
        val r = TaxRuleRateResolver.resolve(rule, TaxScenario.INTER)
        assertEquals(listOf("IGST"), r.components.map { it.name })
    }

    @Test
    fun nullRule_isExempt() {
        assertEquals(ResolvedRate.EXEMPT, TaxRuleRateResolver.resolve(null, TaxScenario.INTRA))
    }

    @Test
    fun missingScenario_isExempt() {
        val intraOnly = rule.copy(componentComposition = rule.componentComposition.filterKeys { it == "INTRA_STATE" })
        assertEquals(ResolvedRate.EXEMPT, TaxRuleRateResolver.resolve(intraOnly, TaxScenario.INTER))
    }
}
