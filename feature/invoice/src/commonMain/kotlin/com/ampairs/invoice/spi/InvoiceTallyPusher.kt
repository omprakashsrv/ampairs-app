package com.ampairs.invoice.spi

/**
 * Optional capability: push a **single** locally-created invoice into Tally, on explicit user action
 * from the invoice-view screen. There is no automatic invoice push — this SPI is the only per-invoice
 * path (the Tally settings screen still offers a bulk push of all unpushed invoices).
 *
 * Contributed **only on Desktop** (the platform with Tally connectivity) via
 * `@ContributesIntoSet(WorkspaceScope::class)`. On Android/iOS nothing contributes it, so the injected
 * `Set<InvoiceTallyPusher>` is empty and the "Push to Tally" button is absent there.
 */
interface InvoiceTallyPusher {
    /**
     * Pushes the given invoice to Tally and returns a human-readable result message on success, or a
     * failure carrying the reason (e.g. Tally host not configured, voucher rejected). Implementations
     * are responsible for capturing the returned Tally reference onto the invoice (idempotency).
     */
    suspend fun pushInvoice(invoiceId: String): Result<String>
}
