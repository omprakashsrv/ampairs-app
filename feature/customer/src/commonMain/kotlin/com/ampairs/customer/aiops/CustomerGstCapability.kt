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
 * **Customer GSTIN normalization** — trims + upper-cases the regulated `gstNumber` identifier. Built on the
 * shared [FieldNormalizationCapability] base, so only the customer-specific bits remain: the deterministic
 * [CustomerGstNormalizer], the field/tag/labels, and the DAO-bound `detect`/`gather`. Auto-fixes at
 * autonomy ≥ AUTO_CORRECT through [CustomerGstExecutor].
 *
 * This is an identifier *format* fix (case + edge whitespace), distinct from the roadmap's tax/HSN advisory
 * work — it never suggests or changes a tax classification. Depends only on the `com.ampairs.common.aiops`
 * contracts (+ this module's `CustomerDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("customer.gst")
class CustomerGstCapability(
    private val customerDao: CustomerDao,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_GST
    override val evidenceTag: String = "gst_normalize"

    override fun normalize(value: String): String = CustomerGstNormalizer.normalize(value)
    override fun needsNormalization(value: String?): Boolean = CustomerGstNormalizer.needsNormalization(value)

    override fun rationale(current: String, canonical: String): String =
        "GSTINs are canonically upper-case; \"$current\" normalizes to \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Normalize customer GSTIN \"$current\" → \"$canonical\""

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        customerDao.getAllCustomers().first()
            .filter { it.active }
            .mapNotNull { findingFor(it.id, it.gstNumber) }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = customerDao.getCustomerById(finding.entityId)?.gstNumber
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    companion object {
        const val KEY = "customer.gst"
        const val ENTITY_TYPE = "customer"
        const val FIELD_GST = "gstNumber"

        /** Stable per-customer finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(customerUid: String): String = "AIO-$KEY-$customerUid"
    }
}
