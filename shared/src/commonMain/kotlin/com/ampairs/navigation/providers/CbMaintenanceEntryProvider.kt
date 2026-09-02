package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.cbmaintenance.ui.CbPmDueListRoute
import com.ampairs.cbmaintenance.ui.CbPmScheduleListRoute
import com.ampairs.cbmaintenance.ui.CbRaiseTicketRoute
import com.ampairs.cbmaintenance.ui.CbTicketListRoute
import com.ampairs.cbmaintenance.ui.due.PmDueListScreen
import com.ampairs.cbmaintenance.ui.schedule.PmScheduleListScreen
import com.ampairs.cbmaintenance.ui.ticket.RaiseTicketScreen
import com.ampairs.cbmaintenance.ui.ticket.TicketListScreen

/** Entry provider for cb-maintenance routes (PM + tickets). */
fun cbMaintenanceEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is CbPmDueListRoute -> NavEntry(key) {
        PmDueListScreen(
            onOpenTickets = { backStack.add(CbTicketListRoute) },
            onOpenSchedules = { backStack.add(CbPmScheduleListRoute) },
            modifier = Modifier,
        )
    }

    is CbTicketListRoute -> NavEntry(key) {
        TicketListScreen(
            onRaiseTicket = { backStack.add(CbRaiseTicketRoute) },
            modifier = Modifier,
        )
    }

    is CbRaiseTicketRoute -> NavEntry(key) {
        RaiseTicketScreen(
            onDone = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is CbPmScheduleListRoute -> NavEntry(key) {
        PmScheduleListScreen(modifier = Modifier)
    }

    else -> null
}
