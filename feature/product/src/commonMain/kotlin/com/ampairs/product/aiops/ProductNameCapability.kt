package com.ampairs.product.aiops

import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.CapabilityKey
import com.ampairs.common.aiops.FieldNormalizationCapability
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.db.dao.ProductDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * **Product name whitespace normalization** — a second field on the `product` entity (alongside
 * `product.code`), tidying stray whitespace in `name` (edge padding or internal runs). Built on the shared
 * [FieldNormalizationCapability] base, so only the product-specific bits remain: the deterministic
 * [ProductNameNormalizer], the field/tag/labels, and the DAO-bound `detect`/`gather`. No LLM, HIGH
 * confidence, LOW risk, reversible (only spacing changes; the original is kept in the audit trail).
 * Auto-fixes at autonomy ≥ AUTO_CORRECT through [ProductNameExecutor].
 *
 * Depends only on the `com.ampairs.common.aiops` contracts (+ the shared `ProductDao`) — never on
 * `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("product.name")
class ProductNameCapability(
    private val productDao: ProductDao,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_NAME
    override val evidenceTag: String = "name_normalize"

    override fun normalize(value: String): String = ProductNameNormalizer.normalize(value)
    override fun needsNormalization(value: String?): Boolean = ProductNameNormalizer.needsNormalization(value)

    override fun rationale(current: String, canonical: String): String =
        "Stray spacing is data-entry noise; \"$current\" tidies to \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Tidy product name spacing \"$current\" → \"$canonical\""

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        productDao.observeAllProducts().first()   // active rows only
            .mapNotNull { findingFor(it.id, it.name) }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = productDao.productById(finding.entityId)?.name
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    companion object {
        const val KEY = "product.name"
        const val ENTITY_TYPE = "product"
        const val FIELD_NAME = "name"

        /** Stable per-product finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(productUid: String): String = "AIO-$KEY-$productUid"
    }
}
