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
 * Third AI Ops capability (a second field on the same `customer` entity, proving capabilities compose
 * without touching the engine): **customer phone normalization**. Detects active customers whose `phone`
 * carries display formatting (spaces, dashes, dots, parentheses) and proposes the digits-only canonical
 * value via the deterministic [CustomerPhoneNormalizer] — no LLM, HIGH confidence, LOW risk, reversible.
 * Auto-fixes at autonomy ≥ AUTO_CORRECT through [CustomerPhoneExecutor] (which writes via the customer
 * repository + offline-sync).
 *
 * Lives in `feature/customer` and depends only on the `com.ampairs.common.aiops` contracts (+ this
 * module's own `CustomerDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.phone")
class CustomerPhoneCapability(
    private val customerDao: CustomerDao,
) : AiOpsCapability {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { customer ->
                val phone = customer.phone
                if (!CustomerPhoneNormalizer.needsNormalization(phone)) return@mapNotNull null
                val canonical = CustomerPhoneNormalizer.normalize(phone!!)
                Finding(
                    id = findingId(customer.id),
                    capability = KEY,
                    entityType = ENTITY_TYPE,
                    entityId = customer.id,
                    field = FIELD_PHONE,
                    summary = "Normalize customer phone \"$phone\" → \"$canonical\"",
                    signals = mapOf("current" to phone, "canonical" to canonical),
                )
            }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.phone
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> {
        val current = context.values["current"] ?: finding.signals["current"] ?: return emptyList()
        if (!CustomerPhoneNormalizer.needsNormalization(current)) return emptyList()
        val canonical = CustomerPhoneNormalizer.normalize(current)
        return listOf(
            Candidate(
                field = FIELD_PHONE,
                before = current,
                after = canonical,
                action = AiOpsActionType.UPDATE_FIELD,
                rationale = "Phone numbers are dialled by digits; \"$current\" normalizes to \"$canonical\".",
                evidence = listOf("phone_normalize:$current→$canonical"),
            ),
        )
    }

    override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation {
        val after = candidate.after
        return when {
            candidate.action != AiOpsActionType.UPDATE_FIELD -> Validation(false, "unexpected action")
            after.isNullOrBlank() -> Validation(false, "no target value")
            after == candidate.before -> Validation(false, "already normalized")
            after.none { it.isDigit() } -> Validation(false, "target is not a phone number")
            after != CustomerPhoneNormalizer.normalize(after) -> Validation(false, "target is not canonical")
            else -> Validation(true)
        }
    }

    override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 0.999, band = AiOpsBand.HIGH, contributors = mapOf("phone_normalize" to 1.0))

    companion object {
        const val KEY = "customer.phone"
        const val ENTITY_TYPE = "customer"
        const val FIELD_PHONE = "phone"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
