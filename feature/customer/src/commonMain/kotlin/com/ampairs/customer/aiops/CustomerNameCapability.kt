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
 * Sixth AI Ops capability: **customer name whitespace normalization** — a fourth field on the `customer`
 * entity, and the first capability to use a *whitespace-collapse* transform rather than case/format
 * normalization. Detects active customers whose `name` carries stray whitespace (edge padding or internal
 * runs) and proposes the collapsed value via the deterministic [CustomerNameNormalizer] — no LLM, HIGH
 * confidence, LOW risk, reversible (it only changes spacing, never words/order/case, and the original is
 * kept in the audit trail). Auto-fixes at autonomy ≥ AUTO_CORRECT through [CustomerNameExecutor] (which
 * writes via the customer repository + offline-sync).
 *
 * Lives in `feature/customer` and depends only on the `com.ampairs.common.aiops` contracts (+ this
 * module's own `CustomerDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.name")
class CustomerNameCapability(
    private val customerDao: CustomerDao,
) : AiOpsCapability {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { customer ->
                val name = customer.name
                if (!CustomerNameNormalizer.needsNormalization(name)) return@mapNotNull null
                val canonical = CustomerNameNormalizer.normalize(name)
                Finding(
                    id = findingId(customer.id),
                    capability = KEY,
                    entityType = ENTITY_TYPE,
                    entityId = customer.id,
                    field = FIELD_NAME,
                    summary = "Tidy customer name spacing \"$name\" → \"$canonical\"",
                    signals = mapOf("current" to name, "canonical" to canonical),
                )
            }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.name
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> {
        val current = context.values["current"] ?: finding.signals["current"] ?: return emptyList()
        if (!CustomerNameNormalizer.needsNormalization(current)) return emptyList()
        val canonical = CustomerNameNormalizer.normalize(current)
        return listOf(
            Candidate(
                field = FIELD_NAME,
                before = current,
                after = canonical,
                action = AiOpsActionType.UPDATE_FIELD,
                rationale = "Stray spacing is data-entry noise; \"$current\" tidies to \"$canonical\".",
                evidence = listOf("name_normalize:$current→$canonical"),
            ),
        )
    }

    override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation {
        val after = candidate.after
        return when {
            candidate.action != AiOpsActionType.UPDATE_FIELD -> Validation(false, "unexpected action")
            after.isNullOrBlank() -> Validation(false, "no target value")
            after == candidate.before -> Validation(false, "already normalized")
            after != CustomerNameNormalizer.normalize(after) -> Validation(false, "target is not canonical")
            else -> Validation(true)
        }
    }

    override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 0.999, band = AiOpsBand.HIGH, contributors = mapOf("name_normalize" to 1.0))

    companion object {
        const val KEY = "customer.name"
        const val ENTITY_TYPE = "customer"
        const val FIELD_NAME = "name"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
