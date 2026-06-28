package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.communication.ui.CampaignListScreen
import com.ampairs.communication.ui.CommCampaignListRoute
import com.ampairs.communication.ui.CommCredentialRoute
import com.ampairs.communication.ui.CommLogRoute
import com.ampairs.communication.ui.CommScheduleListRoute
import com.ampairs.communication.ui.CommTemplateEditRoute
import com.ampairs.communication.ui.CommTemplateListRoute
import com.ampairs.communication.ui.CommUsageRoute
import com.ampairs.communication.ui.CommunicationHomeRoute
import com.ampairs.communication.ui.CommunicationHomeScreen
import com.ampairs.communication.ui.CredentialSettingsScreen
import com.ampairs.communication.ui.LogListScreen
import com.ampairs.communication.ui.ScheduleListScreen
import com.ampairs.communication.ui.TemplateEditScreen
import com.ampairs.communication.ui.TemplateListScreen
import com.ampairs.communication.ui.UsageScreen

/**
 * Entry provider for the communication module routes in Navigation 3.
 * Returns a NavEntry for communication routes or null if the route doesn't match.
 */
fun communicationEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is CommunicationHomeRoute -> NavEntry(key) {
        CommunicationHomeScreen(
            onOpenTemplates = { backStack.add(CommTemplateListRoute) },
            onOpenSchedules = { backStack.add(CommScheduleListRoute) },
            onOpenCampaigns = { backStack.add(CommCampaignListRoute) },
            onOpenCredentials = { backStack.add(CommCredentialRoute) },
            onOpenUsage = { backStack.add(CommUsageRoute) },
            onOpenLogs = { backStack.add(CommLogRoute) },
            modifier = Modifier,
        )
    }

    is CommTemplateListRoute -> NavEntry(key) {
        TemplateListScreen(
            onEditTemplate = { uid -> backStack.add(CommTemplateEditRoute(uid)) },
            modifier = Modifier,
        )
    }

    is CommTemplateEditRoute -> NavEntry(key) {
        TemplateEditScreen(
            templateUid = key.templateUid,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is CommScheduleListRoute -> NavEntry(key) { ScheduleListScreen(modifier = Modifier) }

    is CommCampaignListRoute -> NavEntry(key) { CampaignListScreen(modifier = Modifier) }

    is CommCredentialRoute -> NavEntry(key) { CredentialSettingsScreen(modifier = Modifier) }

    is CommUsageRoute -> NavEntry(key) { UsageScreen(modifier = Modifier) }

    is CommLogRoute -> NavEntry(key) { LogListScreen(modifier = Modifier) }

    else -> null
}
