package com.ampairs.product.aiops

import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.AiOpsExecutorKey
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.ExecResult
import com.ampairs.common.aiops.Finding
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.product.data.repository.ProductRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Applies an approved `product.name` [Candidate] by rewriting the product's `name` through
 * [ProductRepository.updateProduct] — the normal offline-first write (`synced = false` + pending-push
 * → `ProductSyncDelegate`). The same path serves rollback: undo passes a candidate whose `after` is the
 * original value.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@AiOpsExecutorKey("product.name")
class ProductNameExecutor(
    private val productRepository: ProductRepository,
) : AiOpsExecutor {

    override val capabilityKey: String = ProductNameCapability.KEY

    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        val target = candidate.after
        if (target.isNullOrBlank()) return ExecResult(false, "no target name")
        val product = productRepository.getProduct(finding.entityId)
            ?: return ExecResult(false, "product ${finding.entityId} not found")
        if (product.name == target) return ExecResult(true, "already \"$target\"")

        return productRepository.updateProduct(product.copy(name = target)).fold(
            onSuccess = { ExecResult(true, "product name → \"$target\"") },
            onFailure = { ExecResult(false, it.message ?: "product update failed") },
        )
    }
}
