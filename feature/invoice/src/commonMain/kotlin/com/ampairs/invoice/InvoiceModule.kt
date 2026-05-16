package com.ampairs.invoice

import com.ampairs.agent.core.ActionHandlerProvider
import com.ampairs.invoice.agent.InvoiceActionHandler
import com.ampairs.invoice.api.InvoiceApi
import com.ampairs.invoice.api.InvoiceApiImpl
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.db.InvoiceRoomDatabase
import com.ampairs.invoice.viewmodel.InvoiceViewModel
import com.ampairs.invoice.viewmodel.InvoiceViewViewModel
import com.ampairs.invoice.viewmodel.InvoicesViewModel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val invoiceModule: Module = module {
    factory { InvoiceApiImpl(get(), get()) } bind (InvoiceApi::class)
    // Database is provided by platform-specific modules (factory for workspace-scoped isolation)
    factory { get<InvoiceRoomDatabase>().invoiceDao() }
    factory { InvoiceRepository(get(), get(), get(), get()) }

    factory<ActionHandlerProvider> {
        ActionHandlerProvider("invoice", InvoiceActionHandler.ACTIONS) {
            InvoiceActionHandler(get())
        }
    } bind ActionHandlerProvider::class

    // Direct ViewModel injection
    factory { InvoicesViewModel(get()) }
    factory { (fromCustomerId: String?, toCustomerId: String?, id: String?) ->
        InvoiceViewModel(fromCustomerId, toCustomerId, id, get(), get(), get(), get())
    }
    factory { (invoiceId: String) -> InvoiceViewViewModel(invoiceId, get()) }
}

fun invoiceModule() = invoiceModule