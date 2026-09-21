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
 * Applies an approved `product.code` [Candidate] by rewriting the product's `code` through
 * [ProductRepository.updateProduct] — the normal offline-first write (`synced = false` + pending-push
 * → `ProductSyncDelegate`). The same path serves rollback: undo passes a candidate whose `after` is the
 * original value.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@AiOpsExecutorKey("product.code")
class ProductCodeExecutor(
    private val productRepository: ProductRepository,
) : AiOpsExecutor {

    override val capabilityKey: String = ProductCodeCapability.KEY

    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        val target = candidate.after
        if (target.isNullOrBlank()) return ExecResult(false, "no target code")
        val product = productRepository.getProduct(finding.entityId)
            ?: return ExecResult(false, "product ${finding.entityId} not found")
        if (product.code == target) return ExecResult(true, "already \"$target\"")

        return productRepository.updateProduct(product.copy(code = target)).fold(
            onSuccess = { ExecResult(true, "product code → \"$target\"") },
            onFailure = { ExecResult(false, it.message ?: "product update failed") },
        )
    }
}
