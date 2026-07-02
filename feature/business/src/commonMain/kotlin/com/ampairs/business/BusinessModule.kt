package com.ampairs.business

import com.ampairs.business.agent.BusinessAgentDao
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface BusinessDaoModule {
    companion object {
        @Provides
        fun provideBusinessDao(db: BusinessDatabase): BusinessDao = db.businessDao()

        @Provides
        fun provideBusinessAgentDao(db: BusinessDatabase): BusinessAgentDao = db.businessAgentDao()
    }
}
