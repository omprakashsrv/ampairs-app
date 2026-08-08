package com.ampairs.invoice.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.spi.InvoiceTallyPusher
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/**
 * Declares the [InvoiceTallyPusher] multibinding so `InvoiceViewViewModel` can inject the (possibly
 * empty) set. Only the Desktop `tallysync` module contributes an implementation via
 * `@ContributesIntoSet(WorkspaceScope::class)`; the set is empty on Android/iOS, so the invoice-view
 * "Push to Tally" button never appears on platforms without Tally connectivity.
 */
@ContributesTo(WorkspaceScope::class)
interface InvoiceTallyModule {

    @Multibinds(allowEmpty = true)
    fun invoiceTallyPushers(): Set<InvoiceTallyPusher>
}
