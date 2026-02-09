package com.ampairs.navigation.providers

import InvoiceRoute
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.invoice.ui.InvoicePaneScreen
import com.ampairs.invoice.ui.InvoiceScreen
import com.ampairs.invoice.ui.InvoiceViewScreen

/**
 * Entry provider for Invoice module routes in Navigation 3.
 * Returns NavEntry for invoice routes or null if route doesn't match.
 */
fun invoiceEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is InvoiceRoute.Root -> NavEntry(key) {
        InvoiceScreen(
            fromCustomerId = key.fromCustomer.ifEmpty { null },
            toCustomerId = key.toCustomer.ifEmpty { null },
            id = key.id.ifEmpty { null },
            onInvoiceSaved = { invoiceId ->
                backStack.add(InvoiceRoute.InvoiceView(id = invoiceId))
            }
        )
    }

    is InvoiceRoute.InvoiceView -> NavEntry(key) {
        InvoiceViewScreen(
            invoiceId = key.id,
            onNavigateBack = { invoiceId ->
                if (!invoiceId.isNullOrEmpty()) {
                    backStack.add(InvoiceRoute.Root(id = invoiceId))
                } else {
                    backStack.removeLastOrNull()
                }
            }
        )
    }

    is InvoiceRoute.Invoices -> NavEntry(key) {
        InvoicePaneScreen(
            onInvoiceEdit = { invoiceId ->
                if (!invoiceId.isNullOrEmpty()) {
                    backStack.add(InvoiceRoute.Root(id = invoiceId))
                } else {
                    backStack.add(InvoiceRoute.Root())
                }
            }
        )
    }

    else -> null
}
