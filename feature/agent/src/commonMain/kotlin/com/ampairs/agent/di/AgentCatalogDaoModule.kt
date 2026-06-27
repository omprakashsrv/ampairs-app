package com.ampairs.agent.di

import com.ampairs.agent.data.db.AgentCatalogDatabase
import com.ampairs.agent.data.db.dao.AiModelDao
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** Provides the model-catalog DAO from the app-scoped [AgentCatalogDatabase] (unscoped). */
@ContributesTo(AppScope::class)
interface AgentCatalogDaoModule {
    companion object {
        @Provides
        fun provideAiModelDao(db: AgentCatalogDatabase): AiModelDao = db.aiModelDao()
    }
}
