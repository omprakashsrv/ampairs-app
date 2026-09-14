package com.ampairs.customer.aiops

import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.AiOpsExecutorKey
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.ExecResult
import com.ampairs.common.aiops.Finding
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.customer.data.repository.CustomerRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Applies an approved `customer.email` [Candidate] by rewriting the customer's `email` through
 * [CustomerRepository.updateCustomer] — the normal offline-first write (`synced = false` + pending-push
 * → `CustomerSyncDelegate`). The same path serves rollback: undo passes a candidate whose `after` is the
 * original value.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@AiOpsExecutorKey("customer.email")
class CustomerEmailExecutor(
    private val customerRepository: CustomerRepository,
) : AiOpsExecutor {

    override val capabilityKey: String = CustomerEmailCapability.KEY

    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        val target = candidate.after
        if (target.isNullOrBlank()) return ExecResult(false, "no target email")
        val customer = customerRepository.getCustomer(finding.entityId)
            ?: return ExecResult(false, "customer ${finding.entityId} not found")
        if (customer.email == target) return ExecResult(true, "already \"$target\"")

        return customerRepository.updateCustomer(customer.copy(email = target)).fold(
            onSuccess = { ExecResult(true, "customer email → \"$target\"") },
            onFailure = { ExecResult(false, it.message ?: "customer update failed") },
        )
    }
}
