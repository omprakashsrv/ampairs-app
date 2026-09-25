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
 * **Customer name whitespace normalization** — trims and collapses internal whitespace runs in `name`
 * (a different transform kind from the case/format normalizers). Built on the shared
 * [FieldNormalizationCapability] base, so only the customer-specific bits remain: the deterministic
 * [CustomerNameNormalizer], the field/tag/labels, and the DAO-bound `detect`/`gather`. Auto-fixes at
 * autonomy ≥ AUTO_CORRECT through [CustomerNameExecutor].
 *
 * Depends only on the `com.ampairs.common.aiops` contracts (+ this module's `CustomerDao`) — never on
 * `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.name")
class CustomerNameCapability(
    private val customerDao: CustomerDao,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_NAME
    override val evidenceTag: String = "name_normalize"

    override fun normalize(value: String): String = CustomerNameNormalizer.normalize(value)
    override fun needsNormalization(value: String?): Boolean = CustomerNameNormalizer.needsNormalization(value)

    override fun rationale(current: String, canonical: String): String =
        "Stray spacing is data-entry noise; \"$current\" tidies to \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Tidy customer name spacing \"$current\" → \"$canonical\""

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { findingFor(it.id, it.name) }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.name
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    companion object {
        const val KEY = "customer.name"
        const val ENTITY_TYPE = "customer"
        const val FIELD_NAME = "name"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
