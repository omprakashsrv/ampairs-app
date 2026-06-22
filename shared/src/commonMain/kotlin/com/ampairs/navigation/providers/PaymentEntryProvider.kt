package com.ampairs.navigation.providers

import PaymentRoute
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.payment.ui.AdjustmentScreen
import com.ampairs.payment.ui.OpeningBalanceScreen
import com.ampairs.payment.ui.PartyPaymentsScreen
import com.ampairs.payment.ui.PartyStatementScreen
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
            onRecordPayment = { backStack.add(PaymentRoute.SelectParty(purpose = "PAYMENT")) },
            onNewAdjustment = { backStack.add(PaymentRoute.SelectParty(purpose = "ADJUSTMENT")) },
            onSetOpeningBalance = { backStack.add(PaymentRoute.SelectParty(purpose = "OPENING")) },
            onOpenParty = { partyUid -> backStack.add(PaymentRoute.Statement(partyUid = partyUid)) },
            onRecordPaymentForParty = { partyUid -> backStack.add(PaymentRoute.Record(partyUid = partyUid)) },
            onAdjustmentForParty = { partyUid -> backStack.add(PaymentRoute.Adjustment(partyUid = partyUid)) },
            onOpeningForParty = { partyUid -> backStack.add(PaymentRoute.OpeningBalance(partyUid = partyUid)) },
            onReceiptsForParty = { partyUid -> backStack.add(PaymentRoute.PartyPayments(partyUid = partyUid)) },
            onEditPaymentForParty = { partyUid, voucherUid ->
                backStack.add(PaymentRoute.Record(partyUid = partyUid, voucherUid = voucherUid))
            },
        )
    }

    is PaymentRoute.SelectParty -> NavEntry(key) {
        val purpose = key.purpose
        PaymentPartyPickerScreen(
            onPartySelected = { partyUid ->
                backStack.removeLastOrNull()
                when (purpose) {
                    "ADJUSTMENT" -> backStack.add(PaymentRoute.Adjustment(partyUid = partyUid))
                    "OPENING" -> backStack.add(PaymentRoute.OpeningBalance(partyUid = partyUid))
                    else -> backStack.add(PaymentRoute.Record(partyUid = partyUid))
                }
            },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    is PaymentRoute.Record -> NavEntry(key) {
        RecordPaymentScreen(
            partyUid = key.partyUid,
            voucherUid = key.voucherUid,
            onSaved = { backStack.removeLastOrNull() },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    is PaymentRoute.Adjustment -> NavEntry(key) {
        AdjustmentScreen(
            partyUid = key.partyUid,
            onSaved = { backStack.removeLastOrNull() },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    is PaymentRoute.OpeningBalance -> NavEntry(key) {
        OpeningBalanceScreen(
            partyUid = key.partyUid,
            onSaved = { backStack.removeLastOrNull() },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    is PaymentRoute.Statement -> NavEntry(key) {
        PartyStatementScreen(
            partyUid = key.partyUid,
            onOpenPayments = { backStack.add(PaymentRoute.PartyPayments(partyUid = key.partyUid)) },
            onRecordPayment = { backStack.add(PaymentRoute.Record(partyUid = key.partyUid)) },
            onAdjustment = { backStack.add(PaymentRoute.Adjustment(partyUid = key.partyUid)) },
            onOpeningBalance = { backStack.add(PaymentRoute.OpeningBalance(partyUid = key.partyUid)) },
            onEditPayment = { voucherUid ->
                backStack.add(PaymentRoute.Record(partyUid = key.partyUid, voucherUid = voucherUid))
            },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    is PaymentRoute.PartyPayments -> NavEntry(key) {
        PartyPaymentsScreen(
            partyUid = key.partyUid,
            onEditVoucher = { voucherUid ->
                backStack.add(PaymentRoute.Record(partyUid = key.partyUid, voucherUid = voucherUid))
            },
            onBack = { backStack.removeLastOrNull() },
        )
    }

    else -> null
}
