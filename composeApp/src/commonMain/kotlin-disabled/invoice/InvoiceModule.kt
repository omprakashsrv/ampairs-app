package com.ampairs.invoice

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
    single { InvoiceApiImpl(get(), get()) } bind (InvoiceApi::class)
    // Database is provided by platform-specific modules
    single { get<InvoiceRoomDatabase>().invoiceDao() }
    single { InvoiceRepository(get(), get(), get(), get()) }
    
    // Direct ViewModel injection
    factory { InvoicesViewModel(get()) }
    factory { (fromCustomerId: String?, toCustomerId: String?, id: String?) ->
        InvoiceViewModel(fromCustomerId, toCustomerId, id, get(), get(), get(), get(), get())
    }
    factory { (invoiceId: String) -> InvoiceViewViewModel(invoiceId, get()) }
}

fun invoiceModule() = invoiceModule