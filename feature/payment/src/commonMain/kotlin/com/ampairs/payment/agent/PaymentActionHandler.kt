package com.ampairs.payment.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.DateRange
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.ReportPeriod
import com.ampairs.common.agent.ReportRow
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.payment.data.db.dao.PaymentVoucherDao
import com.ampairs.payment.domain.OutstandingService
import com.ampairs.payment.domain.PaymentDirection
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Curated receivables/payments reports (deterministic; preferred over free SQL). Outstanding and
 * top-debtors come from [OutstandingService] (which resolves customer names — payment rows are keyed
 * only by `party_uid`); payments-received sums the voucher ledger directly.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("payment")
class PaymentActionHandler(
    private val outstandingService: OutstandingService,
    private val voucherDao: PaymentVoucherDao,
) : ActionHandler {

    override val moduleName = "payment"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.OUTSTANDING -> reportOutstanding()
        ActionType.TOP_DEBTORS -> reportTopDebtors(action.params)
        ActionType.PAYMENTS_RECEIVED -> reportPaymentsReceived(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    private suspend fun reportOutstanding(): ActionResult {
        val receivable = outstandingService.agingSummary().totalReceivable
        return ActionResult.Success(
            summary = "Total outstanding receivables (money owed to you).",
            amount = receivable.toDouble(),
        )
    }

    private suspend fun reportTopDebtors(params: Map<String, String>): ActionResult {
        val limit = params["limit"]?.trim()?.toIntOrNull()?.coerceIn(1, 20) ?: 5
        val rows = outstandingService.topDebtors(limit)
            .map { ReportRow(label = it.partyName.ifBlank { "Unknown party" }, amount = it.balance.toDouble()) }
        return if (rows.isEmpty()) {
            ActionResult.Success("No outstanding balances — every party is settled up.")
        } else {
            ActionResult.Success(summary = "Top ${rows.size} debtors by outstanding balance:", rows = rows)
        }
    }

    private suspend fun reportPaymentsReceived(params: Map<String, String>): ActionResult {
        val period = resolvePeriod(params)
        val direction = PaymentDirection.RECEIVED.name
        val minor = if (period.range == null) {
            voucherDao.sumByDirection(direction)
        } else {
            // voucher_date is an ISO-8601 instant string, so bounds must be ISO too (Instant.toString()).
            voucherDao.sumByDirectionBetween(
                direction,
                period.range.startInclusive.toString(),
                period.range.endExclusive.toString(),
            )
        }
        return ActionResult.Success(summary = "Payments received${period.label}.", amount = minor / 100.0)
    }

    private data class ResolvedPeriod(val range: DateRange?, val label: String)

    private fun resolvePeriod(params: Map<String, String>): ResolvedPeriod {
        val raw = (params["period"] ?: params["range"] ?: params["duration"])?.trim()
        val range = ReportPeriod.current(raw)
        val label = if (range != null && !raw.isNullOrBlank()) {
            " (" + raw.lowercase().replace('_', ' ').replace('-', ' ') + ")"
        } else {
            ""
        }
        return ResolvedPeriod(range, label)
    }

    companion object {
        private val PERIOD_PARAM = ActionParameter(
            "period",
            ParameterType.STRING,
            required = false,
            "Time window: today, yesterday, this_week, last_week, this_month, last_month, this_year, last_year. Omit for all time.",
        )
        private val LIMIT_PARAM = ActionParameter(
            "limit",
            ParameterType.NUMBER,
            required = false,
            "How many to return (default 5, max 20).",
        )

        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.OUTSTANDING,
                moduleName = "payment",
                description = "Total outstanding receivables (money customers owe you). Use for \"outstanding\", \"total receivables\", \"how much is owed to me\".",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.TOP_DEBTORS,
                moduleName = "payment",
                description = "Customers with the largest outstanding balance, ranked. Use for \"top debtors\", \"who owes the most\", \"biggest pending payments\".",
                parameters = listOf(LIMIT_PARAM),
            ),
            ActionDescriptor(
                actionType = ActionType.PAYMENTS_RECEIVED,
                moduleName = "payment",
                description = "Total payments received (collections). Use for \"payments received\", \"how much did we collect\", \"collections this month\".",
                parameters = listOf(PERIOD_PARAM),
            ),
        )
    }
}
