package com.ampairs.subscription.feature

import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Service for checking and enforcing subscription resource limits.
 *
 * Provides:
 * 1. Limit checks before creating resources
 * 2. Usage warnings when approaching limits
 * 3. Enforcement decisions (allow/block/warn)
 */
class LimitChecker(
    private val repository: SubscriptionRepository
) {

    // ======================
    // Resource Type Constants
    // ======================

    object ResourceType {
        const val CUSTOMER = "CUSTOMER"
        const val PRODUCT = "PRODUCT"
        const val INVOICE = "INVOICE"
        const val MEMBER = "MEMBER"
        const val DEVICE = "DEVICE"
        const val STORAGE_GB = "STORAGE_GB"
        const val ORDER = "ORDER"
    }

    // ======================
    // Limit Checks
    // ======================

    /**
     * Check if creating a new resource is allowed
     *
     * @param workspaceId The workspace ID
     * @param resourceType Type of resource (CUSTOMER, PRODUCT, etc.)
     * @param currentCount Current count of this resource type
     * @return LimitCheckDecision with allowed/blocked status and details
     */
    suspend fun checkLimit(
        workspaceId: String,
        resourceType: String,
        currentCount: Int
    ): LimitCheckDecision {
        val subscription = repository.getSubscription(workspaceId)
            ?: return checkLocalLimit(resourceType, currentCount, SubscriptionLimits.FREE)

        val limits = subscription.getLimits()
        return checkLocalLimit(resourceType, currentCount, limits)
    }

    /**
     * Check limit against local cached limits (offline-capable)
     */
    private fun checkLocalLimit(
        resourceType: String,
        currentCount: Int,
        limits: SubscriptionLimits
    ): LimitCheckDecision {
        val limit = getLimit(resourceType, limits)

        // Unlimited
        if (limit == SubscriptionLimits.UNLIMITED) {
            return LimitCheckDecision.Allowed(
                currentCount = currentCount,
                limit = limit,
                remaining = Int.MAX_VALUE,
                isUnlimited = true
            )
        }

        val remaining = limit - currentCount
        val percentUsed = if (limit > 0) (currentCount.toFloat() / limit * 100).toInt() else 0

        return when {
            currentCount >= limit -> {
                LimitCheckDecision.Blocked(
                    currentCount = currentCount,
                    limit = limit,
                    resourceType = resourceType,
                    message = "You've reached the maximum limit of $limit ${resourceType.lowercase()}s"
                )
            }
            percentUsed >= 80 -> {
                LimitCheckDecision.Warning(
                    currentCount = currentCount,
                    limit = limit,
                    remaining = remaining,
                    percentUsed = percentUsed,
                    message = "You're using ${percentUsed}% of your ${resourceType.lowercase()} limit"
                )
            }
            else -> {
                LimitCheckDecision.Allowed(
                    currentCount = currentCount,
                    limit = limit,
                    remaining = remaining,
                    isUnlimited = false
                )
            }
        }
    }

    /**
     * Get limit value for a resource type
     */
    private fun getLimit(resourceType: String, limits: SubscriptionLimits): Int {
        return when (resourceType.uppercase()) {
            ResourceType.CUSTOMER -> limits.maxCustomers
            ResourceType.PRODUCT -> limits.maxProducts
            ResourceType.INVOICE -> limits.maxInvoicesPerMonth
            ResourceType.MEMBER -> limits.maxMembersPerWorkspace
            ResourceType.DEVICE -> limits.maxDevices
            ResourceType.STORAGE_GB -> limits.maxStorageGb
            else -> SubscriptionLimits.UNLIMITED
        }
    }

    // ======================
    // Quick Checks
    // ======================

    /**
     * Quick check if resource can be created (returns true/false only)
     */
    suspend fun canCreate(
        workspaceId: String,
        resourceType: String,
        currentCount: Int
    ): Boolean {
        val decision = checkLimit(workspaceId, resourceType, currentCount)
        return decision !is LimitCheckDecision.Blocked
    }

    /**
     * Get remaining count for a resource type
     */
    suspend fun getRemainingCount(
        workspaceId: String,
        resourceType: String,
        currentCount: Int
    ): Int {
        val subscription = repository.getSubscription(workspaceId)
        val limits = subscription?.getLimits() ?: SubscriptionLimits.FREE
        val limit = getLimit(resourceType, limits)

        if (limit == SubscriptionLimits.UNLIMITED) return Int.MAX_VALUE
        return (limit - currentCount).coerceAtLeast(0)
    }

    // ======================
    // Observe Limits
    // ======================

    /**
     * Observe limit for a resource type (reactive)
     */
    fun observeLimit(workspaceId: String, resourceType: String): Flow<Int> {
        return repository.observeSubscription(workspaceId).map { subscription ->
            val limits = subscription?.getLimits() ?: SubscriptionLimits.FREE
            getLimit(resourceType, limits)
        }
    }

    /**
     * Observe usage status for all resources
     */
    fun observeUsageStatus(workspaceId: String): Flow<UsageStatus?> {
        return combine(
            repository.observeSubscription(workspaceId),
            repository.observeUsage(workspaceId)
        ) { subscription, usage ->
            if (subscription == null || usage == null) return@combine null

            val limits = subscription.getLimits()

            UsageStatus(
                customerStatus = getResourceStatus(usage.usage.customerCount, limits.maxCustomers),
                productStatus = getResourceStatus(usage.usage.productCount, limits.maxProducts),
                invoiceStatus = getResourceStatus(usage.usage.invoiceCount, limits.maxInvoicesPerMonth),
                memberStatus = getResourceStatus(usage.usage.memberCount, limits.maxMembersPerWorkspace),
                deviceStatus = getResourceStatus(usage.usage.deviceCount, limits.maxDevices),
                storageStatus = getResourceStatus(usage.usage.storageUsedGb.toInt(), limits.maxStorageGb),
                hasAnyLimitExceeded = usage.exceeded.hasAnyExceeded,
                hasAnyWarning = listOf(
                    usage.usage.customerCount to limits.maxCustomers,
                    usage.usage.productCount to limits.maxProducts,
                    usage.usage.invoiceCount to limits.maxInvoicesPerMonth,
                    usage.usage.memberCount to limits.maxMembersPerWorkspace,
                    usage.usage.deviceCount to limits.maxDevices
                ).any { (current, limit) ->
                    limit != SubscriptionLimits.UNLIMITED && current >= (limit * 0.8).toInt()
                }
            )
        }
    }

    private fun getResourceStatus(current: Int, limit: Int): ResourceUsageStatus {
        if (limit == SubscriptionLimits.UNLIMITED) {
            return ResourceUsageStatus(
                current = current,
                limit = limit,
                percentUsed = 0,
                status = ResourceStatus.UNLIMITED
            )
        }

        val percentUsed = if (limit > 0) (current.toFloat() / limit * 100).toInt() else 0

        val status = when {
            current >= limit -> ResourceStatus.EXCEEDED
            percentUsed >= 80 -> ResourceStatus.WARNING
            else -> ResourceStatus.OK
        }

        return ResourceUsageStatus(
            current = current,
            limit = limit,
            percentUsed = percentUsed,
            status = status
        )
    }

    // ======================
    // Upgrade Suggestions
    // ======================

    /**
     * Get upgrade suggestion when limit is exceeded
     */
    suspend fun getUpgradeSuggestion(
        workspaceId: String,
        resourceType: String
    ): LimitUpgradeSuggestion? {
        val currentPlan = repository.getSubscription(workspaceId)
            ?.let { SubscriptionPlan.fromCode(it.planCode) }
            ?: SubscriptionPlan.FREE

        if (currentPlan == SubscriptionPlan.ENTERPRISE) return null

        val nextPlan = when (currentPlan) {
            SubscriptionPlan.FREE -> SubscriptionPlan.STARTER
            SubscriptionPlan.STARTER -> SubscriptionPlan.PROFESSIONAL
            SubscriptionPlan.PROFESSIONAL -> SubscriptionPlan.ENTERPRISE
            SubscriptionPlan.ENTERPRISE -> return null
        }

        val nextPlanLimits = when (nextPlan) {
            SubscriptionPlan.FREE -> SubscriptionLimits.FREE
            SubscriptionPlan.STARTER -> SubscriptionLimits.STARTER
            SubscriptionPlan.PROFESSIONAL -> SubscriptionLimits.PROFESSIONAL
            SubscriptionPlan.ENTERPRISE -> SubscriptionLimits.ENTERPRISE
        }

        val newLimit = getLimit(resourceType, nextPlanLimits)

        return LimitUpgradeSuggestion(
            currentPlan = currentPlan,
            suggestedPlan = nextPlan,
            resourceType = resourceType,
            newLimit = if (newLimit == SubscriptionLimits.UNLIMITED) "Unlimited" else newLimit.toString(),
            message = "Upgrade to ${nextPlan.displayName} for ${if (newLimit == SubscriptionLimits.UNLIMITED) "unlimited" else "$newLimit"} ${resourceType.lowercase()}s"
        )
    }
}

