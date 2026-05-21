package com.ampairs.subscription.feature

import com.ampairs.subscription.repository.SubscriptionRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Service for enforcing subscription limits and read-only mode.
 *
 * This service checks limits BEFORE allowing create/update operations.
 * When limits are exceeded, workspace enters read-only mode for that resource.
 */
@Inject
class SubscriptionEnforcement(
    private val repository: SubscriptionRepository,
    private val limitChecker: LimitChecker
) {
    /**
     * Check if a resource can be created. Returns enforcement decision.
     *
     * @param workspaceId Workspace ID
     * @param resourceType Type of resource (CUSTOMER, PRODUCT, etc.)
     * @param currentCount Current count of resources
     * @return EnforcementDecision indicating if operation is allowed
     */
    suspend fun canCreateResource(
        workspaceId: String,
        resourceType: String,
        currentCount: Int
    ): EnforcementDecision {
        // Check subscription limits
        val decision = limitChecker.checkLimit(workspaceId, resourceType, currentCount)

        return when (decision) {
            is LimitCheckDecision.Blocked -> {
                // Get upgrade suggestion
                val suggestion = limitChecker.getUpgradeSuggestion(workspaceId, resourceType)

                EnforcementDecision.Blocked(
                    resourceType = resourceType,
                    currentCount = decision.currentCount,
                    limit = decision.limit,
                    message = decision.message,
                    upgradeSuggestion = suggestion
                )
            }
            is LimitCheckDecision.Warning -> {
                EnforcementDecision.Warning(
                    resourceType = resourceType,
                    currentCount = decision.currentCount,
                    limit = decision.limit,
                    remaining = decision.remaining,
                    percentUsed = decision.percentUsed,
                    message = decision.message
                )
            }
            is LimitCheckDecision.Allowed -> {
                EnforcementDecision.Allowed(
                    resourceType = resourceType,
                    currentCount = decision.currentCount,
                    remaining = decision.remaining,
                    isUnlimited = decision.isUnlimited
                )
            }
        }
    }

    /**
     * Check if a resource can be updated. Usually allowed unless workspace is locked.
     */
    suspend fun canUpdateResource(
        workspaceId: String,
        resourceType: String
    ): EnforcementDecision {
        val subscription = repository.getSubscription(workspaceId)

        // Check if subscription is in a state that allows updates
        return if (subscription == null || !subscription.isActive()) {
            EnforcementDecision.Blocked(
                resourceType = resourceType,
                currentCount = 0,
                limit = 0,
                message = "Subscription is not active. Please renew your subscription.",
                upgradeSuggestion = null
            )
        } else {
            EnforcementDecision.Allowed(
                resourceType = resourceType,
                currentCount = 0,
                remaining = Int.MAX_VALUE,
                isUnlimited = true
            )
        }
    }

    /**
     * Get current workspace mode
     */
    suspend fun getWorkspaceMode(workspaceId: String): WorkspaceMode {
        val usage = repository.getUsage(workspaceId)

        return when {
            usage == null -> WorkspaceMode.Normal
            usage.exceeded.hasAnyExceeded -> WorkspaceMode.ReadOnly(
                exceededResources = buildList {
                    if (usage.exceeded.customerLimitExceeded) add("Customers")
                    if (usage.exceeded.productLimitExceeded) add("Products")
                    if (usage.exceeded.invoiceLimitExceeded) add("Invoices")
                    if (usage.exceeded.deviceLimitExceeded) add("Devices")
                    if (usage.exceeded.memberLimitExceeded) add("Members")
                    if (usage.exceeded.storageLimitExceeded) add("Storage")
                }
            )
            else -> WorkspaceMode.Normal
        }
    }
}

/**
 * Result of enforcement check
 */
sealed class EnforcementDecision {
    /**
     * Operation is allowed
     */
    data class Allowed(
        val resourceType: String,
        val currentCount: Int,
        val remaining: Int,
        val isUnlimited: Boolean
    ) : EnforcementDecision()

    /**
     * Operation allowed with warning
     */
    data class Warning(
        val resourceType: String,
        val currentCount: Int,
        val limit: Int,
        val remaining: Int,
        val percentUsed: Int,
        val message: String
    ) : EnforcementDecision()

    /**
     * Operation blocked - show upgrade dialog
     */
    data class Blocked(
        val resourceType: String,
        val currentCount: Int,
        val limit: Int,
        val message: String,
        val upgradeSuggestion: LimitUpgradeSuggestion?
    ) : EnforcementDecision()
}

/**
 * Workspace operating mode
 */
sealed class WorkspaceMode {
    /**
     * Normal operation - all features available
     */
    data object Normal : WorkspaceMode()

    /**
     * Read-only mode - some resources cannot be created
     */
    data class ReadOnly(
        val exceededResources: List<String>
    ) : WorkspaceMode()
}
