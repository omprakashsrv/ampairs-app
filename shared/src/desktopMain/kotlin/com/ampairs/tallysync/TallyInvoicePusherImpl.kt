package com.ampairs.tallysync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.invoice.spi.InvoiceTallyPusher
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Desktop implementation of the invoice module's [InvoiceTallyPusher] SPI, wired into the invoice
 * feature via Metro multibinding. It is the manual, per-invoice counterpart to the bulk push on the
 * Tally settings screen — the invoice-view "Push to Tally" button calls this. Only Desktop
 * contributes it, so the button is absent on Android/iOS (empty set).
 *
 * The workspace slug (which selects the Tally host config) comes from the injected [WorkspaceConfig],
 * so callers don't need to thread it through the common invoice UI.
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class TallyInvoicePusherImpl(
    private val scheduler: TallySyncScheduler,
    private val config: WorkspaceConfig,
) : InvoiceTallyPusher {

    override suspend fun pushInvoice(invoiceId: String): Result<String> {
        val result = scheduler.pushInvoices(config.workspaceSlug, invoiceId)
        return when {
            !result.success -> Result.failure(Exception(result.error ?: "Tally push failed"))
            result.pushed > 0 -> Result.success("Pushed to Tally")
            result.failed > 0 -> Result.failure(Exception("Tally rejected the voucher — see the Tally sync log"))
            result.skippedReason != null -> Result.failure(Exception(result.skippedReason))
            else -> Result.success("Already in Tally — nothing to push")
        }
    }
}
