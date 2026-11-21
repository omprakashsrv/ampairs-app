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
 * Applies an approved `customer.name` [Candidate] by rewriting the customer's `name` through
 * [CustomerRepository.updateCustomer] — the normal offline-first write (`synced = false` + pending-push
 * → `CustomerSyncDelegate`). The same path serves rollback: undo passes a candidate whose `after` is the
 * original value.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@AiOpsExecutorKey("customer.name")
class CustomerNameExecutor(
    private val customerRepository: CustomerRepository,
) : AiOpsExecutor {

    override val capabilityKey: String = CustomerNameCapability.KEY

    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        val target = candidate.after
        if (target.isNullOrBlank()) return ExecResult(false, "no target name")
        val customer = customerRepository.getCustomer(finding.entityId)
            ?: return ExecResult(false, "customer ${finding.entityId} not found")
        if (customer.name == target) return ExecResult(true, "already \"$target\"")

        return customerRepository.updateCustomer(customer.copy(name = target)).fold(
            onSuccess = { ExecResult(true, "customer name → \"$target\"") },
            onFailure = { ExecResult(false, it.message ?: "customer update failed") },
        )
    }
}