/**
 * Result of a limit check
 */
sealed class LimitCheckDecision {
    data class Allowed(
        val currentCount: Int,
        val limit: Int,
        val remaining: Int,
        val isUnlimited: Boolean
    ) : LimitCheckDecision()

    data class Warning(
        val currentCount: Int,
        val limit: Int,
        val remaining: Int,
        val percentUsed: Int,
        val message: String
    ) : LimitCheckDecision()

    data class Blocked(
        val currentCount: Int,
        val limit: Int,
        val resourceType: String,
        val message: String
    ) : LimitCheckDecision()
}

/**
 * Overall usage status for a workspace
 */
data class UsageStatus(
    val customerStatus: ResourceUsageStatus,
    val productStatus: ResourceUsageStatus,
    val invoiceStatus: ResourceUsageStatus,
    val memberStatus: ResourceUsageStatus,
    val deviceStatus: ResourceUsageStatus,
    val storageStatus: ResourceUsageStatus,
    val hasAnyLimitExceeded: Boolean,
    val hasAnyWarning: Boolean
)

/**
 * Usage status for a single resource type
 */
data class ResourceUsageStatus(
    val current: Int,
    val limit: Int,
    val percentUsed: Int,
    val status: ResourceStatus
)

/**
 * Status of a resource limit
 */
enum class ResourceStatus {
    OK,
    WARNING,
    EXCEEDED,
    UNLIMITED
}

/**
 * Suggestion to upgrade when limit is reached
 */
data class LimitUpgradeSuggestion(
    val currentPlan: SubscriptionPlan,
    val suggestedPlan: SubscriptionPlan,
    val resourceType: String,
    val newLimit: String,
    val message: String
)
