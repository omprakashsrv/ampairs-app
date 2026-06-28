package com.ampairs.pricing

import android.content.Context
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.database.createAndroidDatabase
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.workspace.WorkspaceClosableRegistry
import com.ampairs.common.workspace.WorkspaceConfig
import com.ampairs.pricing.data.db.OffersDatabase
import com.ampairs.pricing.data.db.PricingDatabase
import com.ampairs.pricing.data.db.migrations.PRICING_MIGRATION_1_2
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers

@ContributesTo(WorkspaceScope::class)
interface PricingAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun providePricingDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): PricingDatabase = factory.createAndroidDatabase<PricingDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "pricing",
            workspaceSlug = config.workspaceSlug,
            migrations = listOf(PRICING_MIGRATION_1_2),
        ).also { closableRegistry.register { it.close() } }

        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideOffersDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): OffersDatabase = factory.createAndroidDatabase<OffersDatabase>(
            context = context,
            queryDispatcher = Dispatchers.IO,
            moduleName = "offers",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
