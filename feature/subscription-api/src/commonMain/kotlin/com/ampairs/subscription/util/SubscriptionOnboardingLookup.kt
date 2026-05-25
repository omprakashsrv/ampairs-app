package com.ampairs.subscription.util

import kotlinx.coroutines.flow.Flow

interface SubscriptionOnboardingLookup {
    suspend fun hasSeenPlanSelection(workspaceId: String): Boolean
    suspend fun markPlanSelectionSeen(workspaceId: String)
    suspend fun saveSubscriptionPlan(workspaceId: String, planCode: String)
    suspend fun getSavedSubscriptionPlan(workspaceId: String): String?
    suspend fun shouldShowPlanSelection(workspaceId: String, currentPlan: String): Boolean
    suspend fun resetPlanSelectionSeen(workspaceId: String)
    suspend fun clearWorkspaceData(workspaceId: String)
    suspend fun setShouldShowUpgrade(workspaceId: String, shouldShow: Boolean)
    suspend fun shouldShowUpgrade(workspaceId: String): Boolean
    fun observeShouldShowPlanSelection(workspaceId: String): Flow<Boolean>
}
