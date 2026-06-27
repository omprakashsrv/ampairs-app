package com.ampairs.payment.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the payment (accounting) module (FR-016). Curated read-only
 * columns only — sync/audit columns omitted. NOTE: every `*_minor` amount is in integer **minor
 * units (paise)**; divide by 100 for the rupee value.
 */
@ContributesTo(WorkspaceScope::class)
interface PaymentQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("payment")
        fun providePaymentQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "payment",
            tables = listOf(
                TableSchema(
                    "party_balance",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Party balance UID"),
                        ColumnSchema("party_uid", "TEXT", "Party / customer UID"),
                        ColumnSchema("opening_minor", "INTEGER", "Opening balance, paise"),
                        ColumnSchema("opening_direction", "TEXT", "Opening direction DR/CR"),
                        ColumnSchema("opening_as_of", "TEXT", "Opening balance date (ISO-8601)"),
                        ColumnSchema("cached_closing_minor", "INTEGER", "Closing balance, paise"),
                        ColumnSchema("last_computed_at", "TEXT", "Last computed (ISO-8601)"),
                    ),
                ),
                TableSchema(
                    "ledger_entry",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Ledger entry UID"),
                        ColumnSchema("party_uid", "TEXT", "Party / customer UID"),
                        ColumnSchema("entry_date", "TEXT", "Entry date (ISO-8601 UTC)"),
                        ColumnSchema("entry_type", "TEXT", "Entry type"),
                        ColumnSchema("direction", "TEXT", "Direction DR/CR"),
                        ColumnSchema("amount_minor", "INTEGER", "Amount, paise"),
                        ColumnSchema("source_type", "TEXT", "Source entity type"),
                        ColumnSchema("voucher_no", "TEXT", "Voucher number"),
                        ColumnSchema("narration", "TEXT", "Memo / narration"),
                        ColumnSchema("reversed", "INTEGER", "1 if reversed, else 0"),
                    ),
                ),
                TableSchema(
                    "payment_voucher",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Payment voucher UID"),
                        ColumnSchema("party_uid", "TEXT", "Party / customer UID"),
                        ColumnSchema("voucher_no", "TEXT", "Voucher number"),
                        ColumnSchema("voucher_date", "TEXT", "Voucher date (ISO-8601)"),
                        ColumnSchema("direction", "TEXT", "RECEIVED or PAID"),
                        ColumnSchema("total_minor", "INTEGER", "Total amount, paise"),
                        ColumnSchema("payment_mode", "TEXT", "Cash/UPI/bank/etc."),
                        ColumnSchema("reference_number", "TEXT", "Bank / instrument reference"),
                        ColumnSchema("clearance_status", "TEXT", "Cleared/pending/bounced"),
                        ColumnSchema("unallocated_minor", "INTEGER", "Unallocated amount, paise"),
                    ),
                ),
                TableSchema(
                    "payment_allocation",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Allocation UID"),
                        ColumnSchema("payment_voucher_uid", "TEXT", "Payment voucher UID"),
                        ColumnSchema("target_type", "TEXT", "Target type, e.g. INVOICE"),
                        ColumnSchema("target_uid", "TEXT", "Target invoice/entity UID"),
                        ColumnSchema("amount_minor", "INTEGER", "Allocated amount, paise"),
                    ),
                ),
                TableSchema(
                    "adjustment_voucher",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Adjustment voucher UID"),
                        ColumnSchema("party_uid", "TEXT", "Party / customer UID"),
                        ColumnSchema("voucher_no", "TEXT", "Voucher number"),
                        ColumnSchema("voucher_date", "TEXT", "Voucher date (ISO-8601)"),
                        ColumnSchema("adjustment_type", "TEXT", "Adjustment type"),
                        ColumnSchema("amount_minor", "INTEGER", "Amount, paise"),
                        ColumnSchema("narration", "TEXT", "Reason / description"),
                    ),
                ),
            ),
        )
    }
}
