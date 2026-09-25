package com.ampairs.customer.aiops

import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.CapabilityKey
import com.ampairs.common.aiops.FieldNormalizationCapability
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.db.CustomerDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * **Customer email normalization** — trims + lower-cases the case-insensitive `email` field. Built on the
 * shared [FieldNormalizationCapability] base, so only the customer-specific bits remain: the deterministic
 * [CustomerEmailNormalizer], the field/tag/labels, the "must contain '@'" target guard, and the DAO-bound
 * `detect`/`gather`. Auto-fixes at autonomy ≥ AUTO_CORRECT through [CustomerEmailExecutor] (customer
 * repository + offline-sync).
 *
 * Depends only on the `com.ampairs.common.aiops` contracts (+ this module's `CustomerDao`) — never on
 * `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.email")
class CustomerEmailCapability(
    private val customerDao: CustomerDao,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_EMAIL
    override val evidenceTag: String = "email_normalize"

    override fun normalize(value: String): String = CustomerEmailNormalizer.normalize(value)
    override fun needsNormalization(value: String?): Boolean = CustomerEmailNormalizer.needsNormalization(value)

    override fun rationale(current: String, canonical: String): String =
        "Email addresses are case-insensitive; \"$current\" normalizes to \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Normalize customer email \"$current\" → \"$canonical\""

    /** Never rewrite a value that isn't an email (no '@') even if it happens to be non-canonical text. */
    override fun rejectTarget(after: String): String? =
        if (!after.contains('@')) "target is not an email" else null

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { findingFor(it.id, it.email) }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.email
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    companion object {
        const val KEY = "customer.email"
        const val ENTITY_TYPE = "customer"
        const val FIELD_EMAIL = "email"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
