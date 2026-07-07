package com.ampairs.sfa.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 routes for the SFA module. */
@Serializable
data object SfaBeatListRoute : NavKey

@Serializable
data class BeatFormRoute(val beatId: String? = null) : NavKey

@Serializable
data object SfaPlannedVisitListRoute : NavKey

@Serializable
data object SfaAttendanceRoute : NavKey

@Serializable
data object SfaLeaveListRoute : NavKey

@Serializable
data class LeaveFormRoute(val leaveId: String? = null) : NavKey

@Serializable
data class VisitCaptureRoute(
    val plannedVisitUid: String? = null,
    val customerUid: String = "",
    val repMemberUid: String = "",
) : NavKey
