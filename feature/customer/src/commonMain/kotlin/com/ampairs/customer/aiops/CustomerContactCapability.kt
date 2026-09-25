package com.ampairs.customer.aiops

import com.ampairs.common.aiops.AdvisoryCapability
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.CapabilityKey
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.db.CustomerDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * First **advisory / completeness** AI Ops capability (a new capability kind — detect-only, never
 * auto-fixed): **customer contact completeness**. Flags active customers that have *neither* an email nor
 * a phone — a customer you can't reach — so the assistant surfaces it for review. There's no safe value to
 * fill in automatically, so it's a suggestion the user acts on manually, then dismisses; it never changes
 * data. Built on the shared [AdvisoryCapability] base (emits a non-reversible NO_OP → the gate always
 * routes it to review), so this file only supplies the metadata and the DAO-bound [detect].
 *
 * Has **no executor** by design. Depends only on the `com.ampairs.common.aiops` contracts (+ this module's
 * `CustomerDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.contact")
class CustomerContactCapability(
    private val customerDao: CustomerDao,
) : AdvisoryCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val evidenceTag: String = "contact_missing"

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active && it.email.isNullOrBlank() && it.phone.isNullOrBlank() }
            .map { customer ->
                advisory(
                    entityId = customer.id,
                    summary = "Customer \"${customer.name}\" has no email or phone",
                )
            }

    override suspend fun gather(finding: Finding): FindingContext = FindingContext()

    companion object {
        const val KEY = "customer.contact"
        const val ENTITY_TYPE = "customer"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
