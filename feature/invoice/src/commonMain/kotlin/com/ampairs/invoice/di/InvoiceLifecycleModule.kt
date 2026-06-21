package com.ampairs.invoice.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.spi.InvoiceLifecycleListener
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Declares the [InvoiceLifecycleListener] multibinding (spec 013, R9) so [com.ampairs.invoice.db.InvoiceRepository]
 * can inject the (possibly empty) set. Downstream modules contribute via
 * `@ContributesIntoSet(WorkspaceScope::class)` — the payment module posts the party receivable.
 */
@ContributesTo(WorkspaceScope::class)
interface InvoiceLifecycleModule {

    @Multibinds(allowEmpty = true)
    fun invoiceLifecycleListeners(): Set<InvoiceLifecycleListener>
}
