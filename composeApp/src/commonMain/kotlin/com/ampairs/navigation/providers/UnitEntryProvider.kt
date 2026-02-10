package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.unit.ui.UnitFormRoute
import com.ampairs.unit.ui.UnitListRoute
import com.ampairs.unit.ui.form.UnitFormScreen
import com.ampairs.unit.ui.list.UnitListScreen

/**
 * Entry provider for Unit module routes in Navigation 3.
 * Returns NavEntry for unit routes or null if route doesn't match.
 */
fun unitEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is UnitListRoute -> NavEntry(key) {
        UnitListScreen(
            onUnitClick = { unitId ->
                backStack.add(UnitFormRoute(unitId))
            },
            onAddUnit = {
                backStack.add(UnitFormRoute())
            },
            modifier = Modifier
        )
    }

    is UnitFormRoute -> NavEntry(key) {
        UnitFormScreen(
            unitId = key.unitId,
            onSaveSuccess = {
                backStack.removeLastOrNull()
            },
            modifier = Modifier
        )
    }

    else -> null
}
