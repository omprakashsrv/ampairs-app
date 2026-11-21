package com.ampairs.unit.aiops

import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.unit.data.db.entity.UnitEntity
import com.ampairs.unit.data.db.entity.toEntity
import com.ampairs.unit.data.repository.FakeUnitConversionDao
import com.ampairs.unit.data.repository.FakeUnitDao
import com.ampairs.unit.data.repository.RecordingSyncStateDao
import com.ampairs.unit.data.repository.UnitRepository
import com.ampairs.unit.domain.model.Unit as UnitModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage tests for the unit-standardization capability. Uses the module's in-memory DAO fakes (shared
 * with the repository test) so `detect`/`gather` run against a real [UnitRepository] with no Room.
 */
class UnitShortNameCapabilityTest {

    private val unitDao = FakeUnitDao()
    private val repository = UnitRepository(unitDao, FakeUnitConversionDao(), RecordingSyncStateDao())
    private val capability = UnitShortNameCapability(repository)

    private suspend fun seed(vararg units: UnitEntity) {
        units.forEach { unitDao.insertUnit(it) }
    }

    private fun unit(uid: String, short: String, active: Boolean = true) =
        UnitModel(uid = uid, name = "Name-$uid", shortName = short, decimalPlaces = 2, active = active).toEntity()

    @Test
    fun `detect flags a non-canonical unit and skips an already-canonical one`() = runTest {
        seed(unit("U1", "Kgs"), unit("U2", "KG"))

        val findings = capability.detect(AiOpsScope(workspaceId = "W1"))

        assertEquals(1, findings.size)
        val f = findings.single()
        assertEquals("U1", f.entityId)
        assertEquals("unit", f.entityType)
        assertEquals("KG", f.signals["canonical"])
        assertEquals(UnitShortNameCapability.findingId("U1"), f.id)
    }

    @Test
    fun `detect ignores unknown spellings`() = runTest {
        seed(unit("U1", "widget"))
        assertTrue(capability.detect(AiOpsScope("W1")).isEmpty())
    }

    private fun finding(entityId: String = "U1") =
        Finding(id = "x", capability = UnitShortNameCapability.KEY, entityType = "unit", entityId = entityId)

    @Test
    fun `propose returns a reversible UPDATE_FIELD candidate to the canonical`() = runTest {
        val candidate = capability.propose(
            finding = finding(),
            context = FindingContext(values = mapOf("current" to "Litres")),
        ).single()

        assertEquals(AiOpsActionType.UPDATE_FIELD, candidate.action)
        assertEquals("Litres", candidate.before)
        assertEquals("L", candidate.after)
        assertEquals("short_name", candidate.field)
    }

    @Test
    fun `validate rejects an unknown target and an unchanged value, accepts a real rewrite`() = runTest {
        val finding = finding()
        val ctx = FindingContext()

        val unknown = Candidate(field = "short_name", before = "kg", after = "widget", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(finding, unknown, ctx).valid)

        val unchanged = Candidate(field = "short_name", before = "KG", after = "KG", action = AiOpsActionType.UPDATE_FIELD)
        assertFalse(capability.validate(finding, unchanged, ctx).valid)

        val good = Candidate(field = "short_name", before = "Kgs", after = "KG", action = AiOpsActionType.UPDATE_FIELD)
        assertTrue(capability.validate(finding, good, ctx).valid)
    }

    @Test
    fun `score is HIGH band for a deterministic alias match`() = runTest {
        val candidate = Candidate(field = "short_name", before = "Kgs", after = "KG", action = AiOpsActionType.UPDATE_FIELD)

        val confidence = capability.score(finding(), candidate, FindingContext())

        assertEquals(AiOpsBand.HIGH, confidence.band)
        assertTrue(confidence.value >= 0.99)
    }

    @Test
    fun `capability metadata is unit low-risk`() {
        assertEquals("unit.shortname", capability.key)
        assertEquals("unit", capability.entityType)
        assertEquals(AiOpsRiskLevel.LOW, capability.riskLevel)
    }
}
