package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.supplier.ui.SupplierCreateRoute
import com.ampairs.supplier.ui.SupplierDetailsRoute
import com.ampairs.supplier.ui.SupplierListRoute
import com.ampairs.supplier.ui.create.SupplierFormScreen
import com.ampairs.supplier.ui.details.SupplierDetailsScreen
import com.ampairs.supplier.ui.list.SuppliersListScreen

/**
 * Entry provider for Supplier module routes in Navigation 3.
 * Returns NavEntry for supplier routes or null if route doesn't match.
 */
fun supplierEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is SupplierListRoute -> NavEntry(key) {
        SuppliersListScreen(
            onSupplierClick = { supplierId ->
                backStack.add(SupplierDetailsRoute(supplierId))
            },
            onCreateSupplier = {
                backStack.add(SupplierCreateRoute())
            },
            modifier = Modifier
        )
    }

    is SupplierDetailsRoute -> NavEntry(key) {
        SupplierDetailsScreen(
            supplierId = key.supplierId,
            onNavigateBack = { backStack.removeLastOrNull() },
            onEditSupplier = { supplierId ->
                backStack.add(SupplierCreateRoute(supplierId))
            },
            modifier = Modifier
        )
    }

    is SupplierCreateRoute -> NavEntry(key) {
        SupplierFormScreen(
            supplierId = key.supplierId,
            onSaveSuccess = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    else -> null
}
