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
 * Applies an approved `customer.gst` [Candidate] by rewriting the customer's `gstNumber` through
 * [CustomerRepository.updateCustomer] — the normal offline-first write (`synced = false` + pending-push
 * → `CustomerSyncDelegate`). The same path serves rollback: undo passes a candidate whose `after` is the
 * original value.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@AiOpsExecutorKey("customer.gst")
class CustomerGstExecutor(
    private val customerRepository: CustomerRepository,
) : AiOpsExecutor {

    override val capabilityKey: String = CustomerGstCapability.KEY

    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        val target = candidate.after
        if (target.isNullOrBlank()) return ExecResult(false, "no target GSTIN")
        val customer = customerRepository.getCustomer(finding.entityId)
            ?: return ExecResult(false, "customer ${finding.entityId} not found")
        if (customer.gstNumber == target) return ExecResult(true, "already \"$target\"")

        return customerRepository.updateCustomer(customer.copy(gstNumber = target)).fold(
            onSuccess = { ExecResult(true, "customer GSTIN → \"$target\"") },
            onFailure = { ExecResult(false, it.message ?: "customer update failed") },
        )
    }
}
