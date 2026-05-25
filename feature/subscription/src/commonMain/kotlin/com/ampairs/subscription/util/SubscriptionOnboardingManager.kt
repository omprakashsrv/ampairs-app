package com.ampairs.subscription.util

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Inject @SingleIn(AppScope::class)
class SubscriptionOnboardingManager(
    private val appPreferences: AppPreferencesDataStore
) : SubscriptionOnboardingLookup {

    override suspend fun hasSeenPlanSelection(workspaceId: String): Boolean {
        return appPreferences.hasSeenPlanSelection(workspaceId).first()
    }

    override suspend fun markPlanSelectionSeen(workspaceId: String) {
        appPreferences.markPlanSelectionSeen(workspaceId)
    }

    override suspend fun saveSubscriptionPlan(workspaceId: String, planCode: String) {
        appPreferences.saveSubscriptionPlan(workspaceId, planCode)
    }

    override suspend fun getSavedSubscriptionPlan(workspaceId: String): String? {
        return appPreferences.getSavedSubscriptionPlan(workspaceId).first()
    }

    override suspend fun shouldShowPlanSelection(workspaceId: String, currentPlan: String): Boolean {
        if (hasSeenPlanSelection(workspaceId)) return false
        return currentPlan.equals("FREE", ignoreCase = true)
    }

    override suspend fun resetPlanSelectionSeen(workspaceId: String) {
        appPreferences.resetPlanSelectionSeen(workspaceId)
    }

    override suspend fun clearWorkspaceData(workspaceId: String) {
        appPreferences.clearSubscriptionOnboardingData(workspaceId)
    }

    override suspend fun setShouldShowUpgrade(workspaceId: String, shouldShow: Boolean) {
        appPreferences.setShouldShowUpgrade(workspaceId, shouldShow)
    }

    override suspend fun shouldShowUpgrade(workspaceId: String): Boolean {
        return appPreferences.shouldShowUpgrade(workspaceId).first()
    }

    override fun observeShouldShowPlanSelection(workspaceId: String): Flow<Boolean> {
        return appPreferences.hasSeenPlanSelection(workspaceId)
    }
}
