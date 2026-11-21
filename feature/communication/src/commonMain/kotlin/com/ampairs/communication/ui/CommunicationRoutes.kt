package com.ampairs.communication.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Hub screen — entry point with cards to each communication area. */
@Serializable
data object CommunicationHomeRoute : NavKey

/** List of message templates. */
@Serializable
data object CommTemplateListRoute : NavKey

/** Editor for a single template (blank uid = create). Raw-HTML body + server-rendered preview. */
@Serializable
data class CommTemplateEditRoute(val templateUid: String = "") : NavKey

/** Recurring schedules list. */
@Serializable
data object CommScheduleListRoute : NavKey

/** Promotional campaigns list with lifecycle actions. */
@Serializable
data object CommCampaignListRoute : NavKey

/** Provider credential settings (BYO sender identity). */
@Serializable
data object CommCredentialRoute : NavKey

/** Usage / billing report. */
@Serializable
data object CommUsageRoute : NavKey

/** Delivery-status (logs) list. */
@Serializable
data object CommLogRoute : NavKey
