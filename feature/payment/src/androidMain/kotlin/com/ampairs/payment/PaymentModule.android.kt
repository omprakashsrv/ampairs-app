package com.ampairs.payment

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.payment.data.db.PaymentRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface PaymentAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePaymentDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PaymentRoomDatabase = factory.createAndroidDatabase<PaymentRoomDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "payment",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
