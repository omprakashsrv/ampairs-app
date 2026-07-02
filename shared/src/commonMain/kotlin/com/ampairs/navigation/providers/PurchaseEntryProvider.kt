package com.ampairs.navigation.providers

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.purchase.ui.PurchaseDetailsScreen
import com.ampairs.purchase.ui.PurchaseFormScreen
import com.ampairs.purchase.ui.PurchaseRoute
import com.ampairs.purchase.ui.PurchasesListScreen

/**
 * Entry provider for Purchase module routes in Navigation 3.
 * Returns a NavEntry for purchase routes, or null if the route doesn't match.
 */
fun purchaseEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is PurchaseRoute.PurchaseListRoute -> NavEntry(key) {
        PurchasesListScreen(
            onPurchaseSelected = { purchaseId ->
                backStack.add(PurchaseRoute.PurchaseDetailsRoute(purchaseId = purchaseId))
            },
            onCreatePurchase = {
                backStack.add(PurchaseRoute.PurchaseCreateRoute())
            }
        )
    }

    is PurchaseRoute.PurchaseDetailsRoute -> NavEntry(key) {
        PurchaseDetailsScreen(
            purchaseId = key.purchaseId,
            onEdit = { purchaseId ->
                backStack.add(PurchaseRoute.PurchaseCreateRoute(purchaseId = purchaseId))
            },
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    is PurchaseRoute.PurchaseCreateRoute -> NavEntry(key) {
        PurchaseFormScreen(
            purchaseId = key.purchaseId,
            onPurchaseSaved = { purchaseId ->
                // Land on the details view without stacking duplicates: pop the editor and,
                // if we were editing FROM the details view, pop the stale view too so a fresh
                // entry shows the just-saved data.
                backStack.removeLastOrNull()
                if ((backStack.lastOrNull() as? PurchaseRoute.PurchaseDetailsRoute)?.purchaseId == purchaseId) {
                    backStack.removeLastOrNull()
                }
                backStack.add(PurchaseRoute.PurchaseDetailsRoute(purchaseId = purchaseId))
            },
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    else -> null
}
