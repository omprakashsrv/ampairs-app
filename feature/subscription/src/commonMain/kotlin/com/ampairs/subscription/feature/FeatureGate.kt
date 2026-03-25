package com.ampairs.subscription.feature

import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Feature gating service that controls access to features based on subscription.
 *
 * This service provides:
 * 1. Module availability checks
 * 2. Feature flag checks
 * 3. Resource limit checks
 * 4. Upgrade recommendations
 */
class FeatureGate(
    private val repository: SubscriptionRepository
) {

    // ======================
    // Module Access
    // ======================

    /**
     * Check if a module is available in current subscription
     */
    suspend fun hasModule(workspaceId: String, moduleCode: String): Boolean {
        val subscription = repository.getSubscription(workspaceId)
            ?: return isModuleInFreePlan(moduleCode)
        return subscription.getFeatures().hasModule(moduleCode)
    }

    /**
     * Check if module is available (reactive)
     */
    fun observeModuleAccess(workspaceId: String, moduleCode: String): Flow<Boolean> {
        return repository.observeSubscription(workspaceId).map { subscription ->
            subscription?.getFeatures()?.hasModule(moduleCode)
                ?: isModuleInFreePlan(moduleCode)
        }
    }

    /**
     * Get list of available modules for current subscription
     */
    suspend fun getAvailableModules(workspaceId: String): List<String> {
        val subscription = repository.getSubscription(workspaceId)
            ?: return SubscriptionFeatures.FREE.availableModules
        return subscription.getFeatures().availableModules
    }

    /**
     * Check if module is in free plan
     */
    private fun isModuleInFreePlan(moduleCode: String): Boolean {
        return SubscriptionFeatures.FREE.hasModule(moduleCode)
    }

    // ======================
    // Feature Flags
    // ======================

    /**
     * Check if a specific feature is enabled
     */
    suspend fun hasFeature(workspaceId: String, feature: SubscriptionFeature): Boolean {
        val subscription = repository.getSubscription(workspaceId) ?: return false
        val features = subscription.getFeatures()

        return when (feature) {
            SubscriptionFeature.API_ACCESS -> features.apiAccessEnabled
            SubscriptionFeature.CUSTOM_BRANDING -> features.customBrandingEnabled
            SubscriptionFeature.SSO -> features.ssoEnabled
            SubscriptionFeature.AUDIT_LOGS -> features.auditLogsEnabled
            SubscriptionFeature.PRIORITY_SUPPORT -> features.prioritySupport
        }
    }

    /**
     * Observe feature availability (reactive)
     */
    fun observeFeature(workspaceId: String, feature: SubscriptionFeature): Flow<Boolean> {
        return repository.observeSubscription(workspaceId).map { subscription ->
            subscription?.let { hasFeatureInternal(it, feature) } ?: false
        }
    }

    private fun hasFeatureInternal(subscription: SubscriptionState, feature: SubscriptionFeature): Boolean {
        val features = subscription.getFeatures()
        return when (feature) {
            SubscriptionFeature.API_ACCESS -> features.apiAccessEnabled
            SubscriptionFeature.CUSTOM_BRANDING -> features.customBrandingEnabled
            SubscriptionFeature.SSO -> features.ssoEnabled
            SubscriptionFeature.AUDIT_LOGS -> features.auditLogsEnabled
            SubscriptionFeature.PRIORITY_SUPPORT -> features.prioritySupport
        }
    }

    // ======================
    // Add-on Checks
    // ======================

    /**
     * Check if an add-on is active
     */
    suspend fun hasAddon(workspaceId: String, addonCode: AddonModuleCode): Boolean {
        val subscription = repository.getSubscription(workspaceId) ?: return false
        return subscription.hasAddon(addonCode.name)
    }

    /**
     * Observe add-on availability
     */
    fun observeAddon(workspaceId: String, addonCode: AddonModuleCode): Flow<Boolean> {
        return repository.observeSubscription(workspaceId).map { subscription ->
            subscription?.hasAddon(addonCode.name) ?: false
        }
    }

    // ======================
    // Subscription Status
    // ======================

    /**
     * Check if subscription is active
     */
    suspend fun isSubscriptionActive(workspaceId: String): Boolean {
        val subscription = repository.getSubscription(workspaceId) ?: return false
        return subscription.isActive()
    }

    /**
     * Check if subscription is in trial
     */
    suspend fun isInTrial(workspaceId: String): Boolean {
        val subscription = repository.getSubscription(workspaceId) ?: return false
        return subscription.isInTrial()
    }

    /**
     * Get current plan tier
     */
    suspend fun getCurrentPlan(workspaceId: String): SubscriptionPlan {
        val subscription = repository.getSubscription(workspaceId)
            ?: return SubscriptionPlan.FREE
        return SubscriptionPlan.fromCode(subscription.planCode)
    }

    /**
     * Observe current plan (reactive)
     */
    fun observeCurrentPlan(workspaceId: String): Flow<SubscriptionPlan> {
        return repository.observeSubscription(workspaceId).map { subscription ->
            subscription?.let { SubscriptionPlan.fromCode(it.planCode) } ?: SubscriptionPlan.FREE
        }
    }

    // ======================
    // Upgrade Recommendations
    // ======================

    /**
     * Get upgrade recommendation for a specific feature
     */
    suspend fun getUpgradeRecommendation(
        workspaceId: String,
        requiredFeature: SubscriptionFeature
    ): UpgradeRecommendation? {
        val currentPlan = getCurrentPlan(workspaceId)

        // Find the minimum plan that includes this feature
        val recommendedPlan = when (requiredFeature) {
            SubscriptionFeature.API_ACCESS -> SubscriptionPlan.PROFESSIONAL
            SubscriptionFeature.CUSTOM_BRANDING -> SubscriptionPlan.PROFESSIONAL
            SubscriptionFeature.SSO -> SubscriptionPlan.ENTERPRISE
            SubscriptionFeature.AUDIT_LOGS -> SubscriptionPlan.ENTERPRISE
            SubscriptionFeature.PRIORITY_SUPPORT -> SubscriptionPlan.PROFESSIONAL
        }

        if (currentPlan >= recommendedPlan) return null

        return UpgradeRecommendation(
            currentPlan = currentPlan,
            recommendedPlan = recommendedPlan,
            feature = requiredFeature.displayName,
            reason = "Upgrade to ${recommendedPlan.displayName} to access ${requiredFeature.displayName}"
        )
    }

    /**
     * Get upgrade recommendation for a module
     */
    suspend fun getModuleUpgradeRecommendation(
        workspaceId: String,
        moduleCode: String
    ): UpgradeRecommendation? {
        if (hasModule(workspaceId, moduleCode)) return null

        val currentPlan = getCurrentPlan(workspaceId)

        // Find minimum plan that includes this module
        val recommendedPlan = findMinimumPlanForModule(moduleCode) ?: SubscriptionPlan.ENTERPRISE

        if (currentPlan >= recommendedPlan) return null

        return UpgradeRecommendation(
            currentPlan = currentPlan,
            recommendedPlan = recommendedPlan,
            feature = moduleCode,
            reason = "Upgrade to ${recommendedPlan.displayName} to access this module"
        )
    }

    private fun findMinimumPlanForModule(moduleCode: String): SubscriptionPlan? {
        // Check each plan in order
        if (SubscriptionFeatures.FREE.hasModule(moduleCode)) return SubscriptionPlan.FREE
        if (SubscriptionFeatures.STARTER.hasModule(moduleCode)) return SubscriptionPlan.STARTER
        if (SubscriptionFeatures.PROFESSIONAL.hasModule(moduleCode)) return SubscriptionPlan.PROFESSIONAL
        if (SubscriptionFeatures.ENTERPRISE.hasModule(moduleCode)) return SubscriptionPlan.ENTERPRISE
        return null
    }
}

/**
 * Subscription features that can be checked
 */
enum class SubscriptionFeature(val displayName: String) {
    API_ACCESS("API Access"),
    CUSTOM_BRANDING("Custom Branding"),
    SSO("Single Sign-On"),
    AUDIT_LOGS("Audit Logs"),
    PRIORITY_SUPPORT("Priority Support")
}

/**
 * Upgrade recommendation
 */
data class UpgradeRecommendation(
    val currentPlan: SubscriptionPlan,
    val recommendedPlan: SubscriptionPlan,
    val feature: String,
    val reason: String
)
