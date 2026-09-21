package com.ampairs.product.aiops

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
import com.ampairs.product.db.dao.ProductDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Fourth AI Ops capability, and the first on a **third module** (`product`): **product-code (SKU)
 * normalization**. Proves the plug-in model is genuinely feature-local — capabilities and unit/customer
 * ones share nothing but the `com.ampairs.common.aiops` contracts. Detects active products whose `code`
 * carries whitespace or inconsistent case and proposes the trimmed-upper-cased value via the deterministic
 * [ProductCodeNormalizer] — no LLM, HIGH confidence, LOW risk, reversible. Auto-fixes at autonomy ≥
 * AUTO_CORRECT through [ProductCodeExecutor] (which writes via the product repository + offline-sync).
 *
 * Lives in `feature/product` and depends only on the `com.ampairs.common.aiops` contracts (+ the shared
 * `ProductDao`) — never on `feature/aiops`.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@CapabilityKey("product.code")
class ProductCodeCapability(
    private val productDao: ProductDao,
) : AiOpsCapability {

    override val key: String = KEY
    override val entityType: String = ENTITY_TYPE
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW

    override suspend fun detect(scope: AiOpsScope): List<Finding> =
        productDao.observeAllProducts().first()   // active rows only
            .mapNotNull { product ->
                val code = product.code
                if (!ProductCodeNormalizer.needsNormalization(code)) return@mapNotNull null
                val canonical = ProductCodeNormalizer.normalize(code)
                Finding(
                    id = findingId(product.id),
                    capability = KEY,
                    entityType = ENTITY_TYPE,
                    entityId = product.id,
                    field = FIELD_CODE,
                    summary = "Normalize product code \"$code\" → \"$canonical\"",
                    signals = mapOf("current" to code, "canonical" to canonical),
                )
            }

    override suspend fun gather(finding: Finding): FindingContext {
        val current = productDao.productById(finding.entityId)?.code
        return FindingContext(values = buildMap { current?.let { put("current", it) } })
    }

    override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> {
        val current = context.values["current"] ?: finding.signals["current"] ?: return emptyList()
        if (!ProductCodeNormalizer.needsNormalization(current)) return emptyList()
        val canonical = ProductCodeNormalizer.normalize(current)
        return listOf(
            Candidate(
                field = FIELD_CODE,
                before = current,
                after = canonical,
                action = AiOpsActionType.UPDATE_FIELD,
                rationale = "Product codes are exact-match identifiers; \"$current\" normalizes to \"$canonical\".",
                evidence = listOf("code_normalize:$current→$canonical"),
            ),
        )
    }

    override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation {
        val after = candidate.after
        return when {
            candidate.action != AiOpsActionType.UPDATE_FIELD -> Validation(false, "unexpected action")
            after.isNullOrBlank() -> Validation(false, "no target value")
            after == candidate.before -> Validation(false, "already normalized")
            after != ProductCodeNormalizer.normalize(after) -> Validation(false, "target is not canonical")
            else -> Validation(true)
        }
    }

    override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        Confidence(value = 0.999, band = AiOpsBand.HIGH, contributors = mapOf("code_normalize" to 1.0))

    companion object {
        const val KEY = "product.code"
        const val ENTITY_TYPE = "product"
        const val FIELD_CODE = "code"

        /** Stable per-product finding id so re-detection upserts the same row instead of duplicating. */
        fun findingId(productUid: String): String = "AIO-$KEY-$productUid"
    }
}
