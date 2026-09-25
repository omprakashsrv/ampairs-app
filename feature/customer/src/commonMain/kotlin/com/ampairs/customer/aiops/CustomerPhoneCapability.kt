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
 * **Customer phone normalization** — strips display formatting (spaces/dashes/dots/parentheses) from
 * `phone` to a digits-only canonical form, preserving a leading '+'. Built on the shared
 * [FieldNormalizationCapability] base, so only the customer-specific bits remain: the deterministic
 * [CustomerPhoneNormalizer], the field/tag/labels, the "must contain a digit" target guard, and the
 * DAO-bound `detect`/`gather`. Auto-fixes at autonomy ≥ AUTO_CORRECT through [CustomerPhoneExecutor].
 *
 * Depends only on the `com.ampairs.common.aiops` contracts (+ this module's `CustomerDao`) — never on
 * `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.phone")
class CustomerPhoneCapability(
    private val customerDao: CustomerDao,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_PHONE
    override val evidenceTag: String = "phone_normalize"

    override fun normalize(value: String): String = CustomerPhoneNormalizer.normalize(value)
    override fun needsNormalization(value: String?): Boolean = CustomerPhoneNormalizer.needsNormalization(value)

    override fun rationale(current: String, canonical: String): String =
        "Phone numbers are dialled by digits; \"$current\" normalizes to \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Normalize customer phone \"$current\" → \"$canonical\""

    /** Never rewrite a value with no digits (not a phone number) even if it's non-canonical text. */
    override fun rejectTarget(after: String): String? =
        if (after.none { it.isDigit() }) "target is not a phone number" else null

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { findingFor(it.id, it.phone) }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.phone
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    companion object {
        const val KEY = "customer.phone"
        const val ENTITY_TYPE = "customer"
        const val FIELD_PHONE = "phone"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
