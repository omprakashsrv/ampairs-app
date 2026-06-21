package com.ampairs.navigation.providers

import PaymentRoute
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.payment.ui.PaymentDashboardScreen
import com.ampairs.payment.ui.PaymentPartyPickerScreen
import com.ampairs.payment.ui.RecordPaymentScreen

/**
 * Entry provider for Payment & Collection module routes (Navigation 3).
 * Returns a NavEntry for payment routes or null if the route doesn't match.
 */
fun paymentEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is PaymentRoute.Dashboard -> NavEntry(key) {
        PaymentDashboardScreen(
            onRecordPayment = { backStack.add(PaymentRoute.SelectParty) },
        )
    }

    is PaymentRoute.SelectParty -> NavEntry(key) {
        PaymentPartyPickerScreen(
            onPartySelected = { partyUid ->
                backStack.removeLastOrNull()
                backStack.add(PaymentRoute.Record(partyUid = partyUid))
            },
        )
    }

    is PaymentRoute.Record -> NavEntry(key) {
        RecordPaymentScreen(
            partyUid = key.partyUid,
            onSaved = { backStack.removeLastOrNull() },
        )
    }

    else -> null
}
