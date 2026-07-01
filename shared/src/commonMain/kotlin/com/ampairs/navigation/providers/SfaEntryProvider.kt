package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.sfa.ui.BeatFormRoute
import com.ampairs.sfa.ui.SfaAttendanceRoute
import com.ampairs.sfa.ui.SfaBeatListRoute
import com.ampairs.sfa.ui.SfaPlannedVisitListRoute
import com.ampairs.sfa.ui.VisitCaptureRoute
import com.ampairs.sfa.ui.attendance.AttendanceScreen
import com.ampairs.sfa.ui.beat.BeatFormScreen
import com.ampairs.sfa.ui.beat.BeatListScreen
import com.ampairs.sfa.ui.plannedvisit.PlannedVisitListScreen
import com.ampairs.sfa.ui.visit.VisitCaptureScreen

/**
 * Entry provider for SFA (field-sales) module routes in Navigation 3.
 * Returns a NavEntry for SFA routes, or null if the key doesn't match.
 */
fun sfaEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is SfaBeatListRoute -> NavEntry(key) {
        BeatListScreen(
            onBeatClick = { beatId -> backStack.add(BeatFormRoute(beatId)) },
            onAddBeat = { backStack.add(BeatFormRoute()) },
            onOpenPlannedVisits = { backStack.add(SfaPlannedVisitListRoute) },
            onOpenAttendance = { backStack.add(SfaAttendanceRoute) },
            modifier = Modifier,
        )
    }

    is SfaAttendanceRoute -> NavEntry(key) {
        AttendanceScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is SfaPlannedVisitListRoute -> NavEntry(key) {
        PlannedVisitListScreen(
            onBack = { backStack.removeLastOrNull() },
            onVisitClick = { pv ->
                backStack.add(VisitCaptureRoute(plannedVisitUid = pv.uid, customerUid = pv.customerUid, repMemberUid = pv.repMemberUid))
            },
            modifier = Modifier,
        )
    }

    is VisitCaptureRoute -> NavEntry(key) {
        VisitCaptureScreen(
            plannedVisitUid = key.plannedVisitUid,
            customerUid = key.customerUid,
            repMemberUid = key.repMemberUid,
            onSaveSuccess = { backStack.removeLastOrNull() },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is BeatFormRoute -> NavEntry(key) {
        BeatFormScreen(
            beatId = key.beatId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
