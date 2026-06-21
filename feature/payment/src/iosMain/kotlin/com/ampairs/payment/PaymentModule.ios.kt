package com.ampairs.payment

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.payment.data.db.PaymentRoomDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(WorkspaceScope::class)
interface PaymentIosModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePaymentDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PaymentRoomDatabase = factory.createDatabase<PaymentRoomDatabase>(
            moduleName = "payment",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
