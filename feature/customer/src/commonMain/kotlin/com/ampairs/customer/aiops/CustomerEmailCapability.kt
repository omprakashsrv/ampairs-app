package com.ampairs.customer.aiops

import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.AiOpsCapability
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.CapabilityKey
import com.ampairs.common.aiops.Confidence
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.aiops.Validation
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.db.CustomerDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Second AI Ops capability (proves the plug-in model across a different entity): **customer email
 * normalization**. Detects active customers whose `email` isn't in canonical form (trailing spaces or
 * mixed case) and proposes the trimmed-lowercased value via the deterministic [CustomerEmailNormalizer]
 * — no LLM, HIGH confidence, LOW risk, reversible. Auto-fixes at autonomy ≥ AUTO_CORRECT through
 * [CustomerEmailExecutor] (which writes via the customer repository + offline-sync).
 *
 * Lives in `feature/customer` and depends only on the `com.ampairs.common.aiops` contracts (+ this
 * module's own `CustomerDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.email")
class CustomerEmailCapability(
    private val customerDao: CustomerDao,
) : AiOpsCapability {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { customer ->
                val email = customer.email
                if (!CustomerEmailNormalizer.needsNormalization(email)) return@mapNotNull null
                val canonical = CustomerEmailNormalizer.normalize(email!!)
                Finding(
                    id = findingId(customer.id),
                    capability = KEY,
                    entityType = ENTITY_TYPE,
                    entityId = customer.id,
                    field = FIELD_EMAIL,
                    summary = "Normalize customer email \"$email\" → \"$canonical\"",
                    signals = mapOf("current" to email, "canonical" to canonical),
                )
            }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.email
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> {
        val current = context.values["current"] ?: finding.signals["current"] ?: return emptyList()
        if (!CustomerEmailNormalizer.needsNormalization(current)) return emptyList()
        val canonical = CustomerEmailNormalizer.normalize(current)
        return listOf(
            Candidate(
                field = FIELD_EMAIL,
                before = current,
                after = canonical,
                action = AiOpsActionType.UPDATE_FIELD,
                rationale = "Email addresses are case-insensitive; \"$current\" normalizes to \"$canonical\".",
                evidence = listOf("email_normalize:$current→$canonical"),
            ),
        )
    }

    override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation {
        val after = candidate.after
        return when {
            candidate.action != AiOpsActionType.UPDATE_FIELD -> Validation(false, "unexpected action")
            after.isNullOrBlank() -> Validation(false, "no target value")
            after == candidate.before -> Validation(false, "already normalized")
            !after.contains('@') -> Validation(false, "target is not an email")
            after != CustomerEmailNormalizer.normalize(after) -> Validation(false, "target is not canonical")
            else -> Validation(true)
        }
    }

    override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 0.999, band = AiOpsBand.HIGH, contributors = mapOf("email_normalize" to 1.0))

    companion object {
        const val KEY = "customer.email"
        const val ENTITY_TYPE = "customer"
        const val FIELD_EMAIL = "email"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
