package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.cbemployee.ui.CbEmployeeFormRoute
import com.ampairs.cbemployee.ui.CbEmployeeListRoute
import com.ampairs.cbemployee.ui.form.CbEmployeeFormScreen
import com.ampairs.cbemployee.ui.list.CbEmployeeListScreen

/** Entry provider for cb-employee routes (maintenance team). */
fun cbEmployeeEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is CbEmployeeListRoute -> NavEntry(key) {
        CbEmployeeListScreen(
            onEmployeeClick = { id -> backStack.add(CbEmployeeFormRoute(id)) },
            onAddEmployee = { backStack.add(CbEmployeeFormRoute()) },
            modifier = Modifier,
        )
    }

    is CbEmployeeFormRoute -> NavEntry(key) {
        CbEmployeeFormScreen(
            employeeId = key.employeeId,
            onDone = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
