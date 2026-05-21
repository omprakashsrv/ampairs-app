package com.ampairs.subscription.util

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Manages subscription onboarding flow
 * Tracks which workspaces have completed subscription setup
 */
@Inject @SingleIn(AppScope::class)
class SubscriptionOnboardingManager(
    private val appPreferences: AppPreferencesDataStore
) {

    /**
     * Check if user has seen plan selection for a workspace
     */
    suspend fun hasSeenPlanSelection(workspaceId: String): Boolean {
        return appPreferences.hasSeenPlanSelection(workspaceId).first()
    }

    /**
     * Mark that user has seen plan selection for a workspace
     */
    suspend fun markPlanSelectionSeen(workspaceId: String) {
        appPreferences.markPlanSelectionSeen(workspaceId)
    }

    /**
     * Save current subscription plan for workspace
     */
    suspend fun saveSubscriptionPlan(workspaceId: String, planCode: String) {
        appPreferences.saveSubscriptionPlan(workspaceId, planCode)
    }

    /**
     * Get saved subscription plan for workspace
     */
    suspend fun getSavedSubscriptionPlan(workspaceId: String): String? {
        return appPreferences.getSavedSubscriptionPlan(workspaceId).first()
    }

    /**
     * Check if user should be shown upgrade prompt
     * Returns true if:
     * - Workspace has FREE plan
     * - User hasn't seen plan selection yet
     */
    suspend fun shouldShowPlanSelection(workspaceId: String, currentPlan: String): Boolean {
        // If user has already seen plan selection, don't show again
        if (hasSeenPlanSelection(workspaceId)) {
            return false
        }

        // Show plan selection only for FREE plan
        return currentPlan.equals("FREE", ignoreCase = true)
    }

    /**
     * Reset plan selection seen flag (for testing or re-onboarding)
     */
    suspend fun resetPlanSelectionSeen(workspaceId: String) {
        appPreferences.resetPlanSelectionSeen(workspaceId)
    }

    /**
     * Clear all subscription onboarding data for a workspace
     */
    suspend fun clearWorkspaceData(workspaceId: String) {
        appPreferences.clearSubscriptionOnboardingData(workspaceId)
    }

    /**
     * Set flag to show upgrade prompt later
     */
    suspend fun setShouldShowUpgrade(workspaceId: String, shouldShow: Boolean) {
        appPreferences.setShouldShowUpgrade(workspaceId, shouldShow)
    }

    /**
     * Check if should show upgrade prompt
     */
    suspend fun shouldShowUpgrade(workspaceId: String): Boolean {
        return appPreferences.shouldShowUpgrade(workspaceId).first()
    }

    /**
     * Observe if should show plan selection
     */
    fun observeShouldShowPlanSelection(workspaceId: String): Flow<Boolean> {
        return appPreferences.hasSeenPlanSelection(workspaceId)
    }
}
