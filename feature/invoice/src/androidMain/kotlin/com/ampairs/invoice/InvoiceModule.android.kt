package com.ampairs.invoice

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.AppScope
import com.ampairs.invoice.db.InvoiceRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
interface InvoiceAndroidModule {
    companion object {
        @Provides
        fun provideInvoiceDatabase(factory: WorkspaceAwareDatabaseFactory, context: Context): InvoiceRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "invoice",
                migrations = emptyList()
            )
    }
}
