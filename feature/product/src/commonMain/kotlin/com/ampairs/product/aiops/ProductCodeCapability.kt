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
 * First AI Ops capability on a **third module** (`product`): **product-code (SKU) normalization** — trims
 * and upper-cases the exact-match `code` identifier. Now built on the shared
 * [FieldNormalizationCapability] base, so only the product-specific bits remain here: the deterministic
 * [ProductCodeNormalizer], the field/tag/labels, and the DAO-bound `detect`/`gather`. Auto-fixes at
 * autonomy ≥ AUTO_CORRECT through [ProductCodeExecutor] (which writes via the product repository +
 * offline-sync).
 *
 * Lives in `feature/product` and depends only on the `com.ampairs.common.aiops` contracts (+ the shared
 * `ProductDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("product.code")
class ProductCodeCapability(
    private val productDao: ProductDao,
) : FieldNormalizationCapability() {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW
    override val field: String = FIELD_CODE
    override val evidenceTag: String = "code_normalize"

    override fun normalize(value: String): String = ProductCodeNormalizer.normalize(value)
    override fun needsNormalization(value: String?): Boolean = ProductCodeNormalizer.needsNormalization(value)

    override fun rationale(current: String, canonical: String): String =
        "Product codes are exact-match identifiers; \"$current\" normalizes to \"$canonical\"."

    override fun summarize(current: String, canonical: String): String =
        "Normalize product code \"$current\" → \"$canonical\""

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        productDao.observeAllProducts().first()   // active rows only
            .mapNotNull { findingFor(it.id, it.code) }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = productDao.productById(finding.entityId)?.code
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    companion object {
        const val KEY = "product.code"
        const val ENTITY_TYPE = "product"
        const val FIELD_CODE = "code"

        /** Stable per-product finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(productUid: String): String = "AIO-$KEY-$productUid"
    }
}
