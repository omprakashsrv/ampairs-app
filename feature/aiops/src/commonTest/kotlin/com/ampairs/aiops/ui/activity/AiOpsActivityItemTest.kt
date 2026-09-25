package com.ampairs.aiops.ui.activity

import com.ampairs.aiops.db.entity.AiOpsDecisionEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the decision → activity-row mapping that decides whether the UI offers Undo. Pure, so it needs
 * no dispatcher: a reversible, un-reverted decision is undoable; a reverted or non-reversible one isn't.
 */
class AiOpsActivityItemTest {

    private fun decision(
        reversible: Boolean = true,
        revertedAt: Long? = null,
    ) = AiOpsDecisionEntity(
        id = "AID1",
        findingId = "AIO1",
        capability = "customer.email",
        entityType = "customer",
        entityId = "C1",
        field = "email",
        beforeValue = "Jo@X.com",
        afterValue = "jo@x.com",
        action = "UPDATE_FIELD",
        confidence = 0.999,
        riskLevel = "LOW",
        source = "AUTO",
        reversible = reversible,
        revertedAt = revertedAt,
        createdAt = 1_000L,
    )

    @Test
    fun `reversible and un-reverted decision is undoable`() {
        val item = decision().toActivityItem()
        assertTrue(item.canUndo)
        assertFalse(item.reverted)
        assertEquals("email", item.field)
        assertEquals("Jo@X.com", item.before)
        assertEquals("jo@x.com", item.after)
    }

    @Test
    fun `a reverted decision is not undoable and shows as reverted`() {
        val item = decision(revertedAt = 2_000L).toActivityItem()
        assertTrue(item.reverted)
        assertFalse(item.canUndo)
    }

    @Test
    fun `a non-reversible decision is never undoable`() {
        val item = decision(reversible = false).toActivityItem()
        assertFalse(item.canUndo)
        assertFalse(item.reverted)
    }
}
